package com.amgm.notebook.domain.repository

import com.amgm.notebook.domain.error.CyclicInheritanceException
import com.amgm.notebook.domain.error.DuplicateTagNameException
import com.amgm.notebook.domain.error.TagNotFoundException
import com.amgm.notebook.domain.model.PropertyDefinition
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.Tag
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.testing.assertThrowsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** TagRepository の契約テスト。JVM では InMemory、実機では Room に対して継承して実行する。 */
abstract class TagRepositoryContract {

    abstract fun createRepositories(): Pair<NoteRepository, TagRepository>

    private fun tags(): TagRepository = createRepositories().second

    private fun tag(
        id: String,
        name: String = id,
        parents: Set<String> = emptySet(),
        defs: List<PropertyDefinition> = emptyList()
    ) = Tag(TagId(id), name, parents.map { TagId(it) }.toSet(), defs)

    private fun def(name: String, type: PropertyType = PropertyType.Text, required: Boolean = false) =
        PropertyDefinition(PropertyName(name), type, required)

    @Test
    fun `保存したタグを取得できる`() = runBlocking<Unit> {
        val repo = tags()
        val saved = tag("t1", "書籍", defs = listOf(def("price", PropertyType.Number, true), def("author")))
        repo.save(saved)
        assertEquals(saved, repo.getTag(TagId("t1")))
        assertEquals(listOf(saved), repo.getAllTags())
        assertEquals(saved, withTimeout(5000) { repo.observeTag(TagId("t1")).first() })
    }

    @Test
    fun `存在しないタグはnull`() = runBlocking<Unit> {
        val repo = tags()
        assertNull(repo.getTag(TagId("none")))
        assertNull(withTimeout(5000) { repo.observeTag(TagId("none")).first() })
    }

    @Test
    fun `プロパティ定義の順序が保持される`() = runBlocking<Unit> {
        val repo = tags()
        val defs = listOf(def("z"), def("a", PropertyType.Date), def("m", PropertyType.Boolean, true))
        repo.save(tag("t1", defs = defs))
        assertEquals(defs, repo.getTag(TagId("t1"))!!.ownPropertyDefinitions)
    }

    @Test
    fun `再保存で更新される`() = runBlocking<Unit> {
        val repo = tags()
        repo.save(tag("t1", "旧名", defs = listOf(def("a"))))
        repo.save(tag("t1", "新名", defs = listOf(def("b", PropertyType.Number))))
        val loaded = repo.getTag(TagId("t1"))!!
        assertEquals("新名", loaded.name)
        assertEquals(listOf(def("b", PropertyType.Number)), loaded.ownPropertyDefinitions)
        assertEquals(1, repo.getAllTags().size)
    }

    @Test
    fun `タグ一覧は名前の昇順で大文字小文字を無視する`() = runBlocking<Unit> {
        val repo = tags()
        repo.save(tag("t1", "banana"))
        repo.save(tag("t2", "Apple"))
        repo.save(tag("t3", "cherry"))
        val names = withTimeout(5000) { repo.observeTags().first() }.map { it.name }
        assertEquals(listOf("Apple", "banana", "cherry"), names)
        assertEquals(names, repo.getAllTags().map { it.name })
    }

    @Test
    fun `複数の親を保存できる`() = runBlocking<Unit> {
        val repo = tags()
        repo.save(tag("a"))
        repo.save(tag("b"))
        repo.save(tag("c", parents = setOf("a", "b")))
        assertEquals(setOf(TagId("a"), TagId("b")), repo.getTag(TagId("c"))!!.parentIds)
    }

    @Test
    fun `保存の反映がFlowに流れる`() = runBlocking<Unit> {
        val repo = tags()
        repo.save(tag("t1", "first"))
        repo.save(tag("t2", "second"))
        val observed = withTimeout(5000) { repo.observeTags().first { it.size == 2 } }
        assertEquals(listOf("first", "second"), observed.map { it.name })
    }

    @Test
    fun `タグ削除で子の親から除去される`() = runBlocking<Unit> {
        val repo = tags()
        repo.save(tag("a"))
        repo.save(tag("b"))
        repo.save(tag("c", parents = setOf("a", "b")))
        repo.delete(TagId("a"))
        assertNull(repo.getTag(TagId("a")))
        assertEquals(setOf(TagId("b")), repo.getTag(TagId("c"))!!.parentIds)
    }

    @Test
    fun `存在しないタグの削除は何もしない`() = runBlocking<Unit> {
        val repo = tags()
        repo.save(tag("a"))
        repo.delete(TagId("zzz"))
        assertEquals(1, repo.getAllTags().size)
    }

    @Test
    fun `自己参照は循環として拒否される`() {
        val repo = tags()
        val e = assertThrowsInstance<CyclicInheritanceException> {
            runBlocking { repo.save(tag("a", parents = setOf("a"))) }
        }
        assertEquals(listOf(TagId("a"), TagId("a")), e.path)
        assertTrue(runBlocking { repo.getAllTags() }.isEmpty())
    }

    @Test
    fun `間接循環は拒否され既存データは変わらない`() = runBlocking<Unit> {
        val repo = tags()
        repo.save(tag("a"))
        repo.save(tag("b", parents = setOf("a")))
        repo.save(tag("c", parents = setOf("b")))
        assertThrowsInstance<CyclicInheritanceException> {
            runBlocking { repo.save(tag("a", parents = setOf("c"))) }
        }
        assertTrue(repo.getTag(TagId("a"))!!.parentIds.isEmpty())
    }

    @Test
    fun `存在しない親は拒否される`() {
        val repo = tags()
        val e = assertThrowsInstance<TagNotFoundException> {
            runBlocking { repo.save(tag("a", parents = setOf("ghost"))) }
        }
        assertEquals(TagId("ghost"), e.tagId)
        assertTrue(runBlocking { repo.getAllTags() }.isEmpty())
    }

    @Test
    fun `タグ名は大文字小文字を区別せず一意`() = runBlocking<Unit> {
        val repo = tags()
        repo.save(tag("a", "Book"))
        assertThrowsInstance<DuplicateTagNameException> {
            runBlocking { repo.save(tag("b", "bOOK")) }
        }
        assertEquals(1, repo.getAllTags().size)
        // 同じタグを同名で再保存するのは重複ではない
        repo.save(tag("a", "Book", defs = listOf(def("x"))))
        assertNotNull(repo.getTag(TagId("a")))
    }
}
