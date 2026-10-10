@file:OptIn(ExperimentalCoroutinesApi::class)

package com.amgm.notebook.ui.notelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amgm.notebook.domain.model.Note
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.Tag
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.repository.NoteRepository
import com.amgm.notebook.domain.repository.TagRepository
import com.amgm.notebook.domain.rules.TagHierarchy
import com.amgm.notebook.ui.common.UiEvent
import com.amgm.notebook.ui.common.UiMessage
import com.amgm.notebook.ui.navigation.Destination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class NoteListItem(
    val id: NoteId,
    val title: String,
    val snippet: String,
    val tagNames: List<String>,
    val updatedAt: Instant
)

data class NoteListUiState(
    val query: String = "",
    val items: List<NoteListItem> = emptyList(),
    val isLoading: Boolean = true,
    /** タグ絞り込みの候補（名前順）。 */
    val availableTags: List<Tag> = emptyList(),
    /** 絞り込み中のタグ（そのタグまたは子孫タグが付いたノートのみ表示）。 */
    val filterTagId: TagId? = null
)

class NoteListViewModel(
    private val noteRepository: NoteRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filterTagId = MutableStateFlow<TagId?>(null)

    private val _uiState = MutableStateFlow(NoteListUiState())
    val uiState: StateFlow<NoteListUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    init {
        val notesFlow = query.flatMapLatest { q -> noteRepository.observeNotes(q) }
        viewModelScope.launch {
            combine(notesFlow, tagRepository.observeTags(), filterTagId) { notes, tags, filter ->
                buildItems(notes, tags, filter)
            }.collect { (items, tags) ->
                _uiState.update { it.copy(items = items, availableTags = tags, isLoading = false) }
            }
        }
    }

    private fun buildItems(
        notes: List<Note>,
        tags: List<Tag>,
        filter: TagId?
    ): Pair<List<NoteListItem>, List<Tag>> {
        val byId = tags.associateBy { it.id }
        val hierarchy = TagHierarchy(byId)
        val filtered = if (filter == null) notes else notes.filter { hierarchy.isInstanceOf(it, filter) }
        val items = filtered.map { note ->
            NoteListItem(
                id = note.id,
                title = note.title,
                snippet = note.body.take(SNIPPET_LENGTH),
                tagNames = note.tagIds.mapNotNull { byId[it]?.name }.sortedBy { it.lowercase() },
                updatedAt = note.updatedAt
            )
        }
        return items to tags
    }

    fun onQueryChange(query: String) {
        this.query.value = query
        _uiState.update { it.copy(query = query) }
    }

    /** null で絞り込み解除。同じタグを再度指定すると解除。 */
    fun onTagFilterChange(tagId: TagId?) {
        val next = if (tagId != null && tagId == filterTagId.value) null else tagId
        filterTagId.value = next
        _uiState.update { it.copy(filterTagId = next) }
    }

    fun onNoteClick(id: NoteId) = send(UiEvent.Navigate(Destination.NoteEdit(id)))

    fun onCreateNoteClick() = send(UiEvent.Navigate(Destination.NoteEdit(null)))

    fun onTagsClick() = send(UiEvent.Navigate(Destination.TagList))

    fun onDeleteNote(id: NoteId) {
        viewModelScope.launch {
            noteRepository.delete(id)
            send(UiEvent.ShowMessage(UiMessage.Deleted))
        }
    }

    private fun send(event: UiEvent) {
        _events.trySend(event)
    }

    companion object {
        const val SNIPPET_LENGTH = 80
    }
}
