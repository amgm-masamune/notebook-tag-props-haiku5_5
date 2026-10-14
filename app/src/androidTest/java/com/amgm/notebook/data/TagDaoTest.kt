package com.amgm.notebook.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amgm.notebook.data.db.NotebookDatabase
import com.amgm.notebook.data.db.entity.NoteEntity
import com.amgm.notebook.data.db.entity.NoteTagEntity
import com.amgm.notebook.data.db.entity.TagEntity
import com.amgm.notebook.data.db.entity.TagParentEntity
import com.amgm.notebook.data.db.entity.TagPropertyDefinitionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TagDaoTest {

    private lateinit var db: NotebookDatabase

    @Before
    fun setUp() {
        db = NotebookDatabase.inMemory(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun tag(id: String, name: String = "name-$id", createdAt: Long = 10L) =
        TagEntity(id = id, name = name, createdAt = createdAt)

    private fun def(tagId: String, name: String, type: String = "TEXT", required: Boolean = false, position: Int = 0) =
        TagPropertyDefinitionEntity(tagId, name, type, required, position)

    private suspend fun put(
        tag: TagEntity,
        parents: List<String> = emptyList(),
        defs: List<TagPropertyDefinitionEntity> = emptyList()
    ) {
        db.tagDao().replaceTag(tag, parents.map { TagParentEntity(tag.id, it) }, defs)
    }

    @Test
    fun replaceTag_then_getById_returnsParentsAndDefinitions() = runBlocking<Unit> {
        put(tag("root"))
        put(tag("other"))
        put(
            tag("child", "子"),
            parents = listOf("root", "other"),
            defs = listOf(
                def("child", "price", "NUMBER", required = true, position = 0),
                def("child", "memo", "TEXT", position = 1)
            )
        )

        val loaded = db.tagDao().getById("child")
        assertNotNull(loaded)
        loaded!!
        assertEquals("子", loaded.tag.name)
        assertEquals(10L, loaded.tag.createdAt)
        assertEquals(setOf("root", "other"), loaded.parentLinks.map { it.parentId }.toSet())
        assertEquals(setOf("price", "memo"), loaded.definitions.map { it.name }.toSet())
        val price = loaded.definitions.first { it.name == "price" }
        assertEquals("NUMBER", price.type)
        assertTrue(price.required)
        assertEquals(0, price.position)
        assertEquals(1, loaded.definitions.first { it.name == "memo" }.position)
    }

    @Test
    fun getById_unknownId_returnsNull() = runBlocking<Unit> {
        assertNull(db.tagDao().getById("missing"))
    }

    @Test
    fun replaceTag_existingId_updatesAndReplacesLinks() = runBlocking<Unit> {
        put(tag("p1"))
        put(tag("p2"))
        put(tag("c", "旧"), parents = listOf("p1"), defs = listOf(def("c", "a")))
        put(tag("c", "新"), parents = listOf("p2"), defs = listOf(def("c", "b")))

        val loaded = db.tagDao().getById("c")!!
        assertEquals("新", loaded.tag.name)
        assertEquals(listOf("p2"), loaded.parentLinks.map { it.parentId })
        assertEquals(listOf("b"), loaded.definitions.map { it.name })
        assertEquals(3, db.tagDao().getAll().size)
    }

    @Test
    fun getAll_returnsEveryTag() = runBlocking<Unit> {
        put(tag("a"))
        put(tag("b"))
        assertEquals(setOf("a", "b"), db.tagDao().getAll().map { it.tag.id }.toSet())
    }

    @Test
    fun findExistingIds_returnsOnlyStoredIds() = runBlocking<Unit> {
        put(tag("a"))
        put(tag("b"))
        assertEquals(setOf("a", "b"), db.tagDao().findExistingIds(listOf("a", "b", "zzz")).toSet())
        assertTrue(db.tagDao().findExistingIds(listOf("zzz")).isEmpty())
    }

    @Test
    fun deleteById_cascadesToParentLinksDefinitionsAndNoteLinks() = runBlocking<Unit> {
        put(tag("parent"))
        put(tag("child"), parents = listOf("parent"), defs = listOf(def("child", "a")))
        put(tag("grand"), parents = listOf("child"))
        db.noteDao().replaceNote(
            NoteEntity("n1", "t", "b", 1L, 2L),
            listOf(NoteTagEntity("n1", "child")),
            emptyList()
        )

        db.tagDao().deleteById("child")

        assertNull(db.tagDao().getById("child"))
        // 子タグ(grand)の親リンクだけが消え、grand 自体は残る
        val grand = db.tagDao().getById("grand")!!
        assertTrue(grand.parentLinks.isEmpty())
        assertNotNull(db.tagDao().getById("parent"))
        // ノートは残り、タグリンクのみ消える
        val note = db.noteDao().getById("n1")!!
        assertTrue(note.tagLinks.isEmpty())
    }

    @Test
    fun observeAll_reflectsInsertAndDelete() = runBlocking<Unit> {
        val flow = db.tagDao().observeAll()
        assertTrue(withTimeout(5000) { flow.first() }.isEmpty())

        put(tag("a"))
        assertEquals(listOf("a"), withTimeout(5000) { flow.first() }.map { it.tag.id })

        db.tagDao().deleteById("a")
        assertTrue(withTimeout(5000) { flow.first() }.isEmpty())
    }

    @Test
    fun observeById_reflectsInheritanceUpdate() = runBlocking<Unit> {
        put(tag("p"))
        put(tag("c"))
        val flow = db.tagDao().observeById("c")
        assertTrue(withTimeout(5000) { flow.first() }!!.parentLinks.isEmpty())

        put(tag("c"), parents = listOf("p"))
        assertEquals(listOf("p"), withTimeout(5000) { flow.first() }!!.parentLinks.map { it.parentId })
    }

    @Test
    fun tagName_isUniqueCaseInsensitive() = runBlocking<Unit> {
        put(tag("a", "Book"))
        // insertTag は IGNORE なので -1 が返り、行は増えない
        val result = db.tagDao().insertTag(tag("b", "BOOK"))
        assertEquals(-1L, result)
        assertEquals(1, db.tagDao().getAll().size)
    }
}
