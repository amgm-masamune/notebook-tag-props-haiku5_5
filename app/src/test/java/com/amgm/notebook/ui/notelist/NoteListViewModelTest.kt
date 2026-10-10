@file:OptIn(ExperimentalCoroutinesApi::class)

package com.amgm.notebook.ui.notelist

import com.amgm.notebook.domain.defOf
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.noteOf
import com.amgm.notebook.domain.tagOf
import com.amgm.notebook.testing.InMemoryRepositories
import com.amgm.notebook.testutil.MainDispatcherRule
import com.amgm.notebook.testutil.recordEvents
import com.amgm.notebook.ui.common.UiEvent
import com.amgm.notebook.ui.common.UiMessage
import com.amgm.notebook.ui.navigation.Destination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NoteListViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun newViewModel(repos: InMemoryRepositories) =
        NoteListViewModel(repos.notes, repos.tags)

    @Test
    fun `初期ロードでisLoadingがtrueからfalseになる`() = runTest {
        val vm = newViewModel(InMemoryRepositories())
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.items.isEmpty())
    }

    @Test
    fun `一覧は更新日時の降順でタグ名とスニペットを持つ`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("t1", name = "仕事"))
        repos.notes.save(noteOf("old", title = "古い", updated = 1L))
        repos.notes.save(noteOf("new", tags = listOf("t1"), title = "新しい", updated = 3L))
        repos.notes.save(noteOf("mid", title = "中間", updated = 2L))
        val vm = newViewModel(repos)
        advanceUntilIdle()

        val items = vm.uiState.value.items
        assertEquals(listOf("new", "mid", "old"), items.map { it.id.value })
        assertEquals(listOf("仕事"), items.first().tagNames)
        assertEquals("新しい", items.first().title)
    }

    @Test
    fun `本文のスニペットは先頭80文字`() = runTest {
        val repos = InMemoryRepositories()
        repos.notes.save(noteOf("n").copy(body = "あ".repeat(200)))
        val vm = newViewModel(repos)
        advanceUntilIdle()
        assertEquals(80, vm.uiState.value.items.single().snippet.length)
    }

    @Test
    fun `検索で絞り込まれ空文字で戻る`() = runTest {
        val repos = InMemoryRepositories()
        repos.notes.save(noteOf("a", title = "買い物リスト"))
        repos.notes.save(noteOf("b", title = "会議メモ"))
        val vm = newViewModel(repos)
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.items.size)

        vm.onQueryChange("会議")
        advanceUntilIdle()
        assertEquals("会議", vm.uiState.value.query)
        assertEquals(listOf("b"), vm.uiState.value.items.map { it.id.value })

        vm.onQueryChange("")
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.items.size)
    }

    @Test
    fun `タグで絞り込むと子孫タグのノートも含まれ再選択で解除される`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("book", defs = listOf(defOf("price"))))
        repos.tags.save(tagOf("ebook", parents = listOf("book")))
        repos.tags.save(tagOf("other"))
        repos.notes.save(noteOf("n1", tags = listOf("book"), updated = 1L))
        repos.notes.save(noteOf("n2", tags = listOf("ebook"), updated = 2L))
        repos.notes.save(noteOf("n3", tags = listOf("other"), updated = 3L))
        val vm = newViewModel(repos)
        advanceUntilIdle()

        vm.onTagFilterChange(TagId("book"))
        advanceUntilIdle()
        assertEquals(TagId("book"), vm.uiState.value.filterTagId)
        assertEquals(listOf("n2", "n1"), vm.uiState.value.items.map { it.id.value })

        vm.onTagFilterChange(TagId("book"))
        advanceUntilIdle()
        assertEquals(null, vm.uiState.value.filterTagId)
        assertEquals(3, vm.uiState.value.items.size)
    }

    @Test
    fun `ノートの追加が一覧に反映される`() = runTest {
        val repos = InMemoryRepositories()
        val vm = newViewModel(repos)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.items.isEmpty())

        repos.notes.save(noteOf("n", title = "追加"))
        advanceUntilIdle()
        assertEquals(listOf("n"), vm.uiState.value.items.map { it.id.value })
    }

    @Test
    fun `削除するとメッセージが出て一覧から消える`() = runTest {
        val repos = InMemoryRepositories()
        repos.notes.save(noteOf("n"))
        val vm = newViewModel(repos)
        val events = recordEvents(vm.events)
        advanceUntilIdle()

        vm.onDeleteNote(NoteId("n"))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.items.isEmpty())
        assertEquals(listOf<UiEvent>(UiEvent.ShowMessage(UiMessage.Deleted)), events)
    }

    @Test
    fun `各操作が正しいNavigateを発行する`() = runTest {
        val vm = newViewModel(InMemoryRepositories())
        val events = recordEvents(vm.events)

        vm.onNoteClick(NoteId("n1"))
        vm.onCreateNoteClick()
        vm.onTagsClick()
        advanceUntilIdle()

        assertEquals(
            listOf<UiEvent>(
                UiEvent.Navigate(Destination.NoteEdit(NoteId("n1"))),
                UiEvent.Navigate(Destination.NoteEdit(null)),
                UiEvent.Navigate(Destination.TagList)
            ),
            events
        )
    }
}
