package com.amgm.notebook.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.amgm.notebook.ui.lighting.softShadow

/**
 * タグのチップ。タグ色の淡い背景、選択状態は主色の塗り（選択中だけごく弱い影を落とす）。
 * [onClick] が null なら表示専用。testTag は [modifier] に付ける。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    tagColor: TagColor = rememberTagColor(label),
    showDot: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    // 選択中のチップだけ、ごく弱い影（elevation 2dp）。角は pill なので半径は高さの半分
    val chipModifier = if (selected) modifier.softShadow(cornerRadius = SelectedChipRadius, elevation = 2.dp) else modifier
    if (onClick == null) {
        Surface(
            modifier = chipModifier,
            shape = CircleShape,
            color = if (selected) scheme.primary else tagColor.background,
            contentColor = if (selected) scheme.onPrimary else tagColor.content
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    } else {
        val dotIcon: (@Composable () -> Unit)? = if (showDot) {
            {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(if (selected) scheme.onPrimary else tagColor.dot, CircleShape)
                )
            }
        } else {
            null
        }
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = dotIcon,
            shape = CircleShape,
            colors = FilterChipDefaults.filterChipColors(
                containerColor = tagColor.background,
                labelColor = tagColor.content,
                selectedContainerColor = scheme.primary,
                selectedLabelColor = scheme.onPrimary
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = Color.Transparent,
                selectedBorderColor = Color.Transparent
            ),
            modifier = chipModifier
        )
    }
}

/** 選択チップの影を描く角丸の半径（pill の半分の高さ）。 */
private val SelectedChipRadius = 16.dp
