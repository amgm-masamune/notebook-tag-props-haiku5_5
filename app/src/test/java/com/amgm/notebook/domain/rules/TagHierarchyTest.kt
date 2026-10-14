package com.amgm.notebook.domain.rules

import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.noteOf
import com.amgm.notebook.domain.tagMap
import com.amgm.notebook.domain.tagOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TagHierarchyTest {

    private fun ids(vararg names: String): List<TagId> = names.map { TagId(it) }

    // Item <- Book <- Ebook / Gift / Hybrid(Book, Gift) / Other
    private val hierarchy = TagHierarchy(
        tagMap(
            tagOf("Item"),
            tagOf("Book", parents = listOf("Item")),
            tagOf("Ebook", parents = listOf("Book")),
            tagOf("Gift"),
            tagOf("Hybrid", parents = listOf("Book", "Gift")),
            tagOf("Other")
        )
    )

    @Test
    fun `多段継承の祖先は祖先が先の順で返る`() {
        assertEquals(ids("Item", "Book"), hierarchy.ancestorsOf(TagId("Ebook")))
        assertEquals(emptyList<TagId>(), hierarchy.ancestorsOf(TagId("Item")))
    }

    @Test
    fun `複数親の祖先は名前順に線形化される`() {
        assertEquals(ids("Item", "Book", "Gift"), hierarchy.ancestorsOf(TagId("Hybrid")))
    }

    @Test
    fun `親の順序は大文字小文字を無視した名前順`() {
        val h = TagHierarchy(
            tagMap(tagOf("b"), tagOf("A"), tagOf("C", parents = listOf("b", "A")))
        )
        assertEquals(ids("A", "b"), h.ancestorsOf(TagId("C")))
    }

    @Test
    fun `ダイヤモンド継承で共通祖先は一度だけ出る`() {
        val h = TagHierarchy(
            tagMap(
                tagOf("Top"),
                tagOf("L", parents = listOf("Top")),
                tagOf("R", parents = listOf("Top")),
                tagOf("Bottom", parents = listOf("L", "R"))
            )
        )
        assertEquals(ids("Top", "L", "R"), h.ancestorsOf(TagId("Bottom")))
        assertEquals(setOf(TagId("L"), TagId("R"), TagId("Bottom")), h.descendantsOf(TagId("Top")))
    }

    @Test
    fun `祖先は常に子孫より前に並ぶ`() {
        val order = hierarchy.ancestorsOf(TagId("Hybrid"))
        assertTrue(order.indexOf(TagId("Item")) < order.indexOf(TagId("Book")))
    }

    @Test
    fun `子孫は推移閉包で自身を含まない`() {
        assertEquals(
            setOf(TagId("Book"), TagId("Ebook"), TagId("Hybrid")),
            hierarchy.descendantsOf(TagId("Item"))
        )
        assertEquals(setOf(TagId("Hybrid")), hierarchy.descendantsOf(TagId("Gift")))
        assertTrue(hierarchy.descendantsOf(TagId("Other")).isEmpty())
    }

    @Test
    fun `isSubtypeOfは自身と祖先でtrue`() {
        assertTrue(hierarchy.isSubtypeOf(TagId("Ebook"), TagId("Ebook")))
        assertTrue(hierarchy.isSubtypeOf(TagId("Ebook"), TagId("Item")))
        assertTrue(hierarchy.isSubtypeOf(TagId("Hybrid"), TagId("Gift")))
        assertFalse(hierarchy.isSubtypeOf(TagId("Item"), TagId("Ebook")))
        assertFalse(hierarchy.isSubtypeOf(TagId("Gift"), TagId("Book")))
    }

    @Test
    fun `子孫タグを持つノートは親タグの一員`() {
        val ebookNote = noteOf("n1", tags = listOf("Ebook"))
        assertTrue(hierarchy.isInstanceOf(ebookNote, TagId("Book")))
        assertTrue(hierarchy.isInstanceOf(ebookNote, TagId("Item")))
        assertTrue(hierarchy.isInstanceOf(ebookNote, TagId("Ebook")))
    }

    @Test
    fun `自タグを持つノートは一員で無関係タグのノートは一員でない`() {
        assertTrue(hierarchy.isInstanceOf(noteOf("n", tags = listOf("Book")), TagId("Book")))
        assertFalse(hierarchy.isInstanceOf(noteOf("n", tags = listOf("Other")), TagId("Book")))
        assertFalse(hierarchy.isInstanceOf(noteOf("n", tags = listOf("Gift")), TagId("Book")))
        assertFalse(hierarchy.isInstanceOf(noteOf("n"), TagId("Book")))
    }

    @Test
    fun `親タグを持つだけのノートは子タグの一員でない`() {
        assertFalse(hierarchy.isInstanceOf(noteOf("n", tags = listOf("Book")), TagId("Ebook")))
    }

    @Test
    fun `複数タグのうち一つでも子孫なら一員`() {
        assertTrue(hierarchy.isInstanceOf(noteOf("n", tags = listOf("Other", "Hybrid")), TagId("Gift")))
    }

    @Test
    fun `自己参照は循環として検出される`() {
        assertTrue(hierarchy.wouldCreateCycle(TagId("Book"), setOf(TagId("Book"))))
        assertEquals(
            ids("Book", "Book"),
            hierarchy.findCyclePath(TagId("Book"), setOf(TagId("Book")))
        )
    }

    @Test
    fun `間接循環は検出されて経路が返る`() {
        assertTrue(hierarchy.wouldCreateCycle(TagId("Item"), setOf(TagId("Ebook"))))
        assertEquals(
            ids("Item", "Ebook", "Book", "Item"),
            hierarchy.findCyclePath(TagId("Item"), setOf(TagId("Ebook")))
        )
    }

    @Test
    fun `循環しない親の変更は許される`() {
        assertFalse(hierarchy.wouldCreateCycle(TagId("Gift"), setOf(TagId("Item"))))
        assertFalse(hierarchy.wouldCreateCycle(TagId("Other"), setOf(TagId("Ebook"), TagId("Gift"))))
        assertNull(hierarchy.findCyclePath(TagId("Other"), emptySet()))
    }

    @Test
    fun `新規タグを既存の親に付けても循環しない`() {
        assertFalse(hierarchy.wouldCreateCycle(TagId("New"), setOf(TagId("Book"))))
        assertNotNull(hierarchy.findCyclePath(TagId("Book"), setOf(TagId("Hybrid"))))
    }

    @Test
    fun `存在しない親IDは無視される`() {
        val h = TagHierarchy(tagMap(tagOf("A", parents = listOf("ghost"))))
        assertEquals(emptyList<TagId>(), h.ancestorsOf(TagId("A")))
    }

    @Test
    fun `スナップショットに循環があっても停止する`() {
        val h = TagHierarchy(tagMap(tagOf("a", parents = listOf("b")), tagOf("b", parents = listOf("a"))))
        assertEquals(ids("b"), h.ancestorsOf(TagId("a")))
        assertEquals(setOf(TagId("b")), h.descendantsOf(TagId("a")))
    }
}
