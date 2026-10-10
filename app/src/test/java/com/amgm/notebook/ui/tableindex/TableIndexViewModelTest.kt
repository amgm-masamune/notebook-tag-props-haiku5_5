@file:OptIn(ExperimentalCoroutinesApi::class)

package com.amgm.notebook.ui.tableindex

import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.noteOf
import com.amgm.notebook.domain.tagOf
import com.amgm.notebook.testing.InMemoryRepositories
import com.amgm.notebook.testutil.MainDispatcherRule
import com.amgm.notebook.testutil.recordEvents
import com.amgm.notebook.ui.common.UiEvent
import com.amgm.notebook.ui.navigation.Destination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TableIndexViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `全タグが名前順に並び子孫タグを含むノート件数を持つ`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("book", name = "書籍"))
        repos.tags.save(tagOf("ebook", name = "電子書籍", parents = listOf("book")))
        repos.tags.save(tagOf("idea", name = "アイデア"))
        repos.notes.save(noteOf("n1", tags = listOf("book")))
        repos.notes.save(noteOf("n2", tags = listOf("ebook")))
        repos.notes.save(noteOf("n3", tags = listOf("ebook", "book")))
        val vm = TableIndexViewModel(repos.tags, repos.notes)
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(setOf("book", "ebook", "idea"), state.rows.map { it.tagId.value }.toSet())
        assertEquals(3, state.rows.size)
        assertEquals(3, state.rows.single { it.tagId == TagId("book") }.noteCount)
        assertEquals(2, state.rows.single { it.tagId == TagId("ebook") }.noteCount)
        assertEquals(0, state.rows.single { it.tagId == TagId("idea") }.noteCount)
        assertEquals(state.rows.map { it.name.lowercase() }.sorted(), state.rows.map { it.name.lowercase() })
    }

    @Test
    fun `ノートの増減で件数が更新される`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("t"))
        val vm = TableIndexViewModel(repos.tags, repos.notes)
        advanceUntilIdle()
        assertEquals(0, vm.uiState.value.rows.single().noteCount)

        repos.notes.save(noteOf("n1", tags = listOf("t")))
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.rows.single().noteCount)
    }

    @Test
    fun `タグが無ければ空の一覧`() = runTest {
        val repos = InMemoryRepositories()
        val vm = TableIndexViewModel(repos.tags, repos.notes)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.rows.isEmpty())
    }

    @Test
    fun `行のタップでそのタグの表ビューへ遷移する`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("t"))
        val vm = TableIndexViewModel(repos.tags, repos.notes)
        val events = recordEvents(vm.events)
        advanceUntilIdle()

        vm.onTagClick(TagId("t"))
        advanceUntilIdle()

        assertEquals(listOf<UiEvent>(UiEvent.Navigate(Destination.TagTable(TagId("t")))), events)
    }
}
