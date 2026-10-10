package com.amgm.notebook.ui.taglist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amgm.notebook.ui.TestTags
import com.amgm.notebook.ui.components.EmptyState
import com.amgm.notebook.ui.components.LargeTitleHeader
import com.amgm.notebook.ui.components.NotebookCard
import com.amgm.notebook.ui.components.rememberTagColor
import com.amgm.notebook.ui.theme.NotebookDimens
import com.amgm.notebook.ui.theme.extraColors

/** 1 段ネストするごとの字下げ幅（縦ガイド線の間隔と同じ）。 */
private val IndentStep: Dp = 22.dp

@Composable
fun TagListScreen(viewModel: TagListViewModel, snackbarHostState: SnackbarHostState) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::onCreateTagClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("新規タグ") },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 1.dp
                ),
                modifier = Modifier.testTag(TestTags.TAG_LIST_FAB)
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
                title = "タグ",
                subtitle = if (state.isLoading) null else "${state.items.size} 件",
                navigationIcon = {
                    IconButton(
                        onClick = viewModel::onBackClick,
                        modifier = Modifier.testTag(TestTags.TAG_LIST_BACK)
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る") }
                }
            )
            when {
                state.isLoading -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) { CircularProgressIndicator() }

                state.items.isEmpty() -> EmptyState(
                    icon = Icons.Default.Star,
                    message = "タグがありません。右下の＋ボタンから作成できます"
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = NotebookDimens.ScreenPadding,
                        end = NotebookDimens.ScreenPadding,
                        top = 12.dp,
                        bottom = 96.dp
                    )
                ) {
                    // 複数親のタグは同じ ID が複数行に出るため、キーは位置を含める
                    itemsIndexed(
                        state.items,
                        key = { index, item -> "${index}_${item.id.value}" }
                    ) { _, item ->
                        TagRow(
                            item = item,
                            onClick = { viewModel.onTagClick(item.id) },
                            onEdit = { viewModel.onEditTagClick(item.id) },
                            onDelete = { viewModel.onDeleteTagClick(item.id) }
                        )
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

/**
 * タグ 1 行。ネストは縦のガイド線（Canvas 描画）と字下げで表す。
 * testTag は初出行のみ ID で一意にし、2回目以降の出現行は別プレフィックスにする。
 */
@Composable
private fun TagRow(
    item: TagListItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val repeated = item.isAdditionalParentOccurrence
    val itemTag = (if (repeated) TestTags.TAG_LIST_ITEM_REPEAT_PREFIX else TestTags.TAG_LIST_ITEM_PREFIX) +
        item.id.value
    val guideColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val tagColor = rememberTagColor(item.name)
    val depth = item.depth

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // 祖先ごとの縦線と、自分の階層への肘（横線）
                val stroke = 1.5.dp.toPx()
                val step = IndentStep.toPx()
                for (level in 1..depth) {
                    val x = step * (level - 1) + step / 2f
                    val bottom = if (level == depth) size.height / 2f else size.height
                    drawLine(guideColor, Offset(x, 0f), Offset(x, bottom), strokeWidth = stroke)
                }
                if (depth > 0) {
                    val x = step * (depth - 1) + step / 2f
                    drawLine(
                        guideColor,
                        Offset(x, size.height / 2f),
                        Offset(step * depth, size.height / 2f),
                        strokeWidth = stroke
                    )
                }
            }
            .padding(start = IndentStep * depth, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NotebookCard(
            onClick = onClick,
            contentPadding = PaddingValues(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            modifier = Modifier
                .weight(1f)
                .alpha(if (repeated) 0.62f else 1f)
                .testTag(itemTag)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(tagColor.dot, CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val parents = if (item.parentNames.isEmpty()) "親なし"
                    else "親: " + item.parentNames.joinToString("、")
                    Text(
                        text = parents,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (repeated) {
                        Text(
                            text = "複数の親",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                CountPill(text = "定義 ${item.ownDefinitionCount} 件")
                IconButton(
                    onClick = onEdit,
                    modifier = if (repeated) Modifier.size(36.dp) else {
                        Modifier
                            .size(36.dp)
                            .testTag(TestTags.TAG_LIST_EDIT_PREFIX + item.id.value)
                    }
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "編集",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = if (repeated) Modifier.size(36.dp) else {
                        Modifier
                            .size(36.dp)
                            .testTag(TestTags.TAG_LIST_DELETE_PREFIX + item.id.value)
                    }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "削除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/** アクセント色の小さな件数ピル。 */
@Composable
private fun CountPill(text: String) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.extraColors.accentContainer,
        contentColor = MaterialTheme.extraColors.onAccentContainer,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}
