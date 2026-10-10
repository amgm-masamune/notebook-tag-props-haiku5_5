@file:OptIn(ExperimentalCoroutinesApi::class)

package com.amgm.notebook.ui.noteedit

import com.amgm.notebook.domain.defOf
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.noteOf
import com.amgm.notebook.domain.tagOf
import com.amgm.notebook.testing.InMemoryRepositories
import com.amgm.notebook.testutil.MainDispatcherRule
import com.amgm.notebook.testutil.SequentialIdGenerator
import com.amgm.notebook.testutil.TestClock
import com.amgm.notebook.testutil.recordEvents
import com.amgm.notebook.ui.common.UiEvent
import com.amgm.notebook.ui.navigation.Destination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/** v2: H1 自動同期・タグ作成導線・初期タグ・NoteLink（ノート紐付け）の ViewModel テスト。 */
class NoteEditViewModelV2Test {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val clock = TestClock(Instant.ofEpochMilli(5_000L))
    private val ids = SequentialIdGenerator()
    private val related = PropertyName("related")

    private suspend fun TestScope.createViewModel(
        repos: InMemoryRepositories,
        noteId: NoteId? = null,
        initialTagId: TagId? = null
    ): NoteEditViewModel {
        val vm = NoteEditViewModel(noteId, repos.notes, repos.tags, ids, clock, initialTagId)
        advanceUntilIdle()
        return vm
    }

    /** book タグ(related: NoteLink→book 絞り込み既定)、other タグ、ノート n1(book,"Alpha") n2(other,"Beta") n3(book,"Gamma")。 */
    private suspend fun InMemoryRepositories.seedLinks() {
        tags.save(
            tagOf(
                "book",
                name = "書籍",
                defs = listOf(defOf("related", PropertyType.NoteLink(TagId("book"))))
            )
        )
        tags.save(tagOf("other", name = "その他"))
        notes.save(noteOf("n1", tags = listOf("book"), title = "Alpha", updated = 1L))
        notes.save(noteOf("n2", tags = listOf("other"), title = "Beta", updated = 2L))
        notes.save(noteOf("n3", tags = listOf("book"), title = "Gamma", updated = 3L))
    }

    private fun NoteEditUiState.row(name: PropertyName): PropertyRowUi =
        propertyRows.single { it.name == name }

    // ---- H1 自動同期 ----------------------------------------------------------

    @Test
    fun `新規ノートはトグルONが規定でタイトル入力で本文が見出しになる`() = runTest {
        val repos = InMemoryRepositories()
        val vm = createViewModel(repos)
        assertTrue(vm.uiState.value.autoSyncTitleH1)
        assertEquals("", vm.uiState.value.body)

        vm.onTitleChange("議事録")
        assertEquals("# 議事録\n\n", vm.uiState.value.body)

        vm.onTitleChange("議事録 2")
        assertEquals("# 議事録 2\n\n", vm.uiState.value.body)

        vm.onSaveClick()
        advanceUntilIdle()
        val saved = repos.notes.snapshot.values.single()
        assertTrue(saved.body.startsWith("# 議事録 2"))
        assertTrue(saved.autoSyncTitleH1)
    }

    @Test
    fun `トグルONでタイトルを変えると本文の最初のh1だけが変わる`() = runTest {
        val repos = InMemoryRepositories()
        repos.notes.save(noteOf("n1", title = "旧").copy(body = "# 旧\n\n本文\n\n# 別の見出し"))
        val vm = createViewModel(repos, NoteId("n1"))
        assertTrue(vm.uiState.value.autoSyncTitleH1)

        vm.onTitleChange("新")

        assertEquals("# 新\n\n本文\n\n# 別の見出し", vm.uiState.value.body)
    }

    @Test
    fun `トグルOFFではタイトルを変えても本文は変わらず保存値にも反映される`() = runTest {
        val repos = InMemoryRepositories()
        repos.notes.save(noteOf("n1", title = "旧").copy(body = "# 旧\n\n本文"))
        val vm = createViewModel(repos, NoteId("n1"))

        vm.onAutoSyncTitleH1Change(false)
        assertFalse(vm.uiState.value.autoSyncTitleH1)
        assertEquals("# 旧\n\n本文", vm.uiState.value.body)

        vm.onTitleChange("新")
        assertEquals("# 旧\n\n本文", vm.uiState.value.body)

        vm.onSaveClick()
        advanceUntilIdle()
        val saved = repos.notes.snapshot.getValue(NoteId("n1"))
        assertEquals("新", saved.title)
        assertEquals("# 旧\n\n本文", saved.body)
        assertFalse(saved.autoSyncTitleH1)

        // 再度開くと保存された値(OFF)が復元される
        val reopened = createViewModel(repos, NoteId("n1"))
        assertFalse(reopened.uiState.value.autoSyncTitleH1)
    }

