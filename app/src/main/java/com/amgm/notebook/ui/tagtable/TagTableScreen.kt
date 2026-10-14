package com.amgm.notebook.ui.tagtable

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.service.TableSort
import com.amgm.notebook.domain.service.TableSortKey
import com.amgm.notebook.domain.service.TableView
import com.amgm.notebook.ui.TestTags
import com.amgm.notebook.ui.common.PropertyInputs

private val TitleColumnWidth: Dp = 150.dp
private val CellWidth: Dp = 130.dp
private val RowHeight: Dp = 52.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagTableScreen(viewModel: TagTableViewModel, snackbarHostState: SnackbarHostState) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.tagName.isEmpty()) "表ビュー" else "${state.tagName} の表") },
                navigationIcon = {
                    IconButton(
                        onClick = viewModel::onBackClick,
                        modifier = Modifier.testTag(TestTags.TAG_TABLE_BACK)
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る") }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::onEditTagClick,
                        modifier = Modifier.testTag(TestTags.TAG_TABLE_EDIT)
                    ) { Icon(Icons.Default.Edit, contentDescription = "タグを編集") }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::onAddNoteClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("ノートを追加") },
                modifier = Modifier.testTag(TestTags.TAG_TABLE_ADD_NOTE)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val table = state.table
        when {
            state.isLoading || (table == null && state.tagFound) -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }

            table == null -> Unit

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                table.conflicts.forEach { conflict ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "「${conflict.name.value}」は継承元で型が衝突しているため列に表示されません",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                if (table.rows.isEmpty()) {
                    Text(
                        text = "このタグのノートはまだありません",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(24.dp)
                            .testTag(TestTags.TAG_TABLE_EMPTY)
                    )
                }
                TableContent(
                    table = table,
                    sort = state.sort,
                    noteTitles = state.noteTitles,
                    onSortChange = viewModel::onSortChange,
                    onRowClick = viewModel::onRowClick
                )
            }
        }
    }
}

@Composable
private fun TableContent(
    table: TableView,
    sort: TableSort?,
    noteTitles: Map<NoteId, String>,
    onSortChange: (TableSortKey) -> Unit,
    onRowClick: (NoteId) -> Unit
) {
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()
    val surface = MaterialTheme.colorScheme.surface
    val headerColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .verticalScroll(vertical)
            .horizontalScroll(horizontal)
            .testTag(TestTags.TAG_TABLE)
    ) {
        // ヘッダ行
        Row(modifier = Modifier.height(RowHeight)) {
            HeaderCell(
                label = "タイトル",
                key = TableSortKey.Title,
                sort = sort,
                tag = TestTags.TAG_TABLE_HEADER_PREFIX + "title",
                width = TitleColumnWidth,
                background = headerColor,
                pinned = horizontal,
                onClick = onSortChange
            )
            table.columns.forEach { column ->
                HeaderCell(
                    label = column.name.value + if (column.required) " *" else "",
                    key = TableSortKey.Property(column.name),
                    sort = sort,
                    tag = TestTags.TAG_TABLE_HEADER_PREFIX + column.name.value,
                    width = CellWidth,
                    background = headerColor,
                    pinned = null,
                    onClick = onSortChange
                )
            }
        }
        HorizontalDivider()
        table.rows.forEach { row ->
            Row(
                modifier = Modifier
                    .height(RowHeight)
                    .clickable { onRowClick(row.noteId) }
                    .testTag(TestTags.TAG_TABLE_ROW_PREFIX + row.noteId.value),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(TitleColumnWidth)
                        .height(RowHeight)
                        .offset { IntOffset(horizontal.value, 0) }
                        .zIndex(1f)
                        .background(surface)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = row.title.ifBlank { "(無題)" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                row.cells.forEach { cell ->
                    Box(
                        modifier = Modifier
                            .width(CellWidth)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = cellText(cell, noteTitles),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            HorizontalDivider()
        }
    }
}

/**
 * 表セルの表示文字列。NoteRefs は参照先タイトルのカンマ区切り（1 件も取得できなければ件数表示、
 * 一部欠けていればその参照は「（削除済み）」）。それ以外は [PropertyInputs.display]。
 */
internal fun cellText(cell: PropertyValue, noteTitles: Map<NoteId, String>): String {
    if (cell !is PropertyValue.NoteRefs) return PropertyInputs.display(cell)
    if (cell.ids.isEmpty()) return ""
    if (cell.ids.none { it in noteTitles }) return "${cell.ids.size} 件"
    return cell.ids.joinToString(", ") { id -> noteTitles[id]?.ifBlank { "(無題)" } ?: "（削除済み）" }
}

@Composable
private fun HeaderCell(
    label: String,
    key: TableSortKey,
    sort: TableSort?,
    tag: String,
    width: Dp,
    background: androidx.compose.ui.graphics.Color,
    pinned: ScrollState?,
    onClick: (TableSortKey) -> Unit
) {
    val indicator = when {
        sort == null || sort.column != key -> ""
        sort.ascending -> " ▲"
        else -> " ▼"
    }
    var modifier = Modifier
        .width(width)
        .height(RowHeight)
    if (pinned != null) {
        modifier = modifier.offset { IntOffset(pinned.value, 0) }.zIndex(1f)
    }
    Box(
        modifier = modifier
            .background(background)
            .clickable { onClick(key) }
            .testTag(tag)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = label + indicator,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
