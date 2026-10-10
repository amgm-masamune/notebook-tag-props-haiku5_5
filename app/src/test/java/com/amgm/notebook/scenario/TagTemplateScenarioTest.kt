@file:OptIn(ExperimentalCoroutinesApi::class)

package com.amgm.notebook.scenario

import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.testutil.MainDispatcherRule
import com.amgm.notebook.testutil.recordEvents
import com.amgm.notebook.ui.common.UiMessage
import com.amgm.notebook.ui.navigation.Destination
import com.amgm.notebook.ui.noteedit.PropertyOrigin
import com.amgm.notebook.ui.tagedit.PropertyDefinitionDraft
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** ユーザーが操作をひと通り行う擬似シナリオ: タグ作成 → プロパティ定義 → ノート作成 → タグ付与 → 表に現れる。 */
class TagTemplateScenarioTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `タグを作りノートに付与して親タグの表ビューで確認できる`() = runTest {
        val app = ScenarioApp()
        val nav = app.navigator

        // 起動: ノート一覧 → タグ一覧
        val noteList = app.noteList()
        val noteListEvents = recordEvents(noteList.events)
        advanceUntilIdle()
        assertTrue(noteList.uiState.value.items.isEmpty())
        noteList.onTagsClick()
        app.route(noteListEvents)
        assertEquals(listOf(Destination.NoteList, Destination.TagList), nav.backStack)

        // ① タグ「書籍」を作成（price:Number required, author:Text）
        val tagList = app.tagList()
        val tagListEvents = recordEvents(tagList.events)
        advanceUntilIdle()
        tagList.onCreateTagClick()
        app.route(tagListEvents)
        assertEquals(Destination.TagEdit(null), nav.current)

        val bookEdit = app.tagEdit(null)
        val bookEditEvents = recordEvents(bookEdit.events)
        advanceUntilIdle()
        bookEdit.onNameChange("書籍")
        bookEdit.onAddDefinition()
        bookEdit.onDefinitionChange(0, PropertyDefinitionDraft("price", PropertyType.Number, true))
        bookEdit.onAddDefinition()
        bookEdit.onDefinitionChange(1, PropertyDefinitionDraft("author", PropertyType.Text, false))
        bookEdit.onSaveClick()
        advanceUntilIdle()
        app.route(bookEditEvents)
        assertEquals(Destination.TagList, nav.current)
        assertEquals(UiMessage.Saved, nav.messages.last().message)
        val bookId = TagId("id-1")
        assertEquals(listOf("書籍"), tagList.uiState.value.items.map { it.name })

        // ② 子タグ「電子書籍」(親=書籍) を作成
        tagList.onCreateTagClick()
        app.route(tagListEvents)
        val ebookEdit = app.tagEdit(null)
        val ebookEditEvents = recordEvents(ebookEdit.events)
        advanceUntilIdle()
        ebookEdit.onNameChange("電子書籍")
        ebookEdit.onParentToggle(bookId)
        assertEquals(
            listOf("price", "author"),
            ebookEdit.uiState.value.inheritedSchema.definitions.map { it.definition.name.value }
        )
        ebookEdit.onSaveClick()
        advanceUntilIdle()
        app.route(ebookEditEvents)
        assertEquals(Destination.TagList, nav.current)
        val ebookId = TagId("id-2")
        assertEquals(listOf("書籍", "電子書籍"), tagList.uiState.value.items.map { it.name })
        assertEquals(listOf("書籍"), tagList.uiState.value.items.last().parentNames)

        // ③ ノート作成で「電子書籍」を付与 → 継承された price / author 行が出る
        tagList.onBackClick()
        app.route(tagListEvents)
        assertEquals(Destination.NoteList, nav.current)
        noteList.onCreateNoteClick()
        app.route(noteListEvents)
        assertEquals(Destination.NoteEdit(null), nav.current)

        val noteEdit = app.noteEdit(null)
        val noteEditEvents = recordEvents(noteEdit.events)
        advanceUntilIdle()
        noteEdit.onTitleChange("Kotlin入門")
        noteEdit.onTagToggle(ebookId)
        val rows = noteEdit.uiState.value.propertyRows
        assertEquals(listOf("price", "author"), rows.map { it.name.value })
        assertTrue(rows.all { it.origin == PropertyOrigin.Defined(bookId) })

        // ④ 値を入力して保存
        noteEdit.onPropertyInputChange(PropertyName("price"), "1200")
        noteEdit.onPropertyInputChange(PropertyName("author"), "山田")
        noteEdit.onSaveClick()
        advanceUntilIdle()
        app.route(noteEditEvents)
        assertEquals(Destination.NoteList, nav.current)
        val noteId = NoteId("id-3")
        val saved = app.repos.notes.snapshot.getValue(noteId)
        assertEquals(setOf(ebookId), saved.tagIds)
        assertEquals(PropertyValue.Number(1200.0), saved.properties[PropertyName("price")])
        assertEquals(PropertyValue.Text("山田"), saved.properties[PropertyName("author")])
        assertEquals(listOf("Kotlin入門"), noteList.uiState.value.items.map { it.title })
        assertEquals(listOf("電子書籍"), noteList.uiState.value.items.single().tagNames)

        // ⑤ 親タグ「書籍」の表ビューに当該ノートが現れ price 列に値が出る
        noteList.onTagsClick()
        app.route(noteListEvents)
        tagList.onTagClick(bookId)
        app.route(tagListEvents)
        assertEquals(Destination.TagTable(bookId), nav.current)

        val table = app.tagTable(bookId)
        val tableEvents = recordEvents(table.events)
        advanceUntilIdle()
        val view = table.uiState.value.table!!
        assertEquals(listOf("price", "author"), view.columns.map { it.name.value })
        val row = view.rows.single()
        assertEquals(noteId, row.noteId)
        assertEquals("Kotlin入門", row.title)
        assertEquals(listOf(PropertyValue.Number(1200.0), PropertyValue.Text("山田")), row.cells)

        // ⑥ 行タップでノート編集へ遷移し、既存値が復元される
        table.onRowClick(noteId)
        app.route(tableEvents)
        assertEquals(Destination.NoteEdit(noteId), nav.current)
        val reopened = app.noteEdit(noteId)
        advanceUntilIdle()
        assertEquals("Kotlin入門", reopened.uiState.value.title)
        assertEquals("1200", reopened.uiState.value.propertyRows.first { it.name.value == "price" }.input)
        assertEquals(setOf(ebookId), reopened.uiState.value.selectedTagIds)
    }
}
