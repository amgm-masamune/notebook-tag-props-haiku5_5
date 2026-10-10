package com.amgm.notebook.domain.repository

import com.amgm.notebook.domain.error.TagNotFoundException
import com.amgm.notebook.domain.model.Note
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.model.Tag
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.testing.assertThrowsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** NoteRepository の契約テスト。JVM では InMemory、実機では Room に対して継承して実行する。 */
abstract class NoteRepositoryContract {

    abstract fun createRepositories(): Pair<NoteRepository, TagRepository>

    private fun at(millis: Long): Instant = Instant.ofEpochMilli(millis)

    private fun note(
        id: String,
        title: String = id,
        body: String = "",
        tags: Set<String> = emptySet(),
        props: Map<String, PropertyValue> = emptyMap(),
        updated: Long = 1_000
    ) = Note(
        id = NoteId(id),
        title = title,
        body = body,
        tagIds = tags.map { TagId(it) }.toSet(),
        properties = props.entries.associate { PropertyName(it.key) to it.value },
        createdAt = at(500),
        updatedAt = at(updated)
    )

    private suspend fun TagRepository.saveSimple(id: String, name: String = id, parents: Set<String> = emptySet()) {
        save(Tag(TagId(id), name, parents.map { TagId(it) }.toSet(), emptyList()))
    }

    private suspend fun NoteRepository.titles(query: String = ""): List<String> =
        withTimeout(5000) { observeNotes(query).first() }.map { it.title }

    @Test
    fun `保存したノートを取得できる`() = runBlocking<Unit> {
        val (notes, tags) = createRepositories()
        tags.saveSimple("t1")
        val saved = note(
            "n1", title = "題名", body = "本文 **x**", tags = setOf("t1"),
            props = mapOf("price" to PropertyValue.Number(12.5))
        )
        notes.save(saved)
        assertEquals(saved, notes.getNote(NoteId("n1")))
        assertEquals(saved, withTimeout(5000) { notes.observeNote(NoteId("n1")).first() })
    }

    @Test
    fun `存在しないノートはnull`() = runBlocking<Unit> {
        val (notes, _) = createRepositories()
        assertNull(notes.getNote(NoteId("none")))
        assertNull(withTimeout(5000) { notes.observeNote(NoteId("none")).first() })
    }

    @Test
    fun `全種類のプロパティ値が往復する`() = runBlocking<Unit> {
        val (notes, _) = createRepositories()
        val props: Map<String, PropertyValue> = mapOf(
            "text" to PropertyValue.Text("あ い\nう"),
            "emptyText" to PropertyValue.Text(""),
            "num" to PropertyValue.Number(-0.125),
            "yes" to PropertyValue.Bool(true),
            "no" to PropertyValue.Bool(false),
            "date" to PropertyValue.Date(LocalDate.of(2024, 2, 29)),
            "empty" to PropertyValue.Empty
        )
        notes.save(note("n1", props = props))
        assertEquals(note("n1", props = props).properties, notes.getNote(NoteId("n1"))!!.properties)
    }

    @Test
    fun `再保存で上書きされタグとプロパティが入れ替わる`() = runBlocking<Unit> {
        val (notes, tags) = createRepositories()
        tags.saveSimple("t1")
        tags.saveSimple("t2")
        notes.save(note("n1", title = "旧", tags = setOf("t1"), props = mapOf("a" to PropertyValue.Text("1"))))
        notes.save(note("n1", title = "新", tags = setOf("t2"), props = mapOf("b" to PropertyValue.Text("2")), updated = 2_000))
        val loaded = notes.getNote(NoteId("n1"))!!
        assertEquals("新", loaded.title)
        assertEquals(setOf(TagId("t2")), loaded.tagIds)
        assertEquals(setOf(PropertyName("b")), loaded.properties.keys)
        assertEquals(1, notes.titles().size)
    }

    @Test
    fun `一覧は更新日時の降順`() = runBlocking<Unit> {
        val (notes, _) = createRepositories()
        notes.save(note("a", updated = 1_000))
        notes.save(note("b", updated = 3_000))
        notes.save(note("c", updated = 2_000))
        assertEquals(listOf("b", "c", "a"), notes.titles())
    }

