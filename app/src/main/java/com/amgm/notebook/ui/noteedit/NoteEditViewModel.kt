package com.amgm.notebook.ui.noteedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amgm.notebook.domain.error.TagNotFoundException
import com.amgm.notebook.domain.model.Note
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.model.Tag
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.repository.NoteRepository
import com.amgm.notebook.domain.repository.TagRepository
import com.amgm.notebook.domain.rules.EffectivePropertyResolver
import com.amgm.notebook.domain.rules.TagHierarchy
import com.amgm.notebook.domain.rules.TitleHeadingSync
import com.amgm.notebook.domain.rules.Severity
import com.amgm.notebook.domain.rules.ValidationIssue
import com.amgm.notebook.domain.rules.NoteValidator
import com.amgm.notebook.domain.util.IdGenerator
import com.amgm.notebook.markdown.MarkdownDocument
import com.amgm.notebook.markdown.MarkdownParser
import com.amgm.notebook.ui.common.PropertyInputs
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
import java.time.Clock
import java.time.Instant

enum class EditorMode { Edit, Preview }

sealed interface PropertyOrigin {
    data class Defined(val definedBy: TagId) : PropertyOrigin
    data object Free : PropertyOrigin
}

enum class PropertyInputError { Unparsable, TypeMismatch, RequiredMissing }

/**
 * プロパティ編集行。[input] は入力欄の生文字列、[value] は解釈結果。
 * 既存ノートから読み込んだ値は型が合わなくても [value] にそのまま保持し、[error] = TypeMismatch で示す。
 */
data class PropertyRowUi(
    val name: PropertyName,
    val type: PropertyType,
    val origin: PropertyOrigin,
    val required: Boolean,
    val input: String,
    val value: PropertyValue,
    val error: PropertyInputError?
)

/** ノートピッカーの候補 1 件。 */
data class NotePickerCandidate(val id: NoteId, val title: String)

/**
 * ノートピッカー（NoteLink プロパティの値選択）の状態。
 * [filterTagId] が null なら「すべて」。初期値は定義の `targetTagId`（強制ではなく変更可能）。
 * [selectedIds] は確定前の作業中の選択（順序保持）。
 */
data class NotePickerUi(
    val propertyName: PropertyName,
    val query: String = "",
    val filterTagId: TagId? = null,
    val selectedIds: List<NoteId> = emptyList(),
    val candidates: List<NotePickerCandidate> = emptyList()
)

data class NoteEditUiState(
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val title: String = "",
    val body: String = "",
    val mode: EditorMode = EditorMode.Edit,
    val previewDocument: MarkdownDocument = MarkdownDocument(emptyList()),
    val availableTags: List<Tag> = emptyList(),
    val selectedTagIds: Set<TagId> = emptySet(),
    val propertyRows: List<PropertyRowUi> = emptyList(),
    val issues: List<ValidationIssue> = emptyList(),
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val notFound: Boolean = false,
    /** タイトル変更時に本文の最初の h1 も更新するか（ノートごと・規定ON）。 */
    val autoSyncTitleH1: Boolean = true,
    /** 存在する全ノートの ID→タイトル。NoteRefs のチップ表示用（無ければ「（削除済み）」）。 */
    val noteTitles: Map<NoteId, String> = emptyMap(),
    /** 開いているノートピッカー。null なら閉じている。 */
    val picker: NotePickerUi? = null
)

