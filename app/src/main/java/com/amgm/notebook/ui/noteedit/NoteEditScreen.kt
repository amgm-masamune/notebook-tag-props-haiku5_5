package com.amgm.notebook.ui.noteedit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.model.Tag
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.rules.ValidationIssue
import com.amgm.notebook.ui.TestTags
import com.amgm.notebook.ui.common.PropertyInputs
import com.amgm.notebook.ui.common.SelectableTagChips
import com.amgm.notebook.ui.markdown.MarkdownView

@Composable
fun NoteEditScreen(viewModel: NoteEditViewModel, snackbarHostState: SnackbarHostState) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler { viewModel.onBackClick() }
    NoteEditContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onTitleChange = viewModel::onTitleChange,
        onAutoSyncChange = viewModel::onAutoSyncTitleH1Change,
        onCreateTagClick = viewModel::onCreateTagClick,
        onOpenNotePicker = viewModel::onOpenNotePicker,
        onRemoveNoteRef = viewModel::onRemoveNoteRef,
        pickerActions = NotePickerActions(
            onQueryChange = viewModel::onPickerQueryChange,
            onFilterChange = viewModel::onPickerFilterChange,
            onToggle = viewModel::onPickerToggle,
            onConfirm = viewModel::onPickerConfirm,
            onDismiss = viewModel::onPickerDismiss
        ),
        onBodyChange = viewModel::onBodyChange,
        onModeChange = viewModel::onModeChange,
        onTagToggle = viewModel::onTagToggle,
        onPropertyInputChange = viewModel::onPropertyInputChange,
        onAddFreeProperty = viewModel::onAddFreeProperty,
        onRemoveFreeProperty = viewModel::onRemoveFreeProperty,
        onTagTableClick = viewModel::onTagTableClick,
        onSaveClick = viewModel::onSaveClick,
        onBackClick = viewModel::onBackClick,
        onDeleteClick = viewModel::onDeleteClick,
        onDiscardConfirm = viewModel::onDiscardConfirm,
        onDiscardDismiss = viewModel::onDiscardDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun NoteEditContent(
    state: NoteEditUiState,
    snackbarHostState: SnackbarHostState,
    onTitleChange: (String) -> Unit,
    onAutoSyncChange: (Boolean) -> Unit,
    onCreateTagClick: () -> Unit,
    onOpenNotePicker: (PropertyName) -> Unit,
    onRemoveNoteRef: (PropertyName, NoteId) -> Unit,
    pickerActions: NotePickerActions,
    onBodyChange: (String) -> Unit,
    onModeChange: (EditorMode) -> Unit,
    onTagToggle: (TagId) -> Unit,
    onPropertyInputChange: (PropertyName, String) -> Unit,
    onAddFreeProperty: (String, PropertyType) -> Unit,
    onRemoveFreeProperty: (PropertyName) -> Unit,
    onTagTableClick: (TagId) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDiscardConfirm: () -> Unit,
    onDiscardDismiss: () -> Unit
) {
    var showAddProperty by remember { mutableStateOf(false) }
    val tagNames = remember(state.availableTags) { state.availableTags.associate { it.id to it.name } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "新規ノート" else "ノート編集") },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag(TestTags.NOTE_EDIT_BACK)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.testTag(TestTags.NOTE_EDIT_DELETE)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "削除")
                        }
                    }
                    TextButton(
                        onClick = onSaveClick,
                        enabled = !state.isSaving && !state.isLoading,
                        modifier = Modifier.testTag(TestTags.NOTE_EDIT_SAVE)
                    ) { Text("保存") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = onTitleChange,
                    label = { Text("タイトル") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.NOTE_EDIT_TITLE)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "タイトル変更時に最初の見出し（h1）も更新する",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = state.autoSyncTitleH1,
                        onCheckedChange = onAutoSyncChange,
                        modifier = Modifier.testTag(TestTags.NOTE_EDIT_AUTO_SYNC_H1)
                    )
                }

                SectionTitle("タグ")
                if (state.availableTags.isEmpty()) {
                    Text(
                        "タグがありません。下のボタンから作成できます",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    SelectableTagChips(
                        tags = state.availableTags,
                        selectedIds = state.selectedTagIds,
                        tagPrefix = TestTags.NOTE_EDIT_TAG_CHIP_PREFIX,
                        onToggle = onTagToggle
                    )
                }
                OutlinedButton(
                    onClick = onCreateTagClick,
                    modifier = Modifier.testTag(TestTags.NOTE_EDIT_CREATE_TAG)
                ) { Text("タグを作成") }
                if (state.selectedTagIds.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        state.availableTags
                            .filter { it.id in state.selectedTagIds }
                            .forEach { tag ->
                                TextButton(
                                    onClick = { onTagTableClick(tag.id) },
                                    modifier = Modifier.testTag(TestTags.NOTE_EDIT_TABLE_BUTTON_PREFIX + tag.id.value)
                                ) { Text("「${tag.name}」の表で見る") }
                            }
                    }
                }

                SectionTitle("プロパティ")
                state.issues.filterIsInstance<ValidationIssue.Conflict>().forEach { issue ->
                    ConflictNotice(issue, tagNames)
                }
                if (state.propertyRows.isEmpty()) {
                    Text(
                        "プロパティはまだありません。タグを選ぶと定義済みの項目が現れます",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                state.propertyRows.forEach { row ->
                    PropertyRowEditor(
                        row = row,
                        definedByName = (row.origin as? PropertyOrigin.Defined)?.let { tagNames[it.definedBy] },
                        noteTitles = state.noteTitles,
                        onInputChange = { onPropertyInputChange(row.name, it) },
                        onRemove = { onRemoveFreeProperty(row.name) },
                        onOpenNotePicker = { onOpenNotePicker(row.name) },
                        onRemoveNoteRef = { onRemoveNoteRef(row.name, it) }
                    )
                }
                OutlinedButton(
                    onClick = { showAddProperty = true },
                    modifier = Modifier.testTag(TestTags.NOTE_EDIT_ADD_PROPERTY)
                ) { Text("プロパティを追加") }

                SectionTitle("本文")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = state.mode == EditorMode.Edit,
                        onClick = { onModeChange(EditorMode.Edit) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag(TestTags.NOTE_EDIT_MODE_EDIT)
                    ) { Text("編集") }
                    SegmentedButton(
                        selected = state.mode == EditorMode.Preview,
                        onClick = { onModeChange(EditorMode.Preview) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag(TestTags.NOTE_EDIT_MODE_PREVIEW)
                    ) { Text("プレビュー") }
                }
                if (state.mode == EditorMode.Edit) {
                    OutlinedTextField(
                        value = state.body,
                        onValueChange = onBodyChange,
                        label = { Text("Markdown で記述") },
                        minLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.NOTE_EDIT_BODY)
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(TestTags.NOTE_EDIT_PREVIEW)
                            .semantics(mergeDescendants = true) {}
                    ) {
                        if (state.previewDocument.blocks.isEmpty()) {
                            Text(
                                "(本文なし)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            MarkdownView(
                                document = state.previewDocument,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showAddProperty) {
        AddPropertyDialog(
            onConfirm = { name, type ->
                onAddFreeProperty(name, type)
                showAddProperty = false
            },
            onDismiss = { showAddProperty = false }
        )
    }

    val picker = state.picker
    if (picker != null) {
        NotePickerSheet(
            picker = picker,
            tags = state.availableTags,
            actions = pickerActions
        )
    }

    if (state.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = onDiscardDismiss,
            title = { Text("変更を破棄しますか？") },
            text = { Text("保存されていない変更があります。") },
            confirmButton = {
                TextButton(
                    onClick = onDiscardConfirm,
                    modifier = Modifier.testTag(TestTags.NOTE_EDIT_DISCARD_CONFIRM)
                ) { Text("破棄") }
            },
            dismissButton = {
                TextButton(
                    onClick = onDiscardDismiss,
                    modifier = Modifier.testTag(TestTags.NOTE_EDIT_DISCARD_DISMISS)
                ) { Text("編集を続ける") }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun ConflictNotice(issue: ValidationIssue.Conflict, tagNames: Map<TagId, String>) {
    val detail = issue.conflict.candidates.joinToString("、") {
        "${tagNames[it.tagId] ?: it.tagId.value}: ${PropertyInputs.label(it.type)}"
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "「${issue.conflict.name.value}」は継承元で型が衝突しているため、定義済みプロパティとして扱えません（$detail）",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun PropertyRowEditor(
    row: PropertyRowUi,
    definedByName: String?,
    noteTitles: Map<NoteId, String>,
    onInputChange: (String) -> Unit,
    onRemove: () -> Unit,
    onOpenNotePicker: () -> Unit,
    onRemoveNoteRef: (NoteId) -> Unit
) {
    val tag = TestTags.NOTE_EDIT_PROPERTY_PREFIX + row.name.value
    val labelText = row.name.value + if (row.required) " *" else ""
    val isError = row.error == PropertyInputError.Unparsable || row.error == PropertyInputError.TypeMismatch
    val typeLabel = PropertyInputs.label(row.type)
    val hint = when (row.error) {
        PropertyInputError.Unparsable -> "$typeLabel として解釈できません"
        PropertyInputError.TypeMismatch -> "保存済みの値が型（$typeLabel）と一致しません。入力し直してください"
        PropertyInputError.RequiredMissing -> "必須項目が未入力です（保存は可能です）"
        null -> if (definedByName != null) "$typeLabel ・ 定義: $definedByName" else "$typeLabel ・ 自由プロパティ"
    }
    val hintColor = when (row.error) {
        PropertyInputError.RequiredMissing -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    if (row.type is PropertyType.NoteLink) {
        NoteLinkRowEditor(
            row = row,
            labelText = labelText,
            hint = hint,
            hintColor = if (isError) MaterialTheme.colorScheme.error else hintColor,
            noteTitles = noteTitles,
            onOpenNotePicker = onOpenNotePicker,
            onRemoveNoteRef = onRemoveNoteRef,
            onRemoveProperty = onRemove
        )
    } else if (row.type == PropertyType.Boolean) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(labelText, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (row.input.isBlank()) "未設定 ・ $hint" else hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) MaterialTheme.colorScheme.error else hintColor
                    )
                }
                Switch(
                    checked = row.input.trim() == "true",
                    onCheckedChange = { onInputChange(if (it) "true" else "false") },
                    modifier = Modifier.testTag(tag)
                )
                if (row.input.isNotBlank()) {
                    TextButton(onClick = { onInputChange("") }) { Text("クリア") }
                }
                if (row.origin is PropertyOrigin.Free) {
                    RemoveButton(row, onRemove)
                }
            }
        }
    } else {
        val placeholderContent: (@Composable () -> Unit)? =
            if (row.type == PropertyType.Date) {
                { Text("yyyy-MM-dd") }
            } else {
                null
            }
        val trailingContent: (@Composable () -> Unit)? =
            if (row.origin is PropertyOrigin.Free) {
                { RemoveButton(row, onRemove) }
            } else {
                null
            }
        OutlinedTextField(
            value = row.input,
            onValueChange = onInputChange,
            label = { Text(labelText) },
            placeholder = placeholderContent,
            singleLine = row.type != PropertyType.Text,
            isError = isError,
            supportingText = { Text(hint, color = if (isError) MaterialTheme.colorScheme.error else hintColor) },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (row.type == PropertyType.Number) KeyboardType.Decimal else KeyboardType.Text
            ),
            trailingIcon = trailingContent,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag)
        )
    }
}

/** NoteLink 型の行。選択済みノートをチップで表示し、× で外せる。ピッカーは「ノートを選ぶ」で開く。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteLinkRowEditor(
    row: PropertyRowUi,
    labelText: String,
    hint: String,
    hintColor: androidx.compose.ui.graphics.Color,
    noteTitles: Map<NoteId, String>,
    onOpenNotePicker: () -> Unit,
    onRemoveNoteRef: (NoteId) -> Unit,
    onRemoveProperty: () -> Unit
) {
    val refs = (row.value as? PropertyValue.NoteRefs)?.ids.orEmpty()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.NOTE_EDIT_PROPERTY_PREFIX + row.name.value)
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(labelText, style = MaterialTheme.typography.bodyLarge)
                    Text(hint, style = MaterialTheme.typography.bodySmall, color = hintColor)
                }
                if (row.origin is PropertyOrigin.Free) {
                    RemoveButton(row, onRemoveProperty)
                }
            }
            if (refs.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    refs.forEach { id ->
                        val title = noteTitles[id]?.let { it.ifBlank { "(無題)" } } ?: "（削除済み）"
                        val suffix = row.name.value + "_" + id.value
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.testTag(TestTags.NOTE_EDIT_LINK_CHIP_PREFIX + suffix)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 12.dp)
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (noteTitles[id] == null) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    }
                                )
                                IconButton(
                                    onClick = { onRemoveNoteRef(id) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag(TestTags.NOTE_EDIT_LINK_REMOVE_PREFIX + suffix)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "$title を外す",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = onOpenNotePicker,
                modifier = Modifier.testTag(TestTags.NOTE_EDIT_LINK_PICK_PREFIX + row.name.value)
            ) { Text("ノートを選ぶ") }
        }
    }
}

@Composable
private fun RemoveButton(row: PropertyRowUi, onRemove: () -> Unit) {
    IconButton(
        onClick = onRemove,
        modifier = Modifier.testTag(TestTags.NOTE_EDIT_PROPERTY_REMOVE_PREFIX + row.name.value)
    ) {
        Icon(Icons.Default.Close, contentDescription = "${row.name.value} を削除")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddPropertyDialog(
    onConfirm: (String, PropertyType) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf<PropertyType>(PropertyType.Text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("プロパティを追加") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("プロパティ名") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.NOTE_EDIT_ADD_PROPERTY_NAME)
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PropertyInputs.selectableTypes.forEach { candidate ->
                        FilterChip(
                            selected = type == candidate,
                            onClick = { type = candidate },
                            label = { Text(PropertyInputs.label(candidate)) },
                            modifier = Modifier.testTag(
                                TestTags.NOTE_EDIT_ADD_PROPERTY_TYPE_PREFIX + PropertyInputs.key(candidate)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, type) },
                modifier = Modifier.testTag(TestTags.NOTE_EDIT_ADD_PROPERTY_CONFIRM)
            ) { Text("追加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
    )
}
