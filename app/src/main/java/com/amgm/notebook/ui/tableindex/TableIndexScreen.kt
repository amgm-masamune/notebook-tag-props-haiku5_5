package com.amgm.notebook.ui.tableindex

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amgm.notebook.ui.TestTags
import com.amgm.notebook.ui.components.EmptyState
import com.amgm.notebook.ui.components.LargeTitleHeader
import com.amgm.notebook.ui.components.NotebookCard
import com.amgm.notebook.ui.components.rememberTagColor
import com.amgm.notebook.ui.theme.NotebookDimens
import com.amgm.notebook.ui.theme.extraColors

@Composable
fun TableIndexScreen(
    viewModel: TableIndexViewModel,
    snackbarHostState: SnackbarHostState,
    listState: LazyListState = rememberLazyListState()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LargeTitleHeader(
                title = "表",
                subtitle = if (state.isLoading) null else "タグごとにノートを一覧できます"
            )
            when {
                state.isLoading -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) { CircularProgressIndicator() }

                state.rows.isEmpty() -> EmptyState(
                    icon = Icons.Default.Menu,
                    message = "タグがありません。タグを作成すると、タグごとの表を見られます",
                    modifier = Modifier.testTag(TestTags.TABLE_INDEX_EMPTY)
                )

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(TestTags.TABLE_INDEX_LIST),
                    contentPadding = PaddingValues(
                        start = NotebookDimens.ScreenPadding,
                        end = NotebookDimens.ScreenPadding,
                        top = 12.dp,
                        bottom = 160.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.rows, key = { it.tagId.value }) { row ->
                        val tagColor = rememberTagColor(row.name)
                        NotebookCard(
                            onClick = { viewModel.onTagClick(row.tagId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(TestTags.TABLE_INDEX_ITEM_PREFIX + row.tagId.value)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // タグ色の角丸タイル（頭文字）
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(tagColor.background, RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (row.name.isEmpty()) "" else String(Character.toChars(row.name.codePointAt(0))),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = tagColor.content
                                    )
                                }
                                Text(
                                    text = row.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 14.dp)
                                )
                                Surface(
                                    shape = RoundedCornerShape(percent = 50),
                                    color = MaterialTheme.extraColors.accentContainer,
                                    contentColor = MaterialTheme.extraColors.onAccentContainer
                                ) {
                                    Text(
                                        text = "${row.noteCount} 件",
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
