package com.amgm.notebook.ui.tagedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amgm.notebook.domain.error.CyclicInheritanceException
import com.amgm.notebook.domain.error.DuplicateTagNameException
import com.amgm.notebook.domain.error.TagNotFoundException
import com.amgm.notebook.domain.model.PropertyDefinition
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.Tag
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.repository.TagRepository
import com.amgm.notebook.domain.rules.EffectivePropertyResolver
import com.amgm.notebook.domain.rules.EffectiveSchema
import com.amgm.notebook.domain.rules.TagHierarchy
import com.amgm.notebook.domain.util.IdGenerator
import com.amgm.notebook.ui.common.UiEvent
import com.amgm.notebook.ui.common.UiMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PropertyDefinitionDraft(
    val name: String,
    val type: PropertyType = PropertyType.Text,
    val required: Boolean = false
)

sealed interface TagEditError {
    data object BlankName : TagEditError
    data object DuplicateName : TagEditError
    data class DuplicateDefinitionName(val name: String) : TagEditError
    data object BlankDefinitionName : TagEditError
    data object Cyclic : TagEditError
}

data class TagEditUiState(
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val name: String = "",
    /** 親にできるタグ。自分自身と自分の子孫を除外（名前順）。 */
    val parentCandidates: List<Tag> = emptyList(),
    val selectedParentIds: Set<TagId> = emptySet(),
    val definitions: List<PropertyDefinitionDraft> = emptyList(),
    /** 選択中の親から得る継承定義と衝突（読み取り専用表示）。 */
    val inheritedSchema: EffectiveSchema = EffectiveSchema.EMPTY,
    /** 表示用: 全タグのID→名前。 */
    val tagNames: Map<TagId, String> = emptyMap(),
    val error: TagEditError? = null,
    val isSaving: Boolean = false
)

class TagEditViewModel(
    private val tagId: TagId?,
    private val tagRepository: TagRepository,
    private val idGenerator: IdGenerator
) : ViewModel() {

    private var allTags: Map<TagId, Tag> = emptyMap()
    private var currentId: TagId? = tagId

    private val _uiState = MutableStateFlow(
        TagEditUiState(isNew = tagId == null, isLoading = tagId != null)
    )
    val uiState: StateFlow<TagEditUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { initialize() }
    }

    private suspend fun initialize() {
        allTags = tagRepository.getAllTags().associateBy { it.id }
        if (tagId != null) {
            val tag = tagRepository.getTag(tagId)
            if (tag == null) {
                _uiState.update { it.copy(isLoading = false) }
                send(UiEvent.ShowMessage(UiMessage.TagNotFound))
                send(UiEvent.NavigateBack)
                return
            }
            _uiState.update {
                recompute(
                    it.copy(
                        isLoading = false,
                        name = tag.name,
                        selectedParentIds = tag.parentIds,
                        definitions = tag.ownPropertyDefinitions.map { d ->
                            PropertyDefinitionDraft(d.name.value, d.type, d.required)
                        }
                    )
                )
            }
        } else {
            _uiState.update { recompute(it.copy(isLoading = false)) }
        }
        tagRepository.observeTags().collect { tags ->
            allTags = tags.associateBy { it.id }
            _uiState.update { recompute(it) }
        }
    }

    /** 親候補・継承スキーマ・タグ名表を最新のタグ集合から再計算する。 */
    private fun recompute(state: TagEditUiState): TagEditUiState {
        val hierarchy = TagHierarchy(allTags)
        val excluded: Set<TagId> =
            if (tagId == null) emptySet() else hierarchy.descendantsOf(tagId) + tagId
        val candidates = allTags.values
            .filter { it.id !in excluded }
            .sortedWith(compareBy<Tag>({ it.name.lowercase() }, { it.id.value }))
        val knownParents = state.selectedParentIds.filter { it in allTags }.toSet()
        val inherited = EffectivePropertyResolver(hierarchy).resolveForTags(knownParents)
        return state.copy(
            parentCandidates = candidates,
            selectedParentIds = knownParents,
            inheritedSchema = inherited,
            tagNames = allTags.mapValues { it.value.name }
        )
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, error = null) }
    }

    fun onParentToggle(id: TagId) {
        _uiState.update { s ->
            val parents = if (id in s.selectedParentIds) s.selectedParentIds - id else s.selectedParentIds + id
            recompute(s.copy(selectedParentIds = parents, error = null))
        }
    }

    fun onAddDefinition() {
        _uiState.update { it.copy(definitions = it.definitions + PropertyDefinitionDraft(""), error = null) }
    }

    fun onDefinitionChange(index: Int, draft: PropertyDefinitionDraft) {
        _uiState.update { s ->
            if (index !in s.definitions.indices) return@update s
            s.copy(
                definitions = s.definitions.mapIndexed { i, d -> if (i == index) draft else d },
                error = null
            )
        }
    }

    fun onRemoveDefinition(index: Int) {
        _uiState.update { s ->
            if (index !in s.definitions.indices) return@update s
            s.copy(definitions = s.definitions.filterIndexed { i, _ -> i != index }, error = null)
        }
    }

    fun onMoveDefinition(from: Int, to: Int) {
        _uiState.update { s ->
            if (from !in s.definitions.indices || to !in s.definitions.indices || from == to) return@update s
            val list = s.definitions.toMutableList()
            val item = list.removeAt(from)
            list.add(to, item)
            s.copy(definitions = list)
        }
    }

    fun onSaveClick() {
        val s = _uiState.value
        if (s.isSaving || s.isLoading) return
        val name = s.name.trim()
        if (name.isEmpty()) {
            setError(TagEditError.BlankName)
            return
        }
        val trimmedNames = s.definitions.map { it.name.trim() }
        if (trimmedNames.any { it.isEmpty() }) {
            setError(TagEditError.BlankDefinitionName)
            return
        }
        val duplicated = trimmedNames.groupBy { it }.entries.firstOrNull { it.value.size > 1 }?.key
        if (duplicated != null) {
            setError(TagEditError.DuplicateDefinitionName(duplicated))
            return
        }
        val id = currentId ?: TagId(idGenerator.newId()).also { currentId = it }
        val tag = Tag(
            id = id,
            name = name,
            parentIds = s.selectedParentIds,
            ownPropertyDefinitions = s.definitions.mapIndexed { i, d ->
                PropertyDefinition(PropertyName(trimmedNames[i]), d.type, d.required)
            }
        )
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                tagRepository.save(tag)
                _uiState.update { it.copy(isSaving = false) }
                send(UiEvent.ShowMessage(UiMessage.Saved))
                send(UiEvent.NavigateBack)
            } catch (e: CyclicInheritanceException) {
                _uiState.update { it.copy(isSaving = false, error = TagEditError.Cyclic) }
                send(UiEvent.ShowMessage(UiMessage.CyclicInheritance))
            } catch (e: DuplicateTagNameException) {
                _uiState.update { it.copy(isSaving = false, error = TagEditError.DuplicateName) }
                send(UiEvent.ShowMessage(UiMessage.DuplicateTagName))
            } catch (e: TagNotFoundException) {
                _uiState.update { it.copy(isSaving = false) }
                send(UiEvent.ShowMessage(UiMessage.TagNotFound))
            }
        }
    }

    fun onBackClick() = send(UiEvent.NavigateBack)

    private fun setError(error: TagEditError) {
        _uiState.update { it.copy(error = error) }
    }

    private fun send(event: UiEvent) {
        _events.trySend(event)
    }
}
