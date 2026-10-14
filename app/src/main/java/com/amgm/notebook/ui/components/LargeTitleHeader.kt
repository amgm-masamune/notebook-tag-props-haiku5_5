package com.amgm.notebook.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.amgm.notebook.ui.lighting.LocalLightState
import com.amgm.notebook.ui.theme.NotebookDimens

/**
 * TopAppBar の代わりに画面上部へ置く大きな見出し + サブタイトル。
 * [navigationIcon] は左端（戻るなど）、[actions] は右端。
 */
@Composable
fun LargeTitleHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    val light = LocalLightState.current
    val scheme = MaterialTheme.colorScheme
    // 見出しに光源側（左上）から差す淡い光。ライトは主色をわずかに明るく、ダークは白から灯りの縁の色へ
    val titleBrush = remember(light.isDark, scheme.primary) {
        if (light.isDark) {
            Brush.linearGradient(listOf(Color.White, light.rimColor))
        } else {
            Brush.linearGradient(listOf(lerp(scheme.primary, Color.White, 0.3f), scheme.primary))
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (navigationIcon != null) 8.dp else NotebookDimens.ScreenPadding,
                end = if (actions != null) 8.dp else NotebookDimens.ScreenPadding,
                top = 12.dp,
                bottom = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (navigationIcon != null) navigationIcon()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                // 文字は brush で塗るので color は指定しない（既定の文字色は画面の contentColor）
                style = MaterialTheme.typography.headlineLarge.copy(brush = titleBrush),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actions != null) actions()
    }
}
