package com.amgm.notebook.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amgm.notebook.data.db.NotebookDatabase
import com.amgm.notebook.data.db.entity.NoteEntity
import com.amgm.notebook.data.db.entity.NotePropertyEntity
import com.amgm.notebook.data.db.entity.NoteTagEntity
import com.amgm.notebook.data.db.entity.TagEntity
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
class NoteDaoTest {

    private lateinit var db: NotebookDatabase

    @Before
    fun setUp() {
        db = NotebookDatabase.inMemory(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun note(id: String, title: String = id, updatedAt: Long = 1_000L) =
        NoteEntity(id = id, title = title, body = "body-$id", createdAt = 500L, updatedAt = updatedAt)

    private suspend fun insertTag(id: String) {
        db.tagDao().replaceTag(TagEntity(id = id, name = "name-$id", createdAt = 0L), emptyList(), emptyList())
    }

    @Test
    fun replaceNote_then_getById_returnsTagLinksAndProperties() = runBlocking<Unit> {
        insertTag("t1")
        insertTag("t2")
        db.noteDao().replaceNote(
            note("n1", "題名"),
            listOf(NoteTagEntity("n1", "t1"), NoteTagEntity("n1", "t2")),
            listOf(
                NotePropertyEntity("n1", "price", "NUMBER", "12.5"),
                NotePropertyEntity("n1", "memo", "TEXT", "hello")
            )
        )

        val loaded = db.noteDao().getById("n1")
        assertNotNull(loaded)
        loaded!!
        assertEquals("題名", loaded.note.title)
        assertEquals("body-n1", loaded.note.body)
        assertEquals(500L, loaded.note.createdAt)
        assertEquals(setOf("t1", "t2"), loaded.tagLinks.map { it.tagId }.toSet())
        assertEquals(setOf("price", "memo"), loaded.properties.map { it.name }.toSet())
        assertEquals("12.5", loaded.properties.first { it.name == "price" }.valueText)
    }

    @Test
    fun getById_unknownId_returnsNull() = runBlocking<Unit> {
        assertNull(db.noteDao().getById("missing"))
    }

    @Test
    fun replaceNote_existingId_updatesAndReplacesLinks() = runBlocking<Unit> {
        insertTag("t1")
        insertTag("t2")
        db.noteDao().replaceNote(
            note("n1", "旧"),
            listOf(NoteTagEntity("n1", "t1")),
            listOf(NotePropertyEntity("n1", "a", "TEXT", "1"))
        )
        db.noteDao().replaceNote(
            note("n1", "新", updatedAt = 2_000L),
            listOf(NoteTagEntity("n1", "t2")),
            listOf(NotePropertyEntity("n1", "b", "TEXT", "2"))
        )

        val loaded = db.noteDao().getById("n1")!!
        assertEquals("新", loaded.note.title)
        assertEquals(2_000L, loaded.note.updatedAt)
        assertEquals(listOf("t2"), loaded.tagLinks.map { it.tagId })
        assertEquals(listOf("b"), loaded.properties.map { it.name })
        assertEquals(1, db.noteDao().observeAll().first().size)
    }

    @Test
    fun deleteById_removesNoteAndCascadesChildren() = runBlocking<Unit> {
        insertTag("t1")
        db.noteDao().replaceNote(
            note("n1"),
            listOf(NoteTagEntity("n1", "t1")),
            listOf(NotePropertyEntity("n1", "a", "TEXT", "1"))
        )
        db.noteDao().deleteById("n1")

        assertNull(db.noteDao().getById("n1"))
        assertTrue(db.noteDao().observeAll().first().isEmpty())
        // タグ自体は残る
        assertNotNull(db.tagDao().getById("t1"))
    }

    @Test
    fun observeAll_isOrderedByUpdatedAtDescending() = runBlocking<Unit> {
        db.noteDao().replaceNote(note("old", updatedAt = 100L), emptyList(), emptyList())
        db.noteDao().replaceNote(note("new", updatedAt = 300L), emptyList(), emptyList())
        db.noteDao().replaceNote(note("mid", updatedAt = 200L), emptyList(), emptyList())

        val ids = withTimeout(5000) { db.noteDao().observeAll().first() }.map { it.note.id }
        assertEquals(listOf("new", "mid", "old"), ids)
    }

    @Test
    fun observeAll_reflectsLaterChanges() = runBlocking<Unit> {
        val flow = db.noteDao().observeAll()
        assertTrue(withTimeout(5000) { flow.first() }.isEmpty())

        db.noteDao().replaceNote(note("n1"), emptyList(), emptyList())
        assertEquals(listOf("n1"), withTimeout(5000) { flow.first() }.map { it.note.id })

        db.noteDao().deleteById("n1")
        assertTrue(withTimeout(5000) { flow.first() }.isEmpty())
    }

    @Test
    fun observeById_emitsNullThenNoteAfterInsert() = runBlocking<Unit> {
        val flow = db.noteDao().observeById("n1")
        assertNull(withTimeout(5000) { flow.first() })

        db.noteDao().replaceNote(note("n1", "題名"), emptyList(), emptyList())
        assertEquals("題名", withTimeout(5000) { flow.first() }?.note?.title)
    }

    @Test
    fun observeByTagIds_returnsDistinctNotesHavingAnyTag() = runBlocking<Unit> {
        insertTag("t1")
        insertTag("t2")
        insertTag("t3")
        db.noteDao().replaceNote(
            note("both", updatedAt = 300L),
            listOf(NoteTagEntity("both", "t1"), NoteTagEntity("both", "t2")),
            emptyList()
        )
        db.noteDao().replaceNote(note("only1", updatedAt = 200L), listOf(NoteTagEntity("only1", "t1")), emptyList())
        db.noteDao().replaceNote(note("only3", updatedAt = 100L), listOf(NoteTagEntity("only3", "t3")), emptyList())
        db.noteDao().replaceNote(note("none", updatedAt = 50L), emptyList(), emptyList())

        val ids = withTimeout(5000) { db.noteDao().observeByTagIds(listOf("t1", "t2")).first() }.map { it.note.id }
        assertEquals(listOf("both", "only1"), ids)
    }

    @Test
    fun deletingTag_removesNoteTagLinkOnly() = runBlocking<Unit> {
        insertTag("t1")
        insertTag("t2")
        db.noteDao().replaceNote(
            note("n1"),
            listOf(NoteTagEntity("n1", "t1"), NoteTagEntity("n1", "t2")),
            emptyList()
        )
        db.tagDao().deleteById("t1")

        val loaded = db.noteDao().getById("n1")!!
        assertEquals(listOf("t2"), loaded.tagLinks.map { it.tagId })
    }
}
