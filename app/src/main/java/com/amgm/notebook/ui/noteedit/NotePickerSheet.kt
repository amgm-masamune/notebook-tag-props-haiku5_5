package com.amgm.notebook.ui.noteedit

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.Tag
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.ui.TestTags

/** ピッカーの操作コールバック。 */
class NotePickerActions(
    val onQueryChange: (String) -> Unit,
    val onFilterChange: (TagId?) -> Unit,
    val onToggle: (NoteId) -> Unit,
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit
)

/**
 * NoteLink プロパティの値を選ぶ ModalBottomSheet。
 * 検索欄・タグ絞り込みチップ（「すべて」で解除）・チェックボックス付きノート一覧・確定/キャンセル。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotePickerSheet(
    picker: NotePickerUi,
    tags: List<Tag>,
    actions: NotePickerActions
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = actions.onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
                .testTag(TestTags.NOTE_PICKER),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "「${picker.propertyName.value}」に紐付けるノート",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = picker.query,
                onValueChange = actions.onQueryChange,
                label = { Text("タイトルで検索") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.NOTE_PICKER_SEARCH)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                FilterChip(
                    selected = picker.filterTagId == null,
                    onClick = { actions.onFilterChange(null) },
                    label = { Text("すべて") },
                    modifier = Modifier.testTag(TestTags.NOTE_PICKER_FILTER_ALL)
                )
                tags.forEach { tag ->
                    FilterChip(
                        selected = picker.filterTagId == tag.id,
                        onClick = { actions.onFilterChange(tag.id) },
                        label = { Text(tag.name) },
                        modifier = Modifier.testTag(TestTags.NOTE_PICKER_FILTER_TAG_PREFIX + tag.id.value)
                    )
                }
            }
            if (picker.candidates.isEmpty()) {
                Text(
                    text = "該当するノートがありません",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .weight(1f)
                        .testTag(TestTags.NOTE_PICKER_EMPTY)
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(picker.candidates, key = { it.id.value }) { candidate ->
                        val checked = candidate.id in picker.selectedIds
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = checked,
                                    role = Role.Checkbox,
                                    onValueChange = { actions.onToggle(candidate.id) }
                                )
                                .padding(vertical = 4.dp)
                                .testTag(TestTags.NOTE_PICKER_ITEM_PREFIX + candidate.id.value)
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Text(
                                text = candidate.title.ifBlank { "(無題)" },
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "${picker.selectedIds.size} 件選択中",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = actions.onDismiss,
                    modifier = Modifier.testTag(TestTags.NOTE_PICKER_CANCEL)
                ) { Text("キャンセル") }
                Button(
                    onClick = actions.onConfirm,
                    modifier = Modifier.testTag(TestTags.NOTE_PICKER_CONFIRM)
                ) { Text("確定") }
            }
        }
    }
}
