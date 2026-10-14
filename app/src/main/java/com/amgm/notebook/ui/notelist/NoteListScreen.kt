package com.amgm.notebook.ui.notelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.ui.TestTags
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm").withZone(ZoneId.systemDefault())

@Composable
fun NoteListScreen(viewModel: NoteListViewModel, snackbarHostState: SnackbarHostState) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NoteListContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onQueryChange = viewModel::onQueryChange,
        onTagFilterChange = viewModel::onTagFilterChange,
        onNoteClick = viewModel::onNoteClick,
        onCreateNoteClick = viewModel::onCreateNoteClick,
        onTagsClick = viewModel::onTagsClick,
        onDeleteNote = viewModel::onDeleteNote
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteListContent(
    state: NoteListUiState,
    snackbarHostState: SnackbarHostState,
    onQueryChange: (String) -> Unit,
    onTagFilterChange: (com.amgm.notebook.domain.model.TagId?) -> Unit,
    onNoteClick: (NoteId) -> Unit,
    onCreateNoteClick: () -> Unit,
    onTagsClick: () -> Unit,
    onDeleteNote: (NoteId) -> Unit
) {
    var deleteTarget by remember { mutableStateOf<NoteListItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ノート") },
                actions = {
                    TextButton(
                        onClick = onTagsClick,
                        modifier = Modifier.testTag(TestTags.NOTE_LIST_TAGS_BUTTON)
                    ) { Text("タグ") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateNoteClick,
                modifier = Modifier.testTag(TestTags.NOTE_LIST_FAB)
            ) {
                Icon(Icons.Default.Add, contentDescription = "新規ノート")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                singleLine = true,
                placeholder = { Text("タイトル・本文を検索") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "検索をクリア")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag(TestTags.NOTE_LIST_SEARCH)
            )
            if (state.availableTags.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.availableTags, key = { it.id.value }) { tag ->
                        FilterChip(
                            selected = state.filterTagId == tag.id,
                            onClick = { onTagFilterChange(tag.id) },
                            label = { Text(tag.name) },
                            modifier = Modifier.testTag(TestTags.NOTE_LIST_FILTER_CHIP_PREFIX + tag.id.value)
                        )
                    }
                }
            }
            when {
                state.isLoading -> CenteredProgress()
                state.items.isEmpty() -> {
                    Text(
                        text = if (state.query.isNotEmpty() || state.filterTagId != null) {
                            "該当するノートがありません"
                        } else {
                            "ノートがありません。右下の＋ボタンから作成できます"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(24.dp)
                            .testTag(TestTags.NOTE_LIST_EMPTY)
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.items, key = { it.id.value }) { item ->
                            NoteCard(
                                item = item,
                                onClick = { onNoteClick(item.id) },
                                onDeleteClick = { deleteTarget = item }
                            )
                        }
                    }
                }
            }
        }
    }

    val target = deleteTarget
    if (target != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("ノートを削除") },
            text = { Text("「${target.title.ifBlank { "(無題)" }}」を削除しますか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteNote(target.id)
                        deleteTarget = null
                    },
                    modifier = Modifier.testTag(TestTags.NOTE_LIST_DELETE_CONFIRM)
                ) { Text("削除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("キャンセル") }
            }
        )
    }
}

@Composable
private fun CenteredProgress() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteCard(item: NoteListItem, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.NOTE_LIST_ITEM_PREFIX + item.id.value)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title.ifBlank { "(無題)" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.snippet.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.snippet,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
                val tags = item.tagNames.joinToString("  ") { "#$it" }
                Text(
                    text = if (tags.isEmpty()) DateFormatter.format(item.updatedAt)
                    else "$tags  ·  ${DateFormatter.format(item.updatedAt)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.testTag(TestTags.NOTE_LIST_DELETE_PREFIX + item.id.value)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "削除")
            }
        }
    }
}
