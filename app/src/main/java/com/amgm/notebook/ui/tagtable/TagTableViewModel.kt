@file:OptIn(ExperimentalCoroutinesApi::class)

package com.amgm.notebook.ui.tagtable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amgm.notebook.domain.model.Note
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.Tag
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.repository.NoteRepository
import com.amgm.notebook.domain.repository.TagRepository
import com.amgm.notebook.domain.rules.TagHierarchy
import com.amgm.notebook.domain.service.TableSort
import com.amgm.notebook.domain.service.TableSortKey
import com.amgm.notebook.domain.service.TableView
import com.amgm.notebook.domain.service.TableViewBuilder
import com.amgm.notebook.ui.common.UiEvent
import com.amgm.notebook.ui.navigation.Destination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TagTableUiState(
    val isLoading: Boolean = true,
    val tagFound: Boolean = true,
    val tagName: String = "",
    val table: TableView? = null,
    val sort: TableSort? = null
)

class TagTableViewModel(
    private val tagId: TagId,
    private val tagRepository: TagRepository,
    private val noteRepository: NoteRepository,
    private val tableViewBuilder: TableViewBuilder = TableViewBuilder()
) : ViewModel() {

    private class Snapshot(val tags: Map<TagId, Tag>, val notes: List<Note>)

    private val sort = MutableStateFlow<TableSort?>(null)
    private var notFoundReported = false

    private val _uiState = MutableStateFlow(TagTableUiState())
    val uiState: StateFlow<TagTableUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    init {
        val snapshots: Flow<Snapshot> = tagRepository.observeTags().flatMapLatest { tags ->
            val byId = tags.associateBy { it.id }
            if (byId[tagId] == null) {
                kotlinx.coroutines.flow.flowOf(Snapshot(byId, emptyList()))
            } else {
                val members = TagHierarchy(byId).descendantsOf(tagId) + tagId
                noteRepository.observeNotesByTagIds(members).map { notes -> Snapshot(byId, notes) }
            }
        }
        viewModelScope.launch {
            snapshots.combine(sort) { snapshot, currentSort -> snapshot to currentSort }
                .collect { (snapshot, currentSort) -> render(snapshot, currentSort) }
        }
    }

    private fun render(snapshot: Snapshot, currentSort: TableSort?) {
        val tag = snapshot.tags[tagId]
        if (tag == null) {
            _uiState.update {
                it.copy(isLoading = false, tagFound = false, tagName = "", table = null, sort = currentSort)
            }
            if (!notFoundReported) {
                notFoundReported = true
                _events.trySend(UiEvent.NavigateBack)
            }
            return
        }
        val table = tableViewBuilder.build(tagId, snapshot.tags, snapshot.notes, currentSort)
        _uiState.update {
            it.copy(isLoading = false, tagFound = true, tagName = tag.name, table = table, sort = currentSort)
        }
    }

    fun onSortChange(key: TableSortKey) {
        val current = sort.value
        val next = when {
            current == null || current.column != key -> TableSort(key, true)
            current.ascending -> TableSort(key, false)
            else -> null
        }
        sort.value = next
        _uiState.update { it.copy(sort = next) }
    }

    fun onRowClick(noteId: NoteId) = send(UiEvent.Navigate(Destination.NoteEdit(noteId)))

    fun onEditTagClick() = send(UiEvent.Navigate(Destination.TagEdit(tagId)))

    fun onBackClick() = send(UiEvent.NavigateBack)

    private fun send(event: UiEvent) {
        _events.trySend(event)
    }
}
