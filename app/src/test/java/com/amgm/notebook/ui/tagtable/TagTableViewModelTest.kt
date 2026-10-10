@file:OptIn(ExperimentalCoroutinesApi::class)

package com.amgm.notebook.ui.tagtable

import com.amgm.notebook.domain.defOf
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.noteOf
import com.amgm.notebook.domain.service.TableSort
import com.amgm.notebook.domain.service.TableSortKey
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TagTableViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val priceKey = TableSortKey.Property(PropertyName("price"))

    private suspend fun InMemoryRepositories.seed() {
        tags.save(
            tagOf(
                "book",
                name = "書籍",
                defs = listOf(defOf("price", PropertyType.Number, required = true), defOf("author"))
            )
        )
        tags.save(tagOf("ebook", name = "電子書籍", parents = listOf("book")))
        tags.save(tagOf("other", name = "その他", defs = listOf(defOf("price", PropertyType.Number))))
    }

    private fun newViewModel(repos: InMemoryRepositories, tagId: String = "book") =
        TagTableViewModel(TagId(tagId), repos.tags, repos.notes)

    @Test
    fun `列は実効定義順で行は子孫タグのノートを含み無関係は除外される`() = runTest {
        val repos = InMemoryRepositories()
        repos.seed()
        repos.notes.save(
            noteOf(
                "n1", tags = listOf("book"), title = "本1", updated = 1L,
                props = mapOf("price" to PropertyValue.Number(1000.0), "author" to PropertyValue.Text("甲"))
            )
        )
        repos.notes.save(
            noteOf(
                "n2", tags = listOf("ebook"), title = "電子本", updated = 2L,
                props = mapOf("price" to PropertyValue.Number(500.0))
            )
        )
        repos.notes.save(
            noteOf("n3", tags = listOf("other"), title = "無関係", updated = 3L,
                props = mapOf("price" to PropertyValue.Number(1.0)))
        )
        val vm = newViewModel(repos)
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.isLoading)
        assertTrue(s.tagFound)
        assertEquals("書籍", s.tagName)
        val table = s.table!!
        assertEquals(listOf("price", "author"), table.columns.map { it.name.value })
        assertEquals(listOf("n2", "n1"), table.rows.map { it.noteId.value })
        val n2 = table.rows[0]
        assertEquals("電子本", n2.title)
        assertEquals(listOf(PropertyValue.Number(500.0), PropertyValue.Null), n2.cells)
        val n1 = table.rows[1]
        assertEquals(listOf(PropertyValue.Number(1000.0), PropertyValue.Text("甲")), n1.cells)
    }

    @Test
    fun `子タグの表には親タグのノートは出ない`() = runTest {
        val repos = InMemoryRepositories()
        repos.seed()
        repos.notes.save(noteOf("n1", tags = listOf("book")))
        repos.notes.save(noteOf("n2", tags = listOf("ebook")))
        val vm = newViewModel(repos, "ebook")
        advanceUntilIdle()
        assertEquals(listOf("n2"), vm.uiState.value.table!!.rows.map { it.noteId.value })
    }

    @Test
    fun `ノートの追加とタグ変更に追従する`() = runTest {
        val repos = InMemoryRepositories()
        repos.seed()
        val vm = newViewModel(repos)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.table!!.rows.isEmpty())

        repos.notes.save(noteOf("n1", tags = listOf("book"), updated = 1L))
        advanceUntilIdle()
        assertEquals(listOf("n1"), vm.uiState.value.table!!.rows.map { it.noteId.value })

        repos.notes.save(noteOf("n1", tags = listOf("other"), updated = 2L))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.table!!.rows.isEmpty())

        repos.notes.save(noteOf("n1", tags = listOf("ebook"), updated = 3L))
        advanceUntilIdle()
        assertEquals(listOf("n1"), vm.uiState.value.table!!.rows.map { it.noteId.value })
    }

    @Test
    fun `ソートは昇順から降順から解除の順に切り替わる`() = runTest {
        val repos = InMemoryRepositories()
        repos.seed()
        listOf("n1" to 1000.0, "n2" to 500.0, "n3" to 2000.0).forEachIndexed { i, (id, price) ->
            repos.notes.save(
                noteOf(id, tags = listOf("book"), updated = (i + 1).toLong(),
                    props = mapOf("price" to PropertyValue.Number(price)))
            )
        }
        val vm = newViewModel(repos)
        advanceUntilIdle()
        fun order() = vm.uiState.value.table!!.rows.map { it.noteId.value }

        assertNull(vm.uiState.value.sort)
        assertEquals(listOf("n3", "n2", "n1"), order())

        vm.onSortChange(priceKey)
        advanceUntilIdle()
        assertEquals(TableSort(priceKey, true), vm.uiState.value.sort)
        assertEquals(listOf("n2", "n1", "n3"), order())

        vm.onSortChange(priceKey)
        advanceUntilIdle()
        assertEquals(TableSort(priceKey, false), vm.uiState.value.sort)
        assertEquals(listOf("n3", "n1", "n2"), order())

        vm.onSortChange(priceKey)
        advanceUntilIdle()
        assertNull(vm.uiState.value.sort)
        assertEquals(listOf("n3", "n2", "n1"), order())

        // 別の列に切り替えると昇順から始まる
        vm.onSortChange(priceKey)
        vm.onSortChange(TableSortKey.Title)
        advanceUntilIdle()
        assertEquals(TableSort(TableSortKey.Title, true), vm.uiState.value.sort)
    }

    @Test
    fun `行タップと編集と戻るが正しいイベントを発行する`() = runTest {
        val repos = InMemoryRepositories()
        repos.seed()
        val vm = newViewModel(repos)
        val events = recordEvents(vm.events)
        advanceUntilIdle()

        vm.onRowClick(NoteId("n1"))
        vm.onEditTagClick()
        vm.onBackClick()

        assertEquals(
            listOf<UiEvent>(
                UiEvent.Navigate(Destination.NoteEdit(NoteId("n1"))),
                UiEvent.Navigate(Destination.TagEdit(TagId("book"))),
                UiEvent.NavigateBack
            ),
            events
        )
    }

    @Test
    fun `存在しないタグはtagFoundがfalseで戻るイベントが一度だけ出る`() = runTest {
        val repos = InMemoryRepositories()
        repos.seed()
        val vm = newViewModel(repos, "missing")
        val events = recordEvents(vm.events)
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.isLoading)
        assertFalse(s.tagFound)
        assertNull(s.table)

        repos.tags.save(tagOf("another"))
        advanceUntilIdle()
        assertEquals(listOf<UiEvent>(UiEvent.NavigateBack), events)
    }

    @Test
    fun `親同士の型衝突が表の衝突に伝わる`() = runTest {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("a", defs = listOf(defOf("x", PropertyType.Number))))
        repos.tags.save(tagOf("b", defs = listOf(defOf("x", PropertyType.Text))))
        repos.tags.save(tagOf("c", parents = listOf("a", "b")))
        val vm = newViewModel(repos, "c")
        advanceUntilIdle()

        val table = vm.uiState.value.table
        assertNotNull(table)
        assertTrue(table!!.columns.isEmpty())
        assertEquals(PropertyName("x"), table.conflicts.single().name)
    }

    @Test
    fun `ノートを追加はこのタグを初期選択した新規ノート作成へ遷移する`() = runTest {
        val repos = InMemoryRepositories()
        repos.seed()
        val vm = newViewModel(repos)
        val events = recordEvents(vm.events)
        advanceUntilIdle()

        vm.onAddNoteClick()

        assertEquals(
            listOf<UiEvent>(UiEvent.Navigate(Destination.NoteEdit(null, TagId("book")))),
            events
        )
        assertEquals("note_edit?tagId=book", Destination.NoteEdit(null, TagId("book")).route)
    }

    @Test
    fun `NoteRefsセルは参照先タイトルのカンマ区切りで取れなければ件数になる`() {
        val titles = mapOf(NoteId("a") to "Alpha", NoteId("b") to "Beta", NoteId("c") to "")
        fun refs(vararg ids: String) = PropertyValue.NoteRefs(ids.map { NoteId(it) })

        assertEquals("Alpha, Beta", cellText(refs("a", "b"), titles))
        assertEquals("(無題)", cellText(refs("c"), titles))
        assertEquals("Alpha, （削除済み）", cellText(refs("a", "gone"), titles))
        assertEquals("2 件", cellText(refs("x", "y"), titles))
        assertEquals("", cellText(PropertyValue.Null, titles))
        assertEquals("1200", cellText(PropertyValue.Number(1200.0), titles))
    }

    @Test
    fun `noteTitlesに全ノートのタイトルが入る`() = runTest {
        val repos = InMemoryRepositories()
        repos.seed()
        repos.notes.save(noteOf("n9", tags = listOf("other"), title = "別タグ"))
        val vm = newViewModel(repos)
        advanceUntilIdle()

        assertEquals("別タグ", vm.uiState.value.noteTitles[NoteId("n9")])
    }
}
