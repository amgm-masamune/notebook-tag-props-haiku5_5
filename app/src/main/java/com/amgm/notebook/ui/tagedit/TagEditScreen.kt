package com.amgm.notebook.ui.tagedit

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.ui.TestTags
import com.amgm.notebook.ui.common.PropertyInputs
import com.amgm.notebook.ui.common.SelectableTagChips
import com.amgm.notebook.ui.components.LargeTitleHeader
import com.amgm.notebook.ui.components.NotebookCard
import com.amgm.notebook.ui.components.NotebookInputShape
import com.amgm.notebook.ui.components.SectionHeader
import com.amgm.notebook.ui.components.TagChip
import com.amgm.notebook.ui.components.neutralTagColor
import com.amgm.notebook.ui.components.notebookFieldColors
import com.amgm.notebook.ui.lighting.glassPanel
import com.amgm.notebook.ui.theme.NotebookDimens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagEditScreen(viewModel: TagEditViewModel, snackbarHostState: SnackbarHostState) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // 上部のバー（戻る・保存）は半透明のガラスの板に載せる
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .glassPanel(cornerRadius = 24.dp)
            ) {
                LargeTitleHeader(
                    title = if (state.isNew) "新規タグ" else "タグ編集",
                    navigationIcon = {
                        IconButton(
                            onClick = viewModel::onBackClick,
                            modifier = Modifier.testTag(TestTags.TAG_EDIT_BACK)
                        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る") }
                    },
                    actions = {
                        TextButton(
                            onClick = viewModel::onSaveClick,
                            enabled = !state.isSaving && !state.isLoading,
                            modifier = Modifier.testTag(TestTags.TAG_EDIT_SAVE)
                        ) { Text("保存", fontWeight = FontWeight.Bold) }
                    }
                )
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
                    val error = state.error

                    // 名前
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("名前")
                        NotebookCard(modifier = Modifier.fillMaxWidth(), elevation = NotebookDimens.MainElevation) {
                            OutlinedTextField(
                                value = state.name,
                                onValueChange = viewModel::onNameChange,
                                label = { Text("タグ名") },
                                singleLine = true,
                                isError = error == TagEditError.BlankName || error == TagEditError.DuplicateName,
                                supportingText = {
                                    when (error) {
                                        TagEditError.BlankName -> Text("タグ名を入力してください")
                                        TagEditError.DuplicateName -> Text("同じ名前のタグが既に存在します")
                                        else -> Unit
                                    }
                                },
                                shape = NotebookInputShape,
                                colors = notebookFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(TestTags.TAG_EDIT_NAME)
                            )
                        }
                        if (error == TagEditError.Cyclic) {
                            ErrorCard("親タグの選択により継承が循環します。親タグを見直してください")
                        }
                    }

                    // 親タグ
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("親タグ")
                        NotebookCard(modifier = Modifier.fillMaxWidth(), elevation = NotebookDimens.MainElevation) {
                            if (state.parentCandidates.isEmpty()) {
                                Text(
                                    "親にできるタグがありません",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                SelectableTagChips(
                                    tags = state.parentCandidates,
                                    selectedIds = state.selectedParentIds,
                                    tagPrefix = TestTags.TAG_EDIT_PARENT_CHIP_PREFIX,
                                    onToggle = viewModel::onParentToggle
                                )
                            }
                        }
                    }

                    val inherited = state.inheritedSchema
                    if (inherited.definitions.isNotEmpty() || inherited.conflicts.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeader("継承されるプロパティ（読み取り専用）")
                            NotebookCard(modifier = Modifier.fillMaxWidth(), elevation = NotebookDimens.MainElevation) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    inherited.definitions.forEach { effective ->
                                        val d = effective.definition
                                        val from = state.tagNames[effective.definedBy] ?: effective.definedBy.value
                                        Text(
                                            text = "${d.name.value}  ·  ${PropertyInputs.label(d.type)}" +
                                                (if (d.required) "  ·  必須" else "") + "  ←  $from",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    inherited.conflicts.forEach { conflict ->
                                        val detail = conflict.candidates.joinToString("、") {
                                            "${state.tagNames[it.tagId] ?: it.tagId.value}: ${PropertyInputs.label(it.type)}"
                                        }
                                        ErrorCard(
                                            "「${conflict.name.value}」の型が親タグ間で衝突しています（$detail）。" +
                                                "このタグで同名のプロパティを定義すると解消できます"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // プロパティ定義（一覧カード）
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("プロパティ定義")
                        when (error) {
                            TagEditError.BlankDefinitionName -> ErrorCard("プロパティ名が空の定義があります")
                            is TagEditError.DuplicateDefinitionName ->
                                ErrorCard("プロパティ名「${error.name}」が重複しています")
                            else -> Unit
                        }
                        NotebookCard(modifier = Modifier.fillMaxWidth(), elevation = NotebookDimens.MainElevation) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                state.definitions.forEachIndexed { index, draft ->
                                    if (index > 0) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                    DefinitionEditor(
                                        index = index,
                                        count = state.definitions.size,
                                        draft = draft,
                                        tagChoices = state.tagNames.entries
                                            .sortedWith(compareBy<Map.Entry<TagId, String>>({ it.value.lowercase() }, { it.key.value }))
                                            .map { it.key to it.value },
                                        onChange = { viewModel.onDefinitionChange(index, it) },
                                        onRemove = { viewModel.onRemoveDefinition(index) },
                                        onMoveUp = { viewModel.onMoveDefinition(index, index - 1) },
                                        onMoveDown = { viewModel.onMoveDefinition(index, index + 1) }
                                    )
                                }
                                OutlinedButton(
                                    onClick = viewModel::onAddDefinition,
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag(TestTags.TAG_EDIT_ADD_DEFINITION)
                                ) { Text("プロパティ定義を追加") }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = NotebookInputShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DefinitionEditor(
    index: Int,
    count: Int,
    draft: PropertyDefinitionDraft,
    tagChoices: List<Pair<TagId, String>>,
    onChange: (PropertyDefinitionDraft) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft.name,
                onValueChange = { onChange(draft.copy(name = it)) },
                label = { Text("プロパティ名") },
                singleLine = true,
                shape = NotebookInputShape,
                colors = notebookFieldColors(),
                modifier = Modifier
                    .weight(1f)
                    .testTag(TestTags.TAG_EDIT_DEFINITION_NAME_PREFIX + index)
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.testTag(TestTags.TAG_EDIT_DEFINITION_REMOVE_PREFIX + index)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "定義を削除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PropertyInputs.selectableTypes.forEach { type ->
                TagChip(
                    label = PropertyInputs.label(type),
                    tagColor = neutralTagColor(),
                    selected = PropertyInputs.key(draft.type) == PropertyInputs.key(type),
                    onClick = {
                        // 同じ型を再タップしても NoteLink の対象タグ設定を消さない
                        if (PropertyInputs.key(draft.type) != PropertyInputs.key(type)) {
                            onChange(draft.copy(type = type))
                        }
                    },
                    modifier = Modifier.testTag(
                        TestTags.TAG_EDIT_DEFINITION_TYPE_PREFIX + index + "_" + PropertyInputs.key(type)
                    )
                )
            }
        }
        val linkType = draft.type as? PropertyType.NoteLink
        if (linkType != null) {
            Text(
                "候補の絞り込み（既定のタグ。編集時に変更可能で、強制はされません）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TagChip(
                    label = "指定なし",
                    tagColor = neutralTagColor(),
                    selected = linkType.targetTagId == null,
                    onClick = { onChange(draft.copy(type = PropertyType.NoteLink(null))) },
                    modifier = Modifier.testTag(
                        TestTags.TAG_EDIT_DEFINITION_LINK_TARGET_PREFIX + index + "_none"
                    )
                )
                tagChoices.forEach { (id, name) ->
                    TagChip(
                        label = name,
                        showDot = true,
                        selected = linkType.targetTagId == id,
                        onClick = { onChange(draft.copy(type = PropertyType.NoteLink(id))) },
                        modifier = Modifier.testTag(
                            TestTags.TAG_EDIT_DEFINITION_LINK_TARGET_PREFIX + index + "_" + id.value
                        )
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = draft.required,
                onCheckedChange = { onChange(draft.copy(required = it)) },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag(TestTags.TAG_EDIT_DEFINITION_REQUIRED_PREFIX + index)
            )
            Text("必須", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onMoveUp, enabled = index > 0) { Text("↑") }
            TextButton(onClick = onMoveDown, enabled = index < count - 1) { Text("↓") }
        }
    }
}
