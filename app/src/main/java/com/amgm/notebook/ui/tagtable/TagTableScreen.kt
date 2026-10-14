package com.amgm.notebook.ui.tagtable

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.service.TableSort
import com.amgm.notebook.domain.service.TableSortKey
import com.amgm.notebook.domain.service.TableView
import com.amgm.notebook.ui.TestTags
import com.amgm.notebook.ui.common.PropertyInputs
import com.amgm.notebook.ui.components.EmptyState
import com.amgm.notebook.ui.components.LargeTitleHeader
import com.amgm.notebook.ui.lighting.GlowFab
import com.amgm.notebook.ui.lighting.glassPanel
import com.amgm.notebook.ui.lighting.litSurface
import com.amgm.notebook.ui.lighting.softShadow
import com.amgm.notebook.ui.theme.NotebookDimens
import com.amgm.notebook.ui.theme.extraColors

private val TitleColumnWidth: Dp = 150.dp
private val CellWidth: Dp = 130.dp
private val RowHeight: Dp = 52.dp
private val HeaderHeight: Dp = 48.dp
private val HeaderRadius: Dp = 24.dp

@Composable
fun TagTableScreen(viewModel: TagTableViewModel, snackbarHostState: SnackbarHostState) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            GlowFab(
                text = "ノートを追加",
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = viewModel::onAddNoteClick,
                modifier = Modifier.testTag(TestTags.TAG_TABLE_ADD_NOTE)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val table = state.table
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 上部のバーは半透明のガラスの板に載せる
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .glassPanel(cornerRadius = HeaderRadius)
            ) {
                LargeTitleHeader(
                    title = if (state.tagName.isEmpty()) "表ビュー" else "${state.tagName} の表",
                    subtitle = table?.let { "${it.rows.size} 件のノート" },
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
            }
            when {
                state.isLoading || (table == null && state.tagFound) -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) { CircularProgressIndicator() }

                table == null -> Unit

                else -> {
                    table.conflicts.forEach { conflict ->
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(NotebookDimens.InputRadius),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = NotebookDimens.ScreenPadding, vertical = 4.dp)
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
                        EmptyState(
                            icon = Icons.Default.Add,
                            message = "このタグのノートはまだありません",
                            modifier = Modifier.testTag(TestTags.TAG_TABLE_EMPTY)
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
}

/**
 * 横スクロール可能な表。タイトル列は横スクロールしても固定し、ヘッダ行は縦スクロールしても残る。
 * 行は交互の薄い背景、数値は右寄せ。
 */
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
    val scheme = MaterialTheme.colorScheme
    val headerColor = scheme.surfaceVariant
    val baseColor = scheme.surface
    val altColor = MaterialTheme.extraColors.rowAlt

    // 表全体を 1 枚の光るカードに載せる。カードの高さは内容に合わせ、下端の FAB 分の余白を空ける
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = NotebookDimens.ScreenPadding, end = NotebookDimens.ScreenPadding, top = 8.dp, bottom = 120.dp)
            .softShadow(cornerRadius = NotebookDimens.CardRadius, elevation = NotebookDimens.MainElevation)
            .litSurface(cornerRadius = NotebookDimens.CardRadius, baseColor = baseColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .horizontalScroll(horizontal)
                .testTag(TestTags.TAG_TABLE)
        ) {
            // ヘッダ行（縦スクロールの外に置き、常に見える）
            Row(modifier = Modifier.height(HeaderHeight)) {
                HeaderCell(
                    label = "タイトル",
                    key = TableSortKey.Title,
                    sort = sort,
                    tag = TestTags.TAG_TABLE_HEADER_PREFIX + "title",
                    width = TitleColumnWidth,
                    background = headerColor,
                    pinned = horizontal,
                    numeric = false,
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
                        numeric = column.type == PropertyType.Number,
                        onClick = onSortChange
                    )
                }
            }
            HorizontalDivider(color = scheme.outlineVariant)
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(vertical)
            ) {
                table.rows.forEachIndexed { index, row ->
                    val even = index % 2 == 0
                    // 固定列もスクロールする列も同じ不透明の地を使い、横スクロール時に色の継ぎ目を出さない
                    val rowColor = if (even) baseColor else altColor
                    Row(
                        modifier = Modifier
                            .height(RowHeight)
                            .background(rowColor)
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
                                .background(rowColor)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = row.title.ifBlank { "(無題)" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        row.cells.forEachIndexed { columnIndex, cell ->
                            val numeric = cell is PropertyValue.Number ||
                                table.columns.getOrNull(columnIndex)?.type == PropertyType.Number
                            Box(
                                modifier = Modifier
                                    .width(CellWidth)
                                    .padding(horizontal = 16.dp),
                                contentAlignment = if (numeric) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Text(
                                    text = cellText(cell, noteTitles),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = if (numeric) TextAlign.End else TextAlign.Start,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
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
    background: Color,
    pinned: ScrollState?,
    numeric: Boolean,
    onClick: (TableSortKey) -> Unit
) {
    val indicator = when {
        sort == null || sort.column != key -> ""
        sort.ascending -> " ▲"
        else -> " ▼"
    }
    var modifier = Modifier
        .width(width)
        .height(HeaderHeight)
    if (pinned != null) {
        modifier = modifier.offset { IntOffset(pinned.value, 0) }.zIndex(1f)
    }
    Box(
        modifier = modifier
            .background(background)
            .clickable { onClick(key) }
            .testTag(tag)
            .padding(horizontal = 16.dp),
        contentAlignment = if (numeric) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Text(
            text = label + indicator,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (indicator.isEmpty()) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.primary
            },
            textAlign = if (numeric) TextAlign.End else TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
