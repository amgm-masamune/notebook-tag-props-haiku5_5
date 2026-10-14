@file:OptIn(ExperimentalCoroutinesApi::class)

package com.amgm.notebook.ui.noteedit

import com.amgm.notebook.domain.defOf
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.noteOf
import com.amgm.notebook.domain.rules.ValidationIssue
import com.amgm.notebook.domain.tagOf
import com.amgm.notebook.markdown.MarkdownBlock
import com.amgm.notebook.testing.InMemoryRepositories
import com.amgm.notebook.testutil.MainDispatcherRule
import com.amgm.notebook.testutil.SequentialIdGenerator
import com.amgm.notebook.testutil.TestClock
import com.amgm.notebook.testutil.recordEvents
import com.amgm.notebook.ui.common.UiEvent
import com.amgm.notebook.ui.common.UiMessage
import com.amgm.notebook.ui.navigation.Destination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class NoteEditViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val clock = TestClock(Instant.ofEpochMilli(5_000L))
    private val ids = SequentialIdGenerator()

    private fun newViewModel(repos: InMemoryRepositories, noteId: NoteId? = null) =
        NoteEditViewModel(noteId, repos.notes, repos.tags, ids, clock)

    private fun NoteEditUiState.row(name: String): PropertyRowUi =
        propertyRows.single { it.name == PropertyName(name) }

    /** 書籍(price:Number 必須, author:Text) と 電子書籍(親=書籍) を用意する。 */
    private suspend fun InMemoryRepositories.seedBookTags() {
        tags.save(
            tagOf(
                "book",
                name = "書籍",
                defs = listOf(defOf("price", PropertyType.Number, required = true), defOf("author"))
            )
        )
        tags.save(tagOf("ebook", name = "電子書籍", parents = listOf("book")))
    }

    private suspend fun TestScope.createViewModel(
        repos: InMemoryRepositories,
        noteId: NoteId? = null
    ): NoteEditViewModel {
        val vm = newViewModel(repos, noteId)
        advanceUntilIdle()
        return vm
    }

    @Test
    fun `新規作成の初期状態`() = runTest {
        val repos = InMemoryRepositories()
        repos.seedBookTags()
        val vm = newViewModel(repos)
        advanceUntilIdle()

        val s = vm.uiState.value
        assertTrue(s.isNew)
        assertFalse(s.isLoading)
        assertFalse(s.isDirty)
        assertFalse(s.notFound)
        assertEquals("", s.title)
        assertEquals(EditorMode.Edit, s.mode)
        assertEquals(listOf("書籍", "電子書籍"), s.availableTags.map { it.name })
        assertTrue(s.propertyRows.isEmpty())
        assertTrue(s.selectedTagIds.isEmpty())
    }

    @Test
    fun `タグを付けると定義済みプロパティ行が現れ外すと自由プロパティになる`() = runTest {
        val repos = InMemoryRepositories()
        repos.seedBookTags()
        val vm = createViewModel(repos)

        vm.onTagToggle(TagId("book"))
        var s = vm.uiState.value
        assertTrue(s.isDirty)
        assertEquals(listOf("price", "author"), s.propertyRows.map { it.name.value })
        val price = s.row("price")
        assertEquals(PropertyType.Number, price.type)
        assertTrue(price.required)
        assertEquals(PropertyOrigin.Defined(TagId("book")), price.origin)

        vm.onPropertyInputChange(PropertyName("price"), "1200")
        vm.onTagToggle(TagId("book"))
        s = vm.uiState.value
        assertEquals(listOf("author", "price"), s.propertyRows.map { it.name.value })
        assertTrue(s.propertyRows.all { it.origin == PropertyOrigin.Free })
        assertEquals(PropertyValue.Number(1200.0), s.row("price").value)
        assertNull(s.row("price").error)
    }

    @Test
    fun `親タグの定義を継承してプロパティ行が現れる`() = runTest {
        val repos = InMemoryRepositories()
        repos.seedBookTags()
        val vm = createViewModel(repos)

        vm.onTagToggle(TagId("ebook"))
        val s = vm.uiState.value
        assertEquals(listOf("price", "author"), s.propertyRows.map { it.name.value })
        assertEquals(PropertyOrigin.Defined(TagId("book")), s.row("price").origin)
    }

    @Test
    fun `型付き入力が解釈され解釈不能はUnparsableになる`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(
            tagOf(
                "t",
                defs = listOf(
                    defOf("n", PropertyType.Number),
                    defOf("d", PropertyType.Date),
                    defOf("b", PropertyType.Boolean)
                )
            )
        )
        val vm = createViewModel(repos)
        vm.onTagToggle(TagId("t"))

        vm.onPropertyInputChange(PropertyName("n"), "12.5")
        vm.onPropertyInputChange(PropertyName("d"), "2024-02-29")
        vm.onPropertyInputChange(PropertyName("b"), "true")
        var s = vm.uiState.value
        assertEquals(PropertyValue.Number(12.5), s.row("n").value)
        assertEquals(PropertyValue.Date(LocalDate.of(2024, 2, 29)), s.row("d").value)
        assertEquals(PropertyValue.Bool(true), s.row("b").value)
        assertTrue(s.propertyRows.all { it.error == null })

        vm.onPropertyInputChange(PropertyName("n"), "abc")
        vm.onPropertyInputChange(PropertyName("d"), "2024-13-01")
        s = vm.uiState.value
        assertEquals(PropertyValue.Empty, s.row("n").value)
        assertEquals("abc", s.row("n").input)
        assertEquals(PropertyInputError.Unparsable, s.row("n").error)
        assertEquals(PropertyInputError.Unparsable, s.row("d").error)

        vm.onPropertyInputChange(PropertyName("n"), "")
        assertNull(vm.uiState.value.row("n").error)
    }

    @Test
    fun `解釈不能な入力が残っていると保存できない`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("t", defs = listOf(defOf("n", PropertyType.Number))))
        val vm = createViewModel(repos)
        val events = recordEvents(vm.events)
        vm.onTagToggle(TagId("t"))
        vm.onPropertyInputChange(PropertyName("n"), "abc")

        vm.onSaveClick()
        advanceUntilIdle()

        assertEquals(listOf<UiEvent>(UiEvent.ShowMessage(UiMessage.SaveBlockedByErrors)), events)
        assertTrue(repos.notes.snapshot.isEmpty())
    }

    @Test
    fun `必須未入力は警告のまま保存できる`() = runTest {
        val repos = InMemoryRepositories()
        repos.seedBookTags()
        val vm = createViewModel(repos)
        val events = recordEvents(vm.events)
        vm.onTitleChange("無題の本")
        vm.onTagToggle(TagId("book"))

        val warning = vm.uiState.value
        assertEquals(PropertyInputError.RequiredMissing, warning.row("price").error)
        assertTrue(warning.issues.any { it is ValidationIssue.RequiredMissing })

        vm.onSaveClick()
        advanceUntilIdle()

        assertEquals(
            listOf<UiEvent>(UiEvent.ShowMessage(UiMessage.Saved), UiEvent.NavigateBack),
            events
        )
        val saved = repos.notes.snapshot.values.single()
        assertEquals("無題の本", saved.title)
        assertEquals(setOf(TagId("book")), saved.tagIds)
        assertEquals(PropertyValue.Empty, saved.properties[PropertyName("price")])
        assertFalse(vm.uiState.value.isDirty)
    }

    @Test
    fun `保存済みの値が型と一致しない場合は保存を拒否し修正すれば保存できる`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("t", defs = listOf(defOf("price", PropertyType.Number))))
        repos.notes.save(
            noteOf(
                "n1",
                tags = listOf("t"),
                props = mapOf("price" to PropertyValue.Text("abc")),
                title = "既存",
                updated = 10L
            )
        )
        val vm = createViewModel(repos, NoteId("n1"))
        val events = recordEvents(vm.events)

        val loaded = vm.uiState.value
        assertFalse(loaded.isNew)
        assertEquals(PropertyInputError.TypeMismatch, loaded.row("price").error)
        assertTrue(loaded.issues.any { it is ValidationIssue.TypeMismatch })

        vm.onSaveClick()
        advanceUntilIdle()
        assertEquals(listOf<UiEvent>(UiEvent.ShowMessage(UiMessage.SaveBlockedByErrors)), events)
        // リポジトリは未更新
        val untouched = repos.notes.snapshot.getValue(NoteId("n1"))
        assertEquals(Instant.ofEpochMilli(10L), untouched.updatedAt)
        assertEquals(PropertyValue.Text("abc"), untouched.properties[PropertyName("price")])

        vm.onPropertyInputChange(PropertyName("price"), "300")
        assertNull(vm.uiState.value.row("price").error)
        events.clear()
        vm.onSaveClick()
        advanceUntilIdle()
        assertEquals(
            listOf<UiEvent>(UiEvent.ShowMessage(UiMessage.Saved), UiEvent.NavigateBack),
            events
        )
        assertEquals(
            PropertyValue.Number(300.0),
            repos.notes.snapshot.getValue(NoteId("n1")).properties[PropertyName("price")]
        )
    }

    @Test
    fun `自由プロパティの追加と削除と重複名`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("t", defs = listOf(defOf("defined"))))
        val vm = createViewModel(repos)
        val events = recordEvents(vm.events)
        vm.onTagToggle(TagId("t"))

        vm.onAddFreeProperty("  memo  ", PropertyType.Text)
        vm.onAddFreeProperty("count", PropertyType.Number)
        var s = vm.uiState.value
        assertEquals(listOf("defined", "count", "memo"), s.propertyRows.map { it.name.value })
        assertEquals(PropertyOrigin.Free, s.row("memo").origin)
        assertEquals(PropertyType.Number, s.row("count").type)

        // 空名・重複名は追加されずメッセージ
        vm.onAddFreeProperty("", PropertyType.Text)
        vm.onAddFreeProperty("memo", PropertyType.Text)
        vm.onAddFreeProperty("defined", PropertyType.Text)
        advanceUntilIdle()
        assertEquals(3, vm.uiState.value.propertyRows.size)
        assertEquals(
            List(3) { UiEvent.ShowMessage(UiMessage.NameRequired) },
            events
        )

        // 定義済み行は削除できないが、自由行は削除できる
        vm.onRemoveFreeProperty(PropertyName("defined"))
        assertEquals(3, vm.uiState.value.propertyRows.size)
        vm.onRemoveFreeProperty(PropertyName("memo"))
        s = vm.uiState.value
        assertEquals(listOf("defined", "count"), s.propertyRows.map { it.name.value })
    }

    @Test
    fun `編集とプレビューの切替でpreviewDocumentが更新される`() = runTest {
        val vm = createViewModel(InMemoryRepositories())
        vm.onBodyChange("# 見出し\n\n**太字**")
        assertTrue(vm.uiState.value.previewDocument.blocks.isEmpty())

        vm.onModeChange(EditorMode.Preview)
        var s = vm.uiState.value
        assertEquals(EditorMode.Preview, s.mode)
        assertTrue(s.previewDocument.blocks.first() is MarkdownBlock.Heading)
        assertEquals(2, s.previewDocument.blocks.size)

        vm.onModeChange(EditorMode.Edit)
        s = vm.uiState.value
        assertEquals(EditorMode.Edit, s.mode)
        assertEquals("# 見出し\n\n**太字**", s.body)
    }

    @Test
    fun `未保存で戻ると確認ダイアログが出て継続または破棄できる`() = runTest {
        val vm = createViewModel(InMemoryRepositories())
        val events = recordEvents(vm.events)

        // 未変更なら即戻る
        vm.onBackClick()
        assertEquals(listOf<UiEvent>(UiEvent.NavigateBack), events)
        events.clear()

        vm.onTitleChange("編集した")
        vm.onBackClick()
        assertTrue(vm.uiState.value.showDiscardDialog)
        assertTrue(events.isEmpty())

        vm.onDiscardDismiss()
        assertFalse(vm.uiState.value.showDiscardDialog)
        assertTrue(events.isEmpty())

        vm.onBackClick()
        vm.onDiscardConfirm()
        assertFalse(vm.uiState.value.showDiscardDialog)
        assertEquals(listOf<UiEvent>(UiEvent.NavigateBack), events)
    }

    @Test
    fun `新規保存ではIDを採番し作成日時と更新日時はClockの値になる`() = runTest {
        val repos = InMemoryRepositories()
        val vm = createViewModel(repos)
        vm.onTitleChange("新規")
        vm.onSaveClick()
        advanceUntilIdle()

        val saved = repos.notes.snapshot.values.single()
        assertEquals(NoteId("id-1"), saved.id)
        assertEquals(Instant.ofEpochMilli(5_000L), saved.createdAt)
        assertEquals(Instant.ofEpochMilli(5_000L), saved.updatedAt)
    }

    @Test
    fun `既存ノートの更新ではcreatedAtを保持しupdatedAtがClockの値になる`() = runTest {
        val repos = InMemoryRepositories()
        repos.notes.save(noteOf("n1", title = "元", updated = 100L))
        val vm = createViewModel(repos, NoteId("n1"))
        assertEquals("元", vm.uiState.value.title)

        vm.onTitleChange("更新後")
        vm.onSaveClick()
        advanceUntilIdle()

        val saved = repos.notes.snapshot.getValue(NoteId("n1"))
        assertEquals("更新後", saved.title)
        assertEquals(Instant.ofEpochMilli(0L), saved.createdAt)
        assertEquals(Instant.ofEpochMilli(5_000L), saved.updatedAt)
        assertEquals(1, repos.notes.snapshot.size)
    }

    @Test
    fun `既存ノートを削除できる`() = runTest {
        val repos = InMemoryRepositories()
        repos.notes.save(noteOf("n1"))
        val vm = createViewModel(repos, NoteId("n1"))
        val events = recordEvents(vm.events)

        vm.onDeleteClick()
        advanceUntilIdle()

        assertTrue(repos.notes.snapshot.isEmpty())
        assertEquals(
            listOf<UiEvent>(UiEvent.ShowMessage(UiMessage.Deleted), UiEvent.NavigateBack),
            events
        )
    }

    @Test
    fun `新規ノートでは削除は何もしない`() = runTest {
        val vm = createViewModel(InMemoryRepositories())
        val events = recordEvents(vm.events)
        vm.onDeleteClick()
        advanceUntilIdle()
        assertTrue(events.isEmpty())
    }

    @Test
    fun `存在しないIDは新規扱いにせずnotFoundで戻る`() = runTest {
        val repos = InMemoryRepositories()
        val vm = newViewModel(repos, NoteId("missing"))
        val events = recordEvents(vm.events)
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.notFound)
        assertFalse(vm.uiState.value.isLoading)
        assertEquals(listOf<UiEvent>(UiEvent.NavigateBack), events)
        assertTrue(repos.notes.snapshot.isEmpty())
    }

    @Test
    fun `選択タグの表で見るはTagTableへ遷移する`() = runTest {
        val vm = createViewModel(InMemoryRepositories())
        val events = recordEvents(vm.events)
        vm.onTagTableClick(TagId("book"))
        assertEquals(
            listOf<UiEvent>(UiEvent.Navigate(Destination.TagTable(TagId("book")))),
            events
        )
    }

    @Test
    fun `既存ノートの読込でタグとプロパティが復元される`() = runTest {
        val repos = InMemoryRepositories()
        repos.seedBookTags()
        repos.notes.save(
            noteOf(
                "n1",
                tags = listOf("ebook"),
                props = mapOf(
                    "price" to PropertyValue.Number(980.0),
                    "memo" to PropertyValue.Text("セール")
                )
            )
        )
        val vm = createViewModel(repos, NoteId("n1"))
        val s = vm.uiState.value

        assertEquals(setOf(TagId("ebook")), s.selectedTagIds)
        assertEquals(listOf("price", "author", "memo"), s.propertyRows.map { it.name.value })
        assertEquals("980", s.row("price").input)
        assertEquals(PropertyOrigin.Free, s.row("memo").origin)
        assertEquals(PropertyType.Text, s.row("memo").type)
        assertNotNull(s.row("author"))
        assertFalse(s.isDirty)
    }
}
