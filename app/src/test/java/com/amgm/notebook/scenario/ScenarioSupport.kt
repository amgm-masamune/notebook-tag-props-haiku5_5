package com.amgm.notebook.scenario

import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.testing.InMemoryRepositories
import com.amgm.notebook.testutil.FakeNavigator
import com.amgm.notebook.testutil.SequentialIdGenerator
import com.amgm.notebook.testutil.TestClock
import com.amgm.notebook.ui.common.UiEvent
import com.amgm.notebook.ui.noteedit.NoteEditViewModel
import com.amgm.notebook.ui.notelist.NoteListViewModel
import com.amgm.notebook.ui.tagedit.TagEditViewModel
import com.amgm.notebook.ui.taglist.TagListViewModel
import com.amgm.notebook.ui.tagtable.TagTableViewModel

/**
 * 複数 ViewModel を共有フェイクリポジトリでつなぐ擬似アプリ。
 * 画面ごとに ViewModel を作る点は NavHost と同じで、遷移は [navigator] で追跡する。
 */
class ScenarioApp {
    val repos = InMemoryRepositories()
    val clock = TestClock()
    val ids = SequentialIdGenerator()
    val navigator = FakeNavigator()

    fun noteList() = NoteListViewModel(repos.notes, repos.tags)

    fun noteEdit(noteId: NoteId?) = NoteEditViewModel(noteId, repos.notes, repos.tags, ids, clock)

    fun tagList() = TagListViewModel(repos.tags)

    fun tagEdit(tagId: TagId?) = TagEditViewModel(tagId, repos.tags, ids)

    fun tagTable(tagId: TagId) = TagTableViewModel(tagId, repos.tags, repos.notes)

    /** 溜まったイベントをナビゲータへ反映する。 */
    fun route(events: MutableList<UiEvent>) = navigator.drain(events)
}
