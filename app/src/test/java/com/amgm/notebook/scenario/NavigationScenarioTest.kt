@file:OptIn(ExperimentalCoroutinesApi::class)

package com.amgm.notebook.scenario

import com.amgm.notebook.domain.defOf
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.noteOf
import com.amgm.notebook.domain.tagOf
import com.amgm.notebook.testutil.MainDispatcherRule
import com.amgm.notebook.testutil.recordEvents
import com.amgm.notebook.ui.common.UiMessage
import com.amgm.notebook.ui.navigation.Destination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** architecture.md 4 章の遷移図どおりにバックスタックが動くことを確認する。 */
class NavigationScenarioTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val tagId = TagId("t")
    private val noteId = NoteId("n")

    private suspend fun ScenarioApp.seed() {
        repos.tags.save(tagOf("t", defs = listOf(defOf("price", PropertyType.Number))))
        repos.notes.save(noteOf("n", tags = listOf("t"), props = mapOf("price" to PropertyValue.Number(1.0))))
    }

    @Test
    fun `ノート一覧からタグ一覧と表ビューとノート編集をたどり戻りで元の画面へ戻る`() = runTest {
        val app = ScenarioApp()
        app.seed()
        val nav = app.navigator

        val noteList = app.noteList()
        val noteListEvents = recordEvents(noteList.events)
        noteList.onTagsClick()
        app.route(noteListEvents)
        assertEquals(listOf(Destination.NoteList, Destination.TagList), nav.backStack)

        val tagList = app.tagList()
        val tagListEvents = recordEvents(tagList.events)
        tagList.onTagClick(tagId)
        app.route(tagListEvents)
        assertEquals(
            listOf(Destination.NoteList, Destination.TagList, Destination.TagTable(tagId)),
            nav.backStack
        )

        val table = app.tagTable(tagId)
        val tableEvents = recordEvents(table.events)
        advanceUntilIdle()
        table.onRowClick(noteId)
        app.route(tableEvents)
        assertEquals(Destination.NoteEdit(noteId), nav.current)
        assertEquals(4, nav.backStack.size)

        // 未変更のノート編集から戻る → 表ビュー → タグ一覧 → ノート一覧
        val noteEdit = app.noteEdit(noteId)
        val noteEditEvents = recordEvents(noteEdit.events)
        advanceUntilIdle()
        noteEdit.onBackClick()
        app.route(noteEditEvents)
        assertEquals(Destination.TagTable(tagId), nav.current)

        table.onBackClick()
        app.route(tableEvents)
        assertEquals(Destination.TagList, nav.current)

        tagList.onBackClick()
        app.route(tagListEvents)
        assertEquals(listOf<Destination>(Destination.NoteList), nav.backStack)
    }

    @Test
    fun `表ビューからタグ編集へ行き戻るとタグ一覧の上に戻る`() = runTest {
        val app = ScenarioApp()
        app.seed()
        val nav = app.navigator

        nav.handle(com.amgm.notebook.ui.common.UiEvent.Navigate(Destination.TagList))
        nav.handle(com.amgm.notebook.ui.common.UiEvent.Navigate(Destination.TagTable(tagId)))

        val table = app.tagTable(tagId)
        val tableEvents = recordEvents(table.events)
        advanceUntilIdle()
        table.onEditTagClick()
        app.route(tableEvents)
        assertEquals(Destination.TagEdit(tagId), nav.current)

        val tagEdit = app.tagEdit(tagId)
        val tagEditEvents = recordEvents(tagEdit.events)
        advanceUntilIdle()
        tagEdit.onBackClick()
        app.route(tagEditEvents)
        assertEquals(Destination.TagTable(tagId), nav.current)
    }

    @Test
    fun `新規ノートの保存で一覧へ戻り編集中ノートの削除でも一覧へ戻る`() = runTest {
        val app = ScenarioApp()
        val nav = app.navigator
        val noteList = app.noteList()
        val noteListEvents = recordEvents(noteList.events)

        noteList.onCreateNoteClick()
        app.route(noteListEvents)
        assertEquals(Destination.NoteEdit(null), nav.current)

        val create = app.noteEdit(null)
        val createEvents = recordEvents(create.events)
        advanceUntilIdle()
        create.onTitleChange("メモ")
        create.onSaveClick()
        advanceUntilIdle()
        app.route(createEvents)
        assertEquals(listOf<Destination>(Destination.NoteList), nav.backStack)
        assertEquals(UiMessage.Saved, nav.messages.last().message)

        val savedId = app.repos.notes.snapshot.keys.single()
        noteList.onNoteClick(savedId)
        app.route(noteListEvents)
        assertEquals(Destination.NoteEdit(savedId), nav.current)

        val edit = app.noteEdit(savedId)
        val editEvents = recordEvents(edit.events)
        advanceUntilIdle()
        edit.onDeleteClick()
        advanceUntilIdle()
        app.route(editEvents)
        assertEquals(listOf<Destination>(Destination.NoteList), nav.backStack)
        assertEquals(UiMessage.Deleted, nav.messages.last().message)
        assertTrue(app.repos.notes.snapshot.isEmpty())
    }

    @Test
    fun `存在しないノートを開くと自動で戻る`() = runTest {
        val app = ScenarioApp()
        val nav = app.navigator
        nav.handle(com.amgm.notebook.ui.common.UiEvent.Navigate(Destination.NoteEdit(NoteId("missing"))))

        val edit = app.noteEdit(NoteId("missing"))
        val events = recordEvents(edit.events)
        advanceUntilIdle()
        app.route(events)
        assertEquals(listOf<Destination>(Destination.NoteList), nav.backStack)
    }
}
