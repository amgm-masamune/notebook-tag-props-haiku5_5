package com.amgm.notebook.ui.notelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.ui.TestTags
import com.amgm.notebook.ui.components.EmptyState
import com.amgm.notebook.ui.components.LargeTitleHeader
import com.amgm.notebook.ui.components.NotebookCard
import com.amgm.notebook.ui.components.PillSearchField
import com.amgm.notebook.ui.components.TagChip
import com.amgm.notebook.ui.components.neutralTagColor
import com.amgm.notebook.ui.theme.NotebookDimens
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneId.systemDefault())

/** 一覧のカードに載せるタグチップの最大数（超過分は「+n」）。 */
private const val MAX_VISIBLE_TAGS = 3

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

@Composable
private fun NoteListContent(
    state: NoteListUiState,
    snackbarHostState: SnackbarHostState,
    onQueryChange: (String) -> Unit,
    onTagFilterChange: (TagId?) -> Unit,
    onNoteClick: (NoteId) -> Unit,
    onCreateNoteClick: () -> Unit,
    onTagsClick: () -> Unit,
    onDeleteNote: (NoteId) -> Unit
) {
    var deleteTarget by remember { mutableStateOf<NoteListItem?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateNoteClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("新規ノート") },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 1.dp
                ),
                modifier = Modifier.testTag(TestTags.NOTE_LIST_FAB)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LargeTitleHeader(
                title = "ノート",
                subtitle = if (state.isLoading) null else "${state.items.size} 件のノート",
                actions = {
                    TextButton(
                        onClick = onTagsClick,
                        modifier = Modifier.testTag(TestTags.NOTE_LIST_TAGS_BUTTON)
                    ) { Text("タグ") }
                }
            )
            PillSearchField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = "タイトル・本文を検索",
                modifier = Modifier
                    .padding(horizontal = NotebookDimens.ScreenPadding, vertical = 8.dp)
                    .testTag(TestTags.NOTE_LIST_SEARCH)
            )
            if (state.availableTags.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = NotebookDimens.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.availableTags, key = { it.id.value }) { tag ->
                        TagChip(
                            label = tag.name,
                            selected = state.filterTagId == tag.id,
                            onClick = { onTagFilterChange(tag.id) },
                            showDot = true,
                            modifier = Modifier.testTag(TestTags.NOTE_LIST_FILTER_CHIP_PREFIX + tag.id.value)
                        )
                    }
                }
            }
            when {
                state.isLoading -> CenteredProgress()
                state.items.isEmpty() -> {
                    val filtering = state.query.isNotEmpty() || state.filterTagId != null
                    EmptyState(
                        icon = if (filtering) Icons.Default.Search else Icons.Default.Edit,
                        message = if (filtering) {
                            "該当するノートがありません"
                        } else {
                            "ノートがありません。右下の＋ボタンから作成できます"
                        },
                        modifier = Modifier.testTag(TestTags.NOTE_LIST_EMPTY)
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = NotebookDimens.ScreenPadding,
                            end = NotebookDimens.ScreenPadding,
                            top = 16.dp,
                            bottom = 96.dp
                        ),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteCard(item: NoteListItem, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    val excerpt = remember(item.snippet, item.title) { plainExcerpt(item.snippet, item.title) }
    NotebookCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TestTags.NOTE_LIST_ITEM_PREFIX + item.id.value)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = item.title.ifBlank { "(無題)" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 6.dp)
            )
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag(TestTags.NOTE_LIST_DELETE_PREFIX + item.id.value)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "削除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (excerpt.isNotBlank()) {
            Text(
                text = excerpt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                item.tagNames.take(MAX_VISIBLE_TAGS).forEach { name -> TagChip(label = name) }
                val rest = item.tagNames.size - MAX_VISIBLE_TAGS
                if (rest > 0) TagChip(label = "+$rest", tagColor = neutralTagColor())
            }
            Text(
                text = DateFormatter.format(item.updatedAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

private val HeadingMarker = Regex("^#{1,6}\\s*")
private val BlockMarker = Regex("^(>\\s*)+|^\\s*([-*+]|\\d+[.)])\\s+|\\[[ xX]\\]\\s*")
private val ImageSyntax = Regex("!\\[([^\\]]*)]\\([^)]*\\)")
private val LinkSyntax = Regex("\\[([^\\]]*)]\\([^)]*\\)")
private val InlineMarks = Regex("(\\*\\*|__|~~|[*`])")

/**
 * 本文の先頭付近から、Markdown 記号を除いた 1 行のプレーンテキストを取り出す。
 * タイトルと同じ行（H1 自動同期の見出し）とコードフェンス行は飛ばす。
 */
internal fun plainExcerpt(body: String, title: String): String {
    val titleText = title.trim()
    val lines = body.lines().map { line ->
        line.trim()
            .replace(HeadingMarker, "")
            .replace(BlockMarker, "")
            .replace(ImageSyntax, "$1")
            .replace(LinkSyntax, "$1")
            .replace(InlineMarks, "")
            .trim()
    }
    val candidates = lines.filter { it.isNotEmpty() && !it.startsWith("---") }
    return candidates.firstOrNull { it != titleText } ?: ""
}