    @Test
    fun `検索はタイトルと本文の部分一致で大文字小文字を無視する`() = runBlocking<Unit> {
        val (notes, _) = createRepositories()
        notes.save(note("a", title = "Kotlin Memo", body = "x", updated = 3_000))
        notes.save(note("b", title = "買い物", body = "milk and KOTLIN cookies", updated = 2_000))
        notes.save(note("c", title = "other", body = "none", updated = 1_000))
        assertEquals(listOf("Kotlin Memo", "買い物"), notes.titles("kotlin"))
        assertEquals(listOf("買い物"), notes.titles("買い"))
        assertEquals(3, notes.titles("").size)
        assertTrue(notes.titles("存在しない").isEmpty())
    }

    @Test
    fun `タグIDで絞り込める`() = runBlocking<Unit> {
        val (notes, tags) = createRepositories()
        tags.saveSimple("t1")
        tags.saveSimple("t2")
        tags.saveSimple("t3")
        notes.save(note("a", tags = setOf("t1"), updated = 1_000))
        notes.save(note("b", tags = setOf("t2"), updated = 2_000))
        notes.save(note("c", tags = setOf("t1", "t2"), updated = 3_000))
        notes.save(note("d", tags = setOf("t3"), updated = 4_000))
        notes.save(note("e", updated = 5_000))
        val result = withTimeout(5000) { notes.observeNotesByTagIds(setOf(TagId("t1"), TagId("t2"))).first() }
        assertEquals(listOf("c", "b", "a"), result.map { it.title })
    }

    @Test
    fun `空のタグ集合では空リスト`() = runBlocking<Unit> {
        val (notes, _) = createRepositories()
        notes.save(note("a"))
        assertTrue(withTimeout(5000) { notes.observeNotesByTagIds(emptySet()).first() }.isEmpty())
    }

    @Test
    fun `未知のタグを付けたノートは拒否される`() {
        val (notes, _) = createRepositories()
        val e = assertThrowsInstance<TagNotFoundException> {
            runBlocking { notes.save(note("a", tags = setOf("ghost"))) }
        }
        assertEquals(TagId("ghost"), e.tagId)
        assertNull(runBlocking { notes.getNote(NoteId("a")) })
    }

    @Test
    fun `ノート削除でノートが消える`() = runBlocking<Unit> {
        val (notes, tags) = createRepositories()
        tags.saveSimple("t1")
        notes.save(note("a", tags = setOf("t1")))
        notes.delete(NoteId("a"))
        assertNull(notes.getNote(NoteId("a")))
        assertTrue(notes.titles().isEmpty())
        assertTrue(withTimeout(5000) { notes.observeNotesByTagIds(setOf(TagId("t1"))).first() }.isEmpty())
    }

    @Test
    fun `存在しないノートの削除は何もしない`() = runBlocking<Unit> {
        val (notes, _) = createRepositories()
        notes.save(note("a"))
        notes.delete(NoteId("zzz"))
        assertEquals(listOf("a"), notes.titles())
    }

    @Test
    fun `タグ削除でノートのタグが除去されプロパティ値は残る`() = runBlocking<Unit> {
        val (notes, tags) = createRepositories()
        tags.saveSimple("t1")
        tags.saveSimple("t2")
        notes.save(
            note("a", tags = setOf("t1", "t2"), props = mapOf("price" to PropertyValue.Number(3.0)))
        )
        tags.delete(TagId("t1"))
        val loaded = notes.getNote(NoteId("a"))!!
        assertEquals(setOf(TagId("t2")), loaded.tagIds)
        assertEquals(PropertyValue.Number(3.0), loaded.properties[PropertyName("price")])
    }

    @Test
    fun `保存の反映がFlowに流れる`() = runBlocking<Unit> {
        val (notes, _) = createRepositories()
        notes.save(note("a", title = "first", updated = 1_000))
        notes.save(note("b", title = "second", updated = 2_000))
        val observed = withTimeout(5000) { notes.observeNotes().first { it.size == 2 } }
        assertEquals(listOf("second", "first"), observed.map { it.title })
    }
}
