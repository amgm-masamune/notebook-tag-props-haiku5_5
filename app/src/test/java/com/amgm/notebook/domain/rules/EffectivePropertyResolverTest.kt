package com.amgm.notebook.domain.rules

import com.amgm.notebook.domain.defOf
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.tagMap
import com.amgm.notebook.domain.tagOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EffectivePropertyResolverTest {

    // data-model.md 3.4 の具体例
    private val item = tagOf("Item", defs = listOf(defOf("price", PropertyType.Number, required = true)))
    private val book = tagOf("Book", parents = listOf("Item"), defs = listOf(defOf("author"), defOf("isbn")))
    private val ebook = tagOf("Ebook", parents = listOf("Book"), defs = listOf(defOf("price", PropertyType.Text)))
    private val gift = tagOf("Gift", defs = listOf(defOf("price", PropertyType.Date)))
    private val hybrid = tagOf("Hybrid", parents = listOf("Book", "Gift"))

    private val tags = tagMap(item, book, ebook, gift, hybrid)
    private val resolver = EffectivePropertyResolver(tags)

    private fun EffectiveSchema.names(): List<String> = definitions.map { it.definition.name.value }

    @Test
    fun `親の定義を継承して親から子の順に並ぶ`() {
        val schema = resolver.resolve(TagId("Book"))
        assertEquals(listOf("price", "author", "isbn"), schema.names())
        val price = schema.find(PropertyName("price"))!!
        assertEquals(PropertyType.Number, price.definition.type)
        assertTrue(price.definition.required)
        assertEquals(TagId("Item"), price.definedBy)
        assertTrue(schema.conflicts.isEmpty())
    }

    @Test
    fun `子の型変更が優先され位置は先頭のまま`() {
        val schema = resolver.resolve(TagId("Ebook"))
        assertEquals(listOf("price", "author", "isbn"), schema.names())
        val price = schema.find(PropertyName("price"))!!
        assertEquals(PropertyType.Text, price.definition.type)
        assertEquals(false, price.definition.required)
        assertEquals(TagId("Ebook"), price.definedBy)
    }

    @Test
    fun `多段継承の定義が全て集まる`() {
        val h = tagMap(
            tagOf("A", defs = listOf(defOf("p"))),
            tagOf("B", parents = listOf("A"), defs = listOf(defOf("q"))),
            tagOf("C", parents = listOf("B"), defs = listOf(defOf("r")))
        )
        assertEquals(listOf("p", "q", "r"), EffectivePropertyResolver(h).resolve(TagId("C")).names())
    }

    @Test
    fun `同一タグ内の定義はリスト順`() {
        val h = tagMap(tagOf("A", defs = listOf(defOf("z"), defOf("a"), defOf("m"))))
        assertEquals(listOf("z", "a", "m"), EffectivePropertyResolver(h).resolve(TagId("A")).names())
    }

    @Test
    fun `型の異なる親同士の同名定義は衝突になり定義から除かれる`() {
        val schema = resolver.resolve(TagId("Hybrid"))
        assertEquals(listOf("author", "isbn"), schema.names())
        assertEquals(1, schema.conflicts.size)
        val conflict = schema.conflicts.single()
        assertEquals(PropertyName("price"), conflict.name)
        assertEquals(
            listOf(
                ConflictCandidate(TagId("Item"), PropertyType.Number),
                ConflictCandidate(TagId("Gift"), PropertyType.Date)
            ),
            conflict.candidates
        )
        assertNull(schema.find(PropertyName("price")))
    }

    @Test
    fun `衝突は子での再定義により解消される`() {
        val fixed = hybrid.copy(ownPropertyDefinitions = listOf(defOf("price", PropertyType.Text)))
        val schema = EffectivePropertyResolver(tags + (fixed.id to fixed)).resolve(TagId("Hybrid"))
        assertTrue(schema.conflicts.isEmpty())
        val price = schema.find(PropertyName("price"))!!
        assertEquals(PropertyType.Text, price.definition.type)
        assertEquals(TagId("Hybrid"), price.definedBy)
    }

    @Test
    fun `同型の多重継承は統合されrequiredは論理和`() {
        val h = tagMap(
            tagOf("A", defs = listOf(defOf("x", PropertyType.Text, required = true))),
            tagOf("B", defs = listOf(defOf("x", PropertyType.Text, required = false))),
            tagOf("C", parents = listOf("A", "B"))
        )
        val schema = EffectivePropertyResolver(h).resolve(TagId("C"))
        assertTrue(schema.conflicts.isEmpty())
        val x = schema.find(PropertyName("x"))!!
        assertEquals(1, schema.definitions.size)
        assertEquals(PropertyType.Text, x.definition.type)
        assertTrue(x.definition.required)
        assertEquals(TagId("A"), x.definedBy)
    }

    @Test
    fun `ダイヤモンドでは中間の再定義が根の定義を上書きする`() {
        val h = tagMap(
            tagOf("Top", defs = listOf(defOf("x", PropertyType.Text))),
            tagOf("L", parents = listOf("Top"), defs = listOf(defOf("x", PropertyType.Number))),
            tagOf("R", parents = listOf("Top")),
            tagOf("Bottom", parents = listOf("L", "R"))
        )
        val schema = EffectivePropertyResolver(h).resolve(TagId("Bottom"))
        assertTrue(schema.conflicts.isEmpty())
        val x = schema.find(PropertyName("x"))!!
        assertEquals(PropertyType.Number, x.definition.type)
        assertEquals(TagId("L"), x.definedBy)
    }

    @Test
    fun `resolveForTagsは複数タグの定義を和集合にする`() {
        val h = tagMap(
            tagOf("Alpha", defs = listOf(defOf("a"))),
            tagOf("Beta", defs = listOf(defOf("b")))
        )
        val schema = EffectivePropertyResolver(h).resolveForTags(setOf(TagId("Beta"), TagId("Alpha")))
        // 走査順は名前順なので Alpha → Beta
        assertEquals(listOf("a", "b"), schema.names())
    }

    @Test
    fun `resolveForTagsでタグ間の型衝突も検出される`() {
        val schema = resolver.resolveForTags(setOf(TagId("Book"), TagId("Gift")))
        assertEquals(listOf("author", "isbn"), schema.names())
        assertEquals(PropertyName("price"), schema.conflicts.single().name)
    }

    @Test
    fun `resolveはresolveForTagsの単一要素版と一致する`() {
        assertEquals(resolver.resolveForTags(setOf(TagId("Ebook"))), resolver.resolve(TagId("Ebook")))
    }

    @Test
    fun `存在しないタグや空集合は空のスキーマ`() {
        assertEquals(EffectiveSchema.EMPTY, resolver.resolve(TagId("ghost")))
        assertEquals(EffectiveSchema.EMPTY, resolver.resolveForTags(emptySet()))
    }

    @Test
    fun `TagHierarchyを直接渡しても同じ結果`() {
        val viaHierarchy = EffectivePropertyResolver(TagHierarchy(tags))
        assertEquals(resolver.resolve(TagId("Hybrid")), viaHierarchy.resolve(TagId("Hybrid")))
    }
}
