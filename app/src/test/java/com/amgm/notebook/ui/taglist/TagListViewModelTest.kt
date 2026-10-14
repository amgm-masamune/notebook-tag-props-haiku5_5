@file:OptIn(ExperimentalCoroutinesApi::class)

package com.amgm.notebook.ui.taglist

import com.amgm.notebook.domain.defOf
import com.amgm.notebook.domain.model.TagId
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TagListViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `一覧は親タグ名と定義数を持つ`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("book", name = "書籍", defs = listOf(defOf("price"), defOf("author"))))
        repos.tags.save(tagOf("ebook", name = "電子書籍", parents = listOf("book")))
        val vm = TagListViewModel(repos.tags)
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        val book = state.items.single { it.id == TagId("book") }
        val ebook = state.items.single { it.id == TagId("ebook") }
        assertEquals(emptyList<String>(), book.parentNames)
        assertEquals(2, book.ownDefinitionCount)
        assertEquals(listOf("書籍"), ebook.parentNames)
        assertEquals(0, ebook.ownDefinitionCount)
    }

    @Test
    fun `削除は確認を経て実行され子タグの親から外れる`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("p"))
        repos.tags.save(tagOf("c", parents = listOf("p")))
        val vm = TagListViewModel(repos.tags)
        val events = recordEvents(vm.events)
        advanceUntilIdle()

        vm.onDeleteTagClick(TagId("p"))
        assertEquals(TagId("p"), vm.uiState.value.pendingDelete?.id)

        vm.onDeleteDismiss()
        assertNull(vm.uiState.value.pendingDelete)
        assertEquals(2, repos.tags.snapshot.size)

        vm.onDeleteTagClick(TagId("p"))
        vm.onDeleteConfirm()
        advanceUntilIdle()

        assertNull(vm.uiState.value.pendingDelete)
        assertEquals(listOf(TagId("c")), vm.uiState.value.items.map { it.id })
        assertTrue(repos.tags.snapshot.getValue(TagId("c")).parentIds.isEmpty())
        assertEquals(listOf<UiEvent>(UiEvent.ShowMessage(UiMessage.Deleted)), events)
    }

    @Test
    fun `存在しないタグの削除確認は何もしない`() = runTest {
        val vm = TagListViewModel(InMemoryRepositories().tags)
        advanceUntilIdle()
        vm.onDeleteTagClick(TagId("none"))
        assertNull(vm.uiState.value.pendingDelete)
    }

    @Test
    fun `各操作が正しいNavigateを発行する`() = runTest {
        val vm = TagListViewModel(InMemoryRepositories().tags)
        val events = recordEvents(vm.events)

        vm.onTagClick(TagId("t"))
        vm.onEditTagClick(TagId("t"))
        vm.onCreateTagClick()
        vm.onBackClick()
        advanceUntilIdle()

        assertEquals(
            listOf<UiEvent>(
                UiEvent.Navigate(Destination.TagTable(TagId("t"))),
                UiEvent.Navigate(Destination.TagEdit(TagId("t"))),
                UiEvent.Navigate(Destination.TagEdit(null)),
                UiEvent.NavigateBack
            ),
            events
        )
    }
    @Test
    fun `一覧は親子関係でネストした行を深さ付きで持つ`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("book", name = "書籍"))
        repos.tags.save(tagOf("ebook", name = "電子書籍", parents = listOf("book")))
        repos.tags.save(tagOf("manga", name = "電子漫画", parents = listOf("ebook")))
        repos.tags.save(tagOf("food", name = "食品"))
        val vm = TagListViewModel(repos.tags)
        advanceUntilIdle()

        val items = vm.uiState.value.items
        assertEquals(listOf("書籍", "電子書籍", "電子漫画", "食品"), items.map { it.name })
        assertEquals(listOf(0, 1, 2, 0), items.map { it.depth })
        assertTrue(items.none { it.isAdditionalParentOccurrence })
    }

    @Test
    fun `複数の親を持つタグは親ごとに出現し2回目以降は複数親の印が付く`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("a", name = "A"))
        repos.tags.save(tagOf("b", name = "B"))
        repos.tags.save(tagOf("c", name = "C", parents = listOf("a", "b")))
        val vm = TagListViewModel(repos.tags)
        advanceUntilIdle()

        val items = vm.uiState.value.items
        assertEquals(listOf("A", "C", "B", "C"), items.map { it.name })
        assertEquals(listOf(0, 1, 0, 1), items.map { it.depth })
        assertEquals(listOf(false, false, false, true), items.map { it.isAdditionalParentOccurrence })
    }
}