class NoteEditViewModel(
    private val noteId: NoteId?,
    private val noteRepository: NoteRepository,
    private val tagRepository: TagRepository,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    /** 新規作成時に初期選択するタグ（存在しなければ無視）。既存ノート編集では使わない。 */
    private val initialTagId: TagId? = null
) : ViewModel() {

    private val validator = NoteValidator()
    private var allTags: Map<TagId, Tag> = emptyMap()
    private var allNotes: List<Note> = emptyList()
    private var createdAt: Instant? = null
    private var currentId: NoteId? = noteId

    private val _uiState = MutableStateFlow(
        NoteEditUiState(isNew = noteId == null, isLoading = noteId != null)
    )
    val uiState: StateFlow<NoteEditUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch { initialize() }
        viewModelScope.launch {
            noteRepository.observeNotes().collect { notes -> onNotesChanged(notes) }
        }
    }

    private suspend fun initialize() {
        allTags = tagRepository.getAllTags().associateBy { it.id }
        if (noteId != null) {
            val note = noteRepository.getNote(noteId)
            if (note == null) {
                _uiState.update { it.copy(isLoading = false, notFound = true) }
                send(UiEvent.NavigateBack)
                return
            }
            createdAt = note.createdAt
            val selected = note.tagIds.filter { it in allTags }.toSet()
            val seeds = note.properties.entries
                .sortedBy { it.key.value }
                .map { (name, value) ->
                    PropertyRowUi(
                        name = name,
                        type = PropertyInputs.inferType(value),
                        origin = PropertyOrigin.Free,
                        required = false,
                        input = PropertyInputs.toInput(value),
                        value = value,
                        error = null
                    )
                }
            _uiState.value = validated(
                NoteEditUiState(
                    isNew = false,
                    isLoading = false,
                    title = note.title,
                    body = note.body,
                    autoSyncTitleH1 = note.autoSyncTitleH1,
                    noteTitles = _uiState.value.noteTitles,
                    availableTags = sortedTags(),
                    selectedTagIds = selected,
                    propertyRows = rebuildRows(selected, seeds)
                )
            )
        } else {
            val initial = initialTagId?.takeIf { it in allTags }
            val selected: Set<TagId> = if (initial != null) setOf(initial) else emptySet()
            _uiState.update {
                validated(
                    it.copy(
                        isLoading = false,
                        availableTags = sortedTags(),
                        selectedTagIds = selected,
                        propertyRows = rebuildRows(selected, it.propertyRows)
                    )
                )
            }
        }
        tagRepository.observeTags().collect { tags -> onTagsChanged(tags) }
    }

    private fun sortedTags(): List<Tag> =
        allTags.values.sortedWith(compareBy<Tag>({ it.name.lowercase() }, { it.id.value }))

    private fun onTagsChanged(tags: List<Tag>) {
        allTags = tags.associateBy { it.id }
        _uiState.update { s ->
            val selected = s.selectedTagIds.filter { it in allTags }.toSet()
            validated(
                s.copy(
                    availableTags = sortedTags(),
                    selectedTagIds = selected,
                    propertyRows = rebuildRows(selected, s.propertyRows)
                )
            )
        }
    }

    private fun onNotesChanged(notes: List<Note>) {
        allNotes = notes
        _uiState.update { s ->
            s.copy(
                noteTitles = notes.associate { it.id to it.title },
                picker = s.picker?.let { buildPicker(it) }
            )
        }
    }

    // ---- 操作 -------------------------------------------------------------

    fun onTitleChange(title: String) {
        _uiState.update { s ->
            var next = s.copy(title = title, isDirty = true)
            if (s.autoSyncTitleH1) {
                val newBody = if (s.body.isEmpty()) {
                    TitleHeadingSync.initialBody(title)
                } else {
                    TitleHeadingSync.syncFirstH1(s.body, title)
                }
                if (newBody != s.body) {
                    val preview = if (s.mode == EditorMode.Preview) MarkdownParser.parse(newBody) else s.previewDocument
                    next = next.copy(body = newBody, previewDocument = preview)
                }
            }
            validated(next)
        }
    }

    /** 「タイトル変更時に最初の h1 も更新する」の切替。切替自体では本文を変更しない。 */
    fun onAutoSyncTitleH1Change(enabled: Boolean) {
        _uiState.update { it.copy(autoSyncTitleH1 = enabled, isDirty = true) }
    }

    /** タグ作成画面（新規）へ遷移する。この ViewModel は戻りスタックに残るため入力中の値は保持される。 */
    fun onCreateTagClick() = send(UiEvent.Navigate(Destination.TagEdit(null)))

    fun onBodyChange(body: String) {
        _uiState.update { s ->
            val preview = if (s.mode == EditorMode.Preview) MarkdownParser.parse(body) else s.previewDocument
            s.copy(body = body, previewDocument = preview, isDirty = true)
        }
    }

    fun onModeChange(mode: EditorMode) {
        _uiState.update { s ->
            val preview = if (mode == EditorMode.Preview) MarkdownParser.parse(s.body) else s.previewDocument
            s.copy(mode = mode, previewDocument = preview)
        }
    }

    fun onTagToggle(tagId: TagId) {
        _uiState.update { s ->
            if (tagId !in s.selectedTagIds && tagId !in allTags) return@update s
            val selected = if (tagId in s.selectedTagIds) s.selectedTagIds - tagId else s.selectedTagIds + tagId
            validated(
                s.copy(
                    selectedTagIds = selected,
                    propertyRows = rebuildRows(selected, s.propertyRows),
                    isDirty = true
                )
            )
        }
    }

    fun onPropertyInputChange(name: PropertyName, input: String) {
        _uiState.update { s ->
            if (s.propertyRows.none { it.name == name }) return@update s
            val rows = s.propertyRows.map { row ->
                if (row.name != name) {
                    row
                } else {
                    row.copy(input = input, value = PropertyInputs.parse(row.type, input) ?: PropertyValue.Empty)
                }
            }
            validated(s.copy(propertyRows = rows, isDirty = true))
        }
    }

    fun onAddFreeProperty(name: String, type: PropertyType) {
        val trimmed = name.trim()
        val current = _uiState.value
        if (trimmed.isEmpty() || current.propertyRows.any { it.name.value == trimmed }) {
            send(UiEvent.ShowMessage(UiMessage.NameRequired))
            return
        }
        val newRow = PropertyRowUi(
            name = PropertyName(trimmed),
            type = type,
            origin = PropertyOrigin.Free,
            required = false,
            input = "",
            value = PropertyValue.Empty,
            error = null
        )
        _uiState.update { s ->
            val defined = s.propertyRows.filter { it.origin is PropertyOrigin.Defined }
            val free = (s.propertyRows.filter { it.origin is PropertyOrigin.Free } + newRow)
                .sortedBy { it.name.value }
            validated(s.copy(propertyRows = defined + free, isDirty = true))
        }
    }

    fun onRemoveFreeProperty(name: PropertyName) {
        _uiState.update { s ->
            val target = s.propertyRows.firstOrNull { it.name == name }
            if (target == null || target.origin !is PropertyOrigin.Free) return@update s
            validated(s.copy(propertyRows = s.propertyRows - target, isDirty = true))
        }
    }

    // ---- NoteLink (ノート紐付け) ---------------------------------------------

    /** 指定した NoteLink 行のピッカーを開く。絞り込みの初期値は定義の targetTagId（存在するタグのみ）。 */
    fun onOpenNotePicker(name: PropertyName) {
        _uiState.update { s ->
            val row = s.propertyRows.firstOrNull { it.name == name } ?: return@update s
            val type = row.type as? PropertyType.NoteLink ?: return@update s
            val selected = (row.value as? PropertyValue.NoteRefs)?.ids.orEmpty()
            val picker = NotePickerUi(
                propertyName = name,
                filterTagId = type.targetTagId?.takeIf { it in allTags },
                selectedIds = selected
            )
            s.copy(picker = buildPicker(picker))
        }
    }

    fun onPickerQueryChange(query: String) = updatePicker { it.copy(query = query) }

    /** タグで絞り込む。null で「すべて」。 */
    fun onPickerFilterChange(tagId: TagId?) = updatePicker { it.copy(filterTagId = tagId) }

    fun onPickerToggle(id: NoteId) = updatePicker { p ->
        p.copy(selectedIds = if (id in p.selectedIds) p.selectedIds - id else p.selectedIds + id)
    }

    fun onPickerConfirm() {
        val picker = _uiState.value.picker ?: return
        _uiState.update { it.copy(picker = null) }
        setNoteRefs(picker.propertyName, picker.selectedIds)
    }

    fun onPickerDismiss() {
        _uiState.update { it.copy(picker = null) }
    }

    /** NoteLink 行から参照を 1 件外す。 */
    fun onRemoveNoteRef(name: PropertyName, id: NoteId) {
        val row = _uiState.value.propertyRows.firstOrNull { it.name == name } ?: return
        val current = (row.value as? PropertyValue.NoteRefs)?.ids ?: return
        setNoteRefs(name, current - id)
    }

    private fun setNoteRefs(name: PropertyName, ids: List<NoteId>) {
        _uiState.update { s ->
            val target = s.propertyRows.firstOrNull { it.name == name }
            if (target == null || target.type !is PropertyType.NoteLink) return@update s
            val value: PropertyValue =
                if (ids.isEmpty()) PropertyValue.Empty else PropertyValue.NoteRefs.of(ids)
            val rows = s.propertyRows.map { row ->
                if (row.name == name) row.copy(value = value, input = PropertyInputs.toInput(value)) else row
            }
            validated(s.copy(propertyRows = rows, isDirty = true))
        }
    }

    private fun updatePicker(transform: (NotePickerUi) -> NotePickerUi) {
        _uiState.update { s ->
            val picker = s.picker ?: return@update s
            s.copy(picker = buildPicker(transform(picker)))
        }
    }

    /** 検索語・タグ絞り込み（子孫タグ含む）・自分自身の除外で候補を再計算する。 */
    private fun buildPicker(picker: NotePickerUi): NotePickerUi {
        val tagFilter: Set<TagId>? = picker.filterTagId?.let { TagHierarchy(allTags).descendantsOf(it) + it }
        val q = picker.query.trim()
        val candidates = allNotes
            .filter { it.id != currentId }
            .filter { note -> tagFilter == null || note.tagIds.any { it in tagFilter } }
            .filter { note -> q.isEmpty() || note.title.contains(q, ignoreCase = true) }
            .sortedWith(compareBy<Note>({ it.title.lowercase() }, { it.id.value }))
            .map { NotePickerCandidate(it.id, it.title) }
        return picker.copy(candidates = candidates)
    }

    fun onTagTableClick(tagId: TagId) = send(UiEvent.Navigate(Destination.TagTable(tagId)))

    fun onSaveClick() {
        val before = _uiState.value
        if (before.isSaving || before.isLoading || before.notFound) return
        val state = validated(before)
        _uiState.value = state
        val blocked = state.issues.any { it.severity == Severity.ERROR } ||
            state.propertyRows.any { it.error == PropertyInputError.Unparsable }
        if (blocked) {
            send(UiEvent.ShowMessage(UiMessage.SaveBlockedByErrors))
            return
        }
        val id = currentId ?: NoteId(idGenerator.newId()).also { currentId = it }
        val now = clock.instant()
        val note = Note(
            id = id,
            title = state.title,
            body = state.body,
            tagIds = state.selectedTagIds,
            properties = state.propertyRows.associate { it.name to it.value },
            createdAt = createdAt ?: now,
            updatedAt = now,
            autoSyncTitleH1 = state.autoSyncTitleH1
        )
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                noteRepository.save(note)
                createdAt = note.createdAt
                _uiState.update { it.copy(isSaving = false, isDirty = false) }
                send(UiEvent.ShowMessage(UiMessage.Saved))
                send(UiEvent.NavigateBack)
            } catch (e: TagNotFoundException) {
                _uiState.update { it.copy(isSaving = false) }
                send(UiEvent.ShowMessage(UiMessage.TagNotFound))
            }
        }
    }

    fun onBackClick() {
        if (_uiState.value.isDirty) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            send(UiEvent.NavigateBack)
        }
    }

    fun onDiscardConfirm() {
        _uiState.update { it.copy(showDiscardDialog = false) }
        send(UiEvent.NavigateBack)
    }

    fun onDiscardDismiss() {
        _uiState.update { it.copy(showDiscardDialog = false) }
    }

    fun onDeleteClick() {
        val id = noteId ?: return
        viewModelScope.launch {
            noteRepository.delete(id)
            send(UiEvent.ShowMessage(UiMessage.Deleted))
            send(UiEvent.NavigateBack)
        }
    }

    // ---- 内部ロジック ---------------------------------------------------------

    private fun resolver(): EffectivePropertyResolver = EffectivePropertyResolver(allTags)

    /** 既存値を引き継いで実効定義に沿った行を再構成する。定義済み→自由の順。 */
    private fun rebuildRows(selected: Set<TagId>, previous: List<PropertyRowUi>): List<PropertyRowUi> {
        val schema = resolver().resolveForTags(selected)
        val previousByName = previous.associateBy { it.name }
        val defined = schema.definitions.map { effective ->
            val definition = effective.definition
            val prev = previousByName[definition.name]
            PropertyRowUi(
                name = definition.name,
                type = definition.type,
                origin = PropertyOrigin.Defined(effective.definedBy),
                required = definition.required,
                input = prev?.input ?: "",
                value = if (prev == null) PropertyValue.Empty else carryValue(definition.type, prev),
                error = null
            )
        }
        val definedNames = schema.definitions.map { it.definition.name }.toSet()
        val free = previous
            .filter { it.name !in definedNames }
            .map { row ->
                row.copy(
                    origin = PropertyOrigin.Free,
                    required = false,
                    value = carryValue(row.type, row),
                    error = null
                )
            }
            .sortedBy { it.name.value }
        return defined + free
    }

    /** 値が既にあればそのまま、空なら入力文字列を新しい型で解釈し直す。 */
    private fun carryValue(type: PropertyType, row: PropertyRowUi): PropertyValue =
        if (row.value !is PropertyValue.Empty) row.value
        else PropertyInputs.parse(type, row.input) ?: PropertyValue.Empty

    /** issues と各行の error を最新の状態から再計算する。 */
    private fun validated(state: NoteEditUiState): NoteEditUiState {
        val schema = resolver().resolveForTags(state.selectedTagIds)
        val draft = Note(
            id = currentId ?: NoteId(DRAFT_ID),
            title = state.title,
            body = state.body,
            tagIds = state.selectedTagIds,
            properties = state.propertyRows.associate { it.name to it.value },
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH
        )
        val issues = validator.validate(draft, schema).issues
        val rows = state.propertyRows.map { row ->
            val unparsable = row.input.isNotBlank() && row.value is PropertyValue.Empty
            val error = when {
                unparsable -> PropertyInputError.Unparsable
                row.origin !is PropertyOrigin.Defined -> null
                issues.any { it is ValidationIssue.TypeMismatch && it.name == row.name } ->
                    PropertyInputError.TypeMismatch
                issues.any { it is ValidationIssue.RequiredMissing && it.name == row.name } ->
                    PropertyInputError.RequiredMissing
                else -> null
            }
            row.copy(error = error)
        }
        return state.copy(issues = issues, propertyRows = rows)
    }

    private fun send(event: UiEvent) {
        _events.trySend(event)
    }

    private companion object {
        const val DRAFT_ID = "draft"
    }
}
