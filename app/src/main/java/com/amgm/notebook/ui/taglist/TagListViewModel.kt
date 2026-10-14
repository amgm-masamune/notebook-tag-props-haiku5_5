package com.amgm.notebook.ui.taglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amgm.notebook.domain.model.Tag
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.repository.TagRepository
import com.amgm.notebook.domain.service.flattenTagTree
import com.amgm.notebook.ui.common.UiEvent
import com.amgm.notebook.ui.common.UiMessage
import com.amgm.notebook.ui.navigation.Destination
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TagListItem(
    val id: TagId,
    val name: String,
    val parentNames: List<String>,
    val ownDefinitionCount: Int,
    /** 親子関係の深さ（根=0）。インデントに使う。 */
    val depth: Int = 0,
    /** 複数親のタグの2回目以降の出現行なら true。 */
    val isAdditionalParentOccurrence: Boolean = false
)

data class TagListUiState(
    val items: List<TagListItem> = emptyList(),
    val isLoading: Boolean = true,
    val pendingDelete: Tag? = null
)

class TagListViewModel(
    private val tagRepository: TagRepository
) : ViewModel() {

    private var tagsById: Map<TagId, Tag> = emptyMap()

    private val _uiState = MutableStateFlow(TagListUiState())
    val uiState: StateFlow<TagListUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            tagRepository.observeTags().collect { tags ->
                tagsById = tags.associateBy { it.id }
                // 親子関係に沿って深さ優先に平坦化する（複数親のタグは親ごとに出現）
                val items = flattenTagTree(tags).map { node ->
                    val tag = node.tag
                    TagListItem(
                        id = tag.id,
                        name = tag.name,
                        parentNames = tag.parentIds
                            .mapNotNull { tagsById[it]?.name }
                            .sortedBy { it.lowercase() },
                        ownDefinitionCount = tag.ownPropertyDefinitions.size,
                        depth = node.depth,
                        isAdditionalParentOccurrence = node.isAdditionalParentOccurrence
                    )
                }
                _uiState.update { it.copy(items = items, isLoading = false) }
            }
        }
    }

    fun onTagClick(id: TagId) = send(UiEvent.Navigate(Destination.TagTable(id)))

    fun onEditTagClick(id: TagId) = send(UiEvent.Navigate(Destination.TagEdit(id)))

    fun onCreateTagClick() = send(UiEvent.Navigate(Destination.TagEdit(null)))

    fun onDeleteTagClick(id: TagId) {
        val tag = tagsById[id] ?: return
        _uiState.update { it.copy(pendingDelete = tag) }
    }

    fun onDeleteConfirm() {
        val target = _uiState.value.pendingDelete ?: return
        _uiState.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            tagRepository.delete(target.id)
            send(UiEvent.ShowMessage(UiMessage.Deleted))
        }
    }

    fun onDeleteDismiss() {
        _uiState.update { it.copy(pendingDelete = null) }
    }

    fun onBackClick() = send(UiEvent.NavigateBack)

    private fun send(event: UiEvent) {
        _events.trySend(event)
    }
}
