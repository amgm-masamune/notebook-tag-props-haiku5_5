package com.amgm.notebook.ui.tagedit

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amgm.notebook.ui.TestTags
import com.amgm.notebook.ui.common.PropertyInputs
import com.amgm.notebook.ui.common.SelectableTagChips

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagEditScreen(viewModel: TagEditViewModel, snackbarHostState: SnackbarHostState) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "新規タグ" else "タグ編集") },
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
                val error = state.error
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.TAG_EDIT_NAME)
                )
                if (error == TagEditError.Cyclic) {
                    ErrorCard("親タグの選択により継承が循環します。親タグを見直してください")
                }

                SectionTitle("親タグ")
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

                val inherited = state.inheritedSchema
                if (inherited.definitions.isNotEmpty() || inherited.conflicts.isNotEmpty()) {
                    SectionTitle("継承されるプロパティ（読み取り専用）")
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

                SectionTitle("プロパティ定義")
                when (error) {
                    TagEditError.BlankDefinitionName -> ErrorCard("プロパティ名が空の定義があります")
                    is TagEditError.DuplicateDefinitionName ->
                        ErrorCard("プロパティ名「${error.name}」が重複しています")
                    else -> Unit
                }
                state.definitions.forEachIndexed { index, draft ->
                    DefinitionEditor(
                        index = index,
                        count = state.definitions.size,
                        draft = draft,
                        onChange = { viewModel.onDefinitionChange(index, it) },
                        onRemove = { viewModel.onRemoveDefinition(index) },
                        onMoveUp = { viewModel.onMoveDefinition(index, index - 1) },
                        onMoveDown = { viewModel.onMoveDefinition(index, index + 1) }
                    )
                }
                OutlinedButton(
                    onClick = viewModel::onAddDefinition,
                    modifier = Modifier.testTag(TestTags.TAG_EDIT_ADD_DEFINITION)
                ) { Text("プロパティ定義を追加") }
                Spacer(Modifier.height(24.dp))
            }
        }
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
private fun ErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
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
    onChange: (PropertyDefinitionDraft) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { onChange(draft.copy(name = it)) },
                    label = { Text("プロパティ名") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TestTags.TAG_EDIT_DEFINITION_NAME_PREFIX + index)
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.testTag(TestTags.TAG_EDIT_DEFINITION_REMOVE_PREFIX + index)
                ) { Icon(Icons.Default.Close, contentDescription = "定義を削除") }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PropertyInputs.allTypes.forEach { type ->
                    FilterChip(
                        selected = draft.type == type,
                        onClick = { onChange(draft.copy(type = type)) },
                        label = { Text(PropertyInputs.label(type)) },
                        modifier = Modifier.testTag(
                            TestTags.TAG_EDIT_DEFINITION_TYPE_PREFIX + index + "_" + PropertyInputs.key(type)
                        )
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = draft.required,
                    onCheckedChange = { onChange(draft.copy(required = it)) },
                    modifier = Modifier.testTag(TestTags.TAG_EDIT_DEFINITION_REQUIRED_PREFIX + index)
                )
                Text("必須", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onMoveUp, enabled = index > 0) { Text("↑") }
                TextButton(onClick = onMoveDown, enabled = index < count - 1) { Text("↓") }
            }
        }
    }
}