    @Test
    fun `トグルを切り替えただけでは本文は変わらない`() = runTest {
        val repos = InMemoryRepositories()
        repos.notes.save(noteOf("n1", title = "旧").copy(body = "# 旧\n\n本文", autoSyncTitleH1 = false))
        val vm = createViewModel(repos, NoteId("n1"))
        assertFalse(vm.uiState.value.autoSyncTitleH1)

        vm.onAutoSyncTitleH1Change(true)

        assertTrue(vm.uiState.value.autoSyncTitleH1)
        assertEquals("# 旧\n\n本文", vm.uiState.value.body)
    }

    @Test
    fun `h1が無い本文にONでタイトルを入れると先頭にh1が挿入される`() = runTest {
        val repos = InMemoryRepositories()
        repos.notes.save(noteOf("n1", title = "").copy(body = "本文だけ"))
        val vm = createViewModel(repos, NoteId("n1"))

        vm.onTitleChange("題")

        assertEquals("# 題\n\n本文だけ", vm.uiState.value.body)
    }

    // ---- タグ作成への導線・初期タグ ----------------------------------------------

    @Test
    fun `タグを作成はTagEdit新規へ遷移し入力中の値とタグ追加が保持される`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("a", name = "A"))
        val vm = createViewModel(repos)
        val events = recordEvents(vm.events)
        vm.onTitleChange("書きかけ")
        vm.onTagToggle(TagId("a"))

        vm.onCreateTagClick()
        assertEquals(listOf<UiEvent>(UiEvent.Navigate(Destination.TagEdit(null))), events)

        // タグ作成画面で保存された想定: Flow で自動反映され、編集中の値は失われない
        repos.tags.save(tagOf("b", name = "B"))
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals(listOf("A", "B"), s.availableTags.map { it.name })
        assertEquals("書きかけ", s.title)
        assertEquals(setOf(TagId("a")), s.selectedTagIds)
        assertTrue(s.isDirty)
    }

    @Test
    fun `initialTagIdが存在すれば新規ノートで初期選択され未編集のままとなる`() = runTest {
        val repos = InMemoryRepositories()
        repos.seedLinks()

        val vm = createViewModel(repos, initialTagId = TagId("book"))

        val s = vm.uiState.value
        assertEquals(setOf(TagId("book")), s.selectedTagIds)
        assertEquals(listOf("related"), s.propertyRows.map { it.name.value })
        assertFalse(s.isDirty)
    }

    @Test
    fun `存在しないinitialTagIdは無視される`() = runTest {
        val repos = InMemoryRepositories()
        repos.seedLinks()

        val vm = createViewModel(repos, initialTagId = TagId("missing"))

        assertTrue(vm.uiState.value.selectedTagIds.isEmpty())
    }

    // ---- NoteLink -----------------------------------------------------------

    @Test
    fun `ピッカーの絞り込み初期値は定義のtargetTagIdで候補はそのタグのノートだけ`() = runTest {
        val repos = InMemoryRepositories()
        repos.seedLinks()
        val vm = createViewModel(repos, initialTagId = TagId("book"))

        vm.onOpenNotePicker(related)

        val picker = vm.uiState.value.picker!!
        assertEquals(TagId("book"), picker.filterTagId)
        assertEquals(listOf(NoteId("n1"), NoteId("n3")), picker.candidates.map { it.id })
    }

    @Test
    fun `絞り込みをすべてにすると全ノートが候補になり再びタグで絞れる`() = runTest {
        val repos = InMemoryRepositories()
        repos.seedLinks()
        val vm = createViewModel(repos, initialTagId = TagId("book"))
        vm.onOpenNotePicker(related)

        vm.onPickerFilterChange(null)
        var picker = vm.uiState.value.picker!!
        assertNull(picker.filterTagId)
        assertEquals(listOf(NoteId("n1"), NoteId("n2"), NoteId("n3")), picker.candidates.map { it.id })

        vm.onPickerFilterChange(TagId("other"))
        picker = vm.uiState.value.picker!!
        assertEquals(listOf(NoteId("n2")), picker.candidates.map { it.id })
    }

    @Test
    fun `検索語でタイトルを絞り込める`() = runTest {
        val repos = InMemoryRepositories()
        repos.seedLinks()
        val vm = createViewModel(repos, initialTagId = TagId("book"))
        vm.onOpenNotePicker(related)
        vm.onPickerFilterChange(null)

        vm.onPickerQueryChange("amm")

        assertEquals(listOf(NoteId("n3")), vm.uiState.value.picker!!.candidates.map { it.id })
    }

    @Test
    fun `選択を確定するとNoteRefsになり保存される`() = runTest {
        val repos = InMemoryRepositories()
        repos.seedLinks()
        val vm = createViewModel(repos, initialTagId = TagId("book"))
        vm.onTitleChange("リンク元")
        vm.onOpenNotePicker(related)

        vm.onPickerToggle(NoteId("n3"))
        vm.onPickerToggle(NoteId("n1"))
        vm.onPickerToggle(NoteId("n3")) // 外す
        vm.onPickerToggle(NoteId("n3")) // 再度付ける（順序は選択順）
        vm.onPickerConfirm()

        val s = vm.uiState.value
        assertNull(s.picker)
        assertEquals(PropertyValue.NoteRefs(listOf(NoteId("n1"), NoteId("n3"))), s.row(related).value)
        assertNull(s.row(related).error)
        assertTrue(s.isDirty)

        vm.onSaveClick()
        advanceUntilIdle()
        val saved = repos.notes.snapshot.getValue(NoteId("id-1"))
        assertEquals(
            PropertyValue.NoteRefs(listOf(NoteId("n1"), NoteId("n3"))),
            saved.properties[related]
        )
    }

    @Test
    fun `ピッカーを閉じるだけなら値は変わらない`() = runTest {
        val repos = InMemoryRepositories()
        repos.seedLinks()
        val vm = createViewModel(repos, initialTagId = TagId("book"))
        vm.onOpenNotePicker(related)
        vm.onPickerToggle(NoteId("n1"))

        vm.onPickerDismiss()

        assertNull(vm.uiState.value.picker)
        assertEquals(PropertyValue.Empty, vm.uiState.value.row(related).value)
    }

    @Test
    fun `選択済みの参照を外せて全部外すと未入力に戻る`() = runTest {
        val repos = InMemoryRepositories()
        repos.seedLinks()
        repos.notes.save(
            noteOf(
                "host", tags = listOf("book"), title = "Host",
                props = mapOf("related" to PropertyValue.NoteRefs(listOf(NoteId("n1"), NoteId("n3"))))
            )
        )
        val vm = createViewModel(repos, NoteId("host"))
        assertEquals(PropertyType.NoteLink(TagId("book")), vm.uiState.value.row(related).type)

        vm.onRemoveNoteRef(related, NoteId("n1"))
        assertEquals(PropertyValue.NoteRefs(listOf(NoteId("n3"))), vm.uiState.value.row(related).value)

        vm.onRemoveNoteRef(related, NoteId("n3"))
        assertEquals(PropertyValue.Empty, vm.uiState.value.row(related).value)
        assertEquals("", vm.uiState.value.row(related).input)
    }

    @Test
    fun `存在しない参照IDはタイトルが引けず自分自身は候補に出ない`() = runTest {
        val repos = InMemoryRepositories()
        repos.seedLinks()
        repos.notes.save(
            noteOf(
                "host", tags = listOf("book"), title = "Host",
                props = mapOf("related" to PropertyValue.NoteRefs(listOf(NoteId("n1"), NoteId("gone"))))
            )
        )
        val vm = createViewModel(repos, NoteId("host"))

        val s = vm.uiState.value
        assertEquals("Alpha", s.noteTitles[NoteId("n1")])
        assertFalse(NoteId("gone") in s.noteTitles) // UI は「（削除済み）」表示
        // 参照先が存在しなくても保存は拒否されない
        assertTrue(s.issues.none { it.severity == com.amgm.notebook.domain.rules.Severity.ERROR })

        vm.onOpenNotePicker(related)
        val picker = vm.uiState.value.picker!!
        assertEquals(listOf(NoteId("n1"), NoteId("n3")), picker.candidates.map { it.id })
        assertTrue(NoteId("host") !in picker.candidates.map { it.id })
    }

    @Test
    fun `自由プロパティのNoteLinkは絞り込みなしで選べる`() = runTest {
        val repos = InMemoryRepositories()
        repos.seedLinks()
        val vm = createViewModel(repos)

        vm.onAddFreeProperty("参照", PropertyType.NoteLink(null))
        val name = PropertyName("参照")
        vm.onOpenNotePicker(name)
        val picker = vm.uiState.value.picker!!
        assertNull(picker.filterTagId)
        assertEquals(3, picker.candidates.size)

        vm.onPickerToggle(NoteId("n2"))
        vm.onPickerConfirm()

        assertEquals(PropertyValue.NoteRefs(listOf(NoteId("n2"))), vm.uiState.value.row(name).value)
    }

    @Test
    fun `NoteLink以外の行ではピッカーは開かない`() = runTest {
        val repos = InMemoryRepositories()
        repos.seedLinks()
        val vm = createViewModel(repos)
        vm.onAddFreeProperty("メモ", PropertyType.Text)

        vm.onOpenNotePicker(PropertyName("メモ"))
        vm.onOpenNotePicker(PropertyName("none"))

        assertNull(vm.uiState.value.picker)
    }

    @Test
    fun `定義の対象タグが削除済みなら絞り込みはすべてになる`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("host", defs = listOf(defOf("related", PropertyType.NoteLink(TagId("gone"))))))
        repos.notes.save(noteOf("n1", title = "Alpha"))
        val vm = createViewModel(repos, initialTagId = TagId("host"))

        vm.onOpenNotePicker(related)

        assertNull(vm.uiState.value.picker!!.filterTagId)
        assertEquals(1, vm.uiState.value.picker!!.candidates.size)
    }
}
