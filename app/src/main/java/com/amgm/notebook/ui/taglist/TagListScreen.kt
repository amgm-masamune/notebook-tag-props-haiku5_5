package com.amgm.notebook.ui.taglist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagListScreen(viewModel: TagListViewModel, snackbarHostState: SnackbarHostState) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("タグ") },
                navigationIcon = {
                    IconButton(
                        onClick = viewModel::onBackClick,
                        modifier = Modifier.testTag(TestTags.TAG_LIST_BACK)
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::onCreateTagClick,
                modifier = Modifier.testTag(TestTags.TAG_LIST_FAB)
            ) { Icon(Icons.Default.Add, contentDescription = "新規タグ") }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when {
            state.isLoading -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }

            state.items.isEmpty() -> Text(
                text = "タグがありません。右下の＋ボタンから作成できます",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(24.dp)
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 複数親のタグは同じ ID が複数行に出るため、キーは位置を含める
                itemsIndexed(
                    state.items,
                    key = { index, item -> "${index}_${item.id.value}" }
                ) { _, item ->
                    val repeated = item.isAdditionalParentOccurrence
                    // testTag は初出行のみ ID で一意にし、2回目以降の出現行は別プレフィックスにする
                    val itemTag = (if (repeated) TestTags.TAG_LIST_ITEM_REPEAT_PREFIX else TestTags.TAG_LIST_ITEM_PREFIX) +
                        item.id.value
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (16 * item.depth).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.depth > 0) {
                            // 親子を示す矢印（親の下にぶら下がっていることを表す）
                            Text(
                                text = "↳",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                        Card(
                            onClick = { viewModel.onTagClick(item.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.depth == 0) {
                                    MaterialTheme.colorScheme.surfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag(itemTag)
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                                    val parents = if (item.parentNames.isEmpty()) "親なし"
                                    else "親: " + item.parentNames.joinToString("、")
                                    Text(
                                        text = "$parents  ·  定義 ${item.ownDefinitionCount} 件",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (repeated) {
                                        Text(
                                            text = "（複数の親）",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.onEditTagClick(item.id) },
                                    modifier = if (repeated) Modifier else {
                                        Modifier.testTag(TestTags.TAG_LIST_EDIT_PREFIX + item.id.value)
                                    }
                                ) { Icon(Icons.Default.Edit, contentDescription = "編集") }
                                IconButton(
                                    onClick = { viewModel.onDeleteTagClick(item.id) },
                                    modifier = if (repeated) Modifier else {
                                        Modifier.testTag(TestTags.TAG_LIST_DELETE_PREFIX + item.id.value)
                                    }
                                ) { Icon(Icons.Default.Delete, contentDescription = "削除") }
                            }
                        }
                    }
                }
            }
        }
    }

    val pending = state.pendingDelete
    if (pending != null) {
        AlertDialog(
            onDismissRequest = viewModel::onDeleteDismiss,
            title = { Text("タグを削除") },
            text = {
                Text("「${pending.name}」を削除しますか？ノートからタグが外れ、プロパティ値は自由プロパティとして残ります。")
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::onDeleteConfirm,
                    modifier = Modifier.testTag(TestTags.TAG_LIST_DELETE_CONFIRM)
                ) { Text("削除") }
            },
            dismissButton = { TextButton(onClick = viewModel::onDeleteDismiss) { Text("キャンセル") } }
        )
    }
}
