package com.amgm.notebook.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.amgm.notebook.domain.model.Tag
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.ui.components.TagChip

/** タグの選択チップを折り返し配置する。[tagPrefix] + TagId.value が testTag になる。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectableTagChips(
    tags: List<Tag>,
    selectedIds: Set<TagId>,
    tagPrefix: String,
    onToggle: (TagId) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tags.forEach { tag ->
            TagChip(
                label = tag.name,
                selected = tag.id in selectedIds,
                onClick = { onToggle(tag.id) },
                showDot = true,
                modifier = Modifier.testTag(tagPrefix + tag.id.value)
            )
        }
    }
}
