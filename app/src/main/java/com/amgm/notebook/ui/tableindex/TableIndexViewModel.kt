package com.amgm.notebook.ui.tableindex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.repository.NoteRepository
import com.amgm.notebook.domain.repository.TagRepository
import com.amgm.notebook.domain.rules.TagHierarchy
import com.amgm.notebook.ui.common.UiEvent
import com.amgm.notebook.ui.navigation.Destination
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** 表タブの 1 行。[noteCount] は子孫タグ付きを含むノート件数（そのタグの表ビューに並ぶ件数と一致）。 */
data class TableIndexRow(
    val tagId: TagId,
    val name: String,
    val noteCount: Int
)

data class TableIndexUiState(
    val rows: List<TableIndexRow> = emptyList(),
    val isLoading: Boolean = true
)

/** 「表」タブ。全タグをフラットに並べ、タップでそのタグの表ビューへ遷移する。 */
class TableIndexViewModel(
    private val tagRepository: TagRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TableIndexUiState())
    val uiState: StateFlow<TableIndexUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            tagRepository.observeTags()
                .combine(noteRepository.observeNotes()) { tags, notes -> tags to notes }
                .collect { (tags, notes) ->
                    val byId = tags.associateBy { it.id }
                    val hierarchy = TagHierarchy(byId)
                    val rows = tags
                        .sortedWith(compareBy({ it.name.lowercase() }, { it.id.value }))
                        .map { tag ->
                            val members = hierarchy.descendantsOf(tag.id) + tag.id
                            TableIndexRow(
                                tagId = tag.id,
                                name = tag.name,
                                noteCount = notes.count { note -> note.tagIds.any { it in members } }
                            )
                        }
                    _uiState.value = TableIndexUiState(rows = rows, isLoading = false)
                }
        }
    }

    fun onTagClick(id: TagId) {
        _events.trySend(UiEvent.Navigate(Destination.TagTable(id)))
    }
}
