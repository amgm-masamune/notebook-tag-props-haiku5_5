package com.amgm.notebook.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.amgm.notebook.ui.lighting.ShaderSupport
import com.amgm.notebook.ui.lighting.litSurface
import com.amgm.notebook.ui.lighting.softShadow
import com.amgm.notebook.ui.theme.NotebookDimens

/** カードの影の強さ（elevation）。光源と反対向きに、ごく淡く落ちる。 */
private val CardElevation: Dp = 2.dp

/**
 * 角丸 20dp のサーフェスカード。面は光源に照らされた素材として描き（[litSurface]）、
 * 影は [softShadow] で落とす。[onClick] を渡すと押下中にわずかに縮むクリック可能カードになる。
 *
 * API 33 未満では縁を 1dp の罫線で描く（API 33 以上は光源側の縁のリムライトが縁の代わりになる）。
 */
@Composable
fun NotebookCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(NotebookDimens.CardPadding),
    content: @Composable ColumnScope.() -> Unit
) {
    val radius = NotebookDimens.CardRadius
    val shape = RoundedCornerShape(radius)
    val surface = MaterialTheme.colorScheme.surface
    val border = if (ShaderSupport.isRuntimeShaderSupported) {
        null
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    if (onClick == null) {
        Surface(
            modifier = modifier
                .softShadow(cornerRadius = radius, elevation = CardElevation)
                .litSurface(cornerRadius = radius, baseColor = surface),
            shape = shape,
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = border
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    } else {
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.98f else 1f,
            animationSpec = tween(durationMillis = 120),
            label = "cardPressScale"
        )
        Surface(
            onClick = onClick,
            modifier = modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .softShadow(cornerRadius = radius, elevation = CardElevation)
                .litSurface(
                    cornerRadius = radius,
                    baseColor = surface,
                    interactionSource = interactionSource
                ),
            shape = shape,
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = border,
            interactionSource = interactionSource
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}
