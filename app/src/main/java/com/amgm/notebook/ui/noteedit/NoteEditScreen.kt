package com.amgm.notebook.ui.noteedit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.rules.ValidationIssue
import com.amgm.notebook.ui.TestTags
import com.amgm.notebook.ui.common.PropertyInputs
import com.amgm.notebook.ui.common.SelectableTagChips
import com.amgm.notebook.ui.components.NotebookCard
import com.amgm.notebook.ui.components.NotebookInputShape
import com.amgm.notebook.ui.components.SectionHeader
import com.amgm.notebook.ui.components.TagChip
import com.amgm.notebook.ui.components.neutralTagColor
import com.amgm.notebook.ui.components.notebookBorderlessFieldColors
import com.amgm.notebook.ui.components.notebookFieldColors
import com.amgm.notebook.ui.markdown.MarkdownView
import com.amgm.notebook.ui.theme.NotebookDimens

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
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // 戻る(アイコン)と保存(テキスト)だけの軽いバー
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag(TestTags.NOTE_EDIT_BACK)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                }
                Spacer(Modifier.weight(1f))
                if (!state.isNew) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.testTag(TestTags.NOTE_EDIT_DELETE)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "削除",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(
                    onClick = onSaveClick,
                    enabled = !state.isSaving && !state.isLoading,
                    modifier = Modifier.testTag(TestTags.NOTE_EDIT_SAVE)
                ) { Text("保存", fontWeight = FontWeight.Bold) }
            }
            if (state.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) { CircularProgressIndicator() }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = NotebookDimens.ScreenPadding, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(NotebookDimens.SectionSpacing)
                ) {
                    // タイトル(大きな見出しスタイルの入力) + H1 同期
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BasicTextField(
                            value = state.title,
                            onValueChange = onTitleChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (state.title.isEmpty()) {
                                        Text(
                                            text = "タイトル",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(TestTags.NOTE_EDIT_TITLE)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, NotebookInputShape)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, NotebookInputShape)
                                .padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
                        ) {
                            Text(
                                "タイトルに合わせて見出しを更新",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = state.autoSyncTitleH1,
                                onCheckedChange = onAutoSyncChange,
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.testTag(TestTags.NOTE_EDIT_AUTO_SYNC_H1)
                            )
                        }
                    }

                    // タグ
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("タグ")
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
                            shape = CircleShape,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.testTag(TestTags.NOTE_EDIT_CREATE_TAG)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("タグを作成")
                        }
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
                    }

                    // プロパティ
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("プロパティ")
                        NotebookCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                                state.propertyRows.forEachIndexed { index, row ->
                                    if (index > 0) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }
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
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag(TestTags.NOTE_EDIT_ADD_PROPERTY)
                                ) { Text("プロパティを追加") }
                            }
                        }
                    }

                    // 本文
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("本文")
                        NotebookCard(modifier = Modifier.fillMaxWidth()) {
                            val scheme = MaterialTheme.colorScheme
                            val segmentColors = SegmentedButtonDefaults.colors(
                                activeContainerColor = scheme.primaryContainer,
                                activeContentColor = scheme.onPrimaryContainer,
                                activeBorderColor = scheme.primaryContainer,
                                inactiveContainerColor = scheme.surface,
                                inactiveContentColor = scheme.onSurfaceVariant,
                                inactiveBorderColor = scheme.outlineVariant
                            )
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = state.mode == EditorMode.Edit,
                                    onClick = { onModeChange(EditorMode.Edit) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    colors = segmentColors,
                                    modifier = Modifier.testTag(TestTags.NOTE_EDIT_MODE_EDIT)
                                ) { Text("編集") }
                                SegmentedButton(
                                    selected = state.mode == EditorMode.Preview,
                                    onClick = { onModeChange(EditorMode.Preview) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    colors = segmentColors,
                                    modifier = Modifier.testTag(TestTags.NOTE_EDIT_MODE_PREVIEW)
                                ) { Text("プレビュー") }
                            }
                            Spacer(Modifier.height(12.dp))
                            if (state.mode == EditorMode.Edit) {
                                OutlinedTextField(
                                    value = state.body,
                                    onValueChange = onBodyChange,
                                    placeholder = { Text("Markdown で記述") },
                                    minLines = 8,
                                    textStyle = MaterialTheme.typography.bodyLarge,
                                    shape = NotebookInputShape,
                                    colors = notebookBorderlessFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag(TestTags.NOTE_EDIT_BODY)
                                )
                            } else {
                                Box(
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
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    } else {
                                        MarkdownView(
                                            document = state.previewDocument,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
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
private fun ConflictNotice(issue: ValidationIssue.Conflict, tagNames: Map<TagId, String>) {
    val detail = issue.conflict.candidates.joinToString("、") {
        "${tagNames[it.tagId] ?: it.tagId.value}: ${PropertyInputs.label(it.type)}"
    }
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = NotebookInputShape,
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
        null -> {
            val base = if (definedByName != null) "$typeLabel ・ 定義: $definedByName" else "$typeLabel ・ 自由プロパティ"
            if (row.value is PropertyValue.Null && row.type != PropertyType.Boolean) "未入力 ・ $base" else base
        }
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(labelText, style = MaterialTheme.typography.labelLarge)
                Text(
                    if (row.input.isBlank()) "未設定 ・ $hint" else hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) MaterialTheme.colorScheme.error else hintColor
                )
            }
            Switch(
                checked = row.input.trim() == "true",
                onCheckedChange = { onInputChange(if (it) "true" else "false") },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.testTag(tag)
            )
            if (row.input.isNotBlank()) {
                TextButton(onClick = { onInputChange("") }) { Text("クリア") }
            }
            if (row.origin is PropertyOrigin.Free) {
                RemoveButton(row, onRemove)
            }
        }
    } else {
        // Null は「未入力」のプレースホルダ（薄い文字）。Text("") は空の入力欄（プレースホルダなし）
        val placeholderContent: (@Composable () -> Unit)? =
            if (row.value is PropertyValue.Null) {
                { Text(if (row.type == PropertyType.Date) "未入力 (yyyy-MM-dd)" else "未入力") }
            } else {
                null
            }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    labelText,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                if (row.origin is PropertyOrigin.Free) {
                    RemoveButton(row, onRemove)
                }
            }
            OutlinedTextField(
                value = row.input,
                onValueChange = onInputChange,
                placeholder = placeholderContent,
                singleLine = row.type != PropertyType.Text,
                isError = isError,
                supportingText = { Text(hint, color = if (isError) MaterialTheme.colorScheme.error else hintColor) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (row.type == PropertyType.Number) KeyboardType.Decimal else KeyboardType.Text
                ),
                shape = NotebookInputShape,
                colors = notebookFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(tag)
            )
        }
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
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.NOTE_EDIT_PROPERTY_PREFIX + row.name.value)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(labelText, style = MaterialTheme.typography.labelLarge)
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
                        color = if (noteTitles[id] == null) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        shape = CircleShape,
                        modifier = Modifier.testTag(TestTags.NOTE_EDIT_LINK_CHIP_PREFIX + suffix)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 14.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (noteTitles[id] == null) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
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
        // 視認性の高い主色のピル
        Button(
            onClick = onOpenNotePicker,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            modifier = Modifier.testTag(TestTags.NOTE_EDIT_LINK_PICK_PREFIX + row.name.value)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("ノートを選ぶ")
        }
    }
}

@Composable
private fun RemoveButton(row: PropertyRowUi, onRemove: () -> Unit) {
    IconButton(
        onClick = onRemove,
        modifier = Modifier
            .size(32.dp)
            .testTag(TestTags.NOTE_EDIT_PROPERTY_REMOVE_PREFIX + row.name.value)
    ) {
        Icon(
            Icons.Default.Close,
            contentDescription = "${row.name.value} を削除",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
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
                    shape = NotebookInputShape,
                    colors = notebookFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.NOTE_EDIT_ADD_PROPERTY_NAME)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PropertyInputs.selectableTypes.forEach { candidate ->
                        TagChip(
                            label = PropertyInputs.label(candidate),
                            selected = type == candidate,
                            onClick = { type = candidate },
                            tagColor = neutralTagColor(),
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
