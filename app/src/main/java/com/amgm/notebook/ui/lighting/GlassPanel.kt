package com.amgm.notebook.ui.lighting

import androidx.annotation.RequiresApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** API 26〜32 の板の上辺に引く光の線の太さ。 */
private val FallbackEdgeLine = 1.dp

/**
 * 半透明の板（ボトムバー等）。背景のぼかしはせず、面色を少し透かして、上辺に細い光の線を引く。
 *
 * - API 33 以上: [NotebookShaders.GLASS]。
 * - API 26〜32: 面色（ライト alpha 0.82 / ダーク 0.78）と上辺の 1dp の線。
 */
fun Modifier.glassPanel(cornerRadius: Dp): Modifier = composed {
    val light = LocalLightState.current
    val density = LocalDensity.current
    val surface = MaterialTheme.colorScheme.surface
    val screen = rememberScreenSizePx()
    val bounds = rememberRootBounds()
    val cornerPx = with(density) { cornerRadius.toPx() }
    val shape = RoundedCornerShape(cornerRadius)
    val tintAlpha = if (light.isDark) 0.78f else 0.82f
    if (ShaderSupport.isRuntimeShaderSupported) {
        Modifier.trackRootBounds(bounds).glassShader(
            light = light,
            bounds = bounds,
            screen = screen,
            cornerPx = cornerPx,
            tint = surface.copy(alpha = tintAlpha),
            shape = shape
        )
    } else {
        Modifier
            .clip(shape)
            .drawBehind { drawFallbackGlass(surface.copy(alpha = tintAlpha), light) }
    }
}

@RequiresApi(33)
@Composable
private fun Modifier.glassShader(
    light: LightState,
    bounds: MutableState<Rect>,
    screen: Size,
    cornerPx: Float,
    tint: Color,
    shape: Shape
): Modifier {
    val shader = rememberRuntimeShader(NotebookShaders.GLASS)
    val brush = remember(shader) { ShaderBrush(shader) }
    return this
        .clip(shape)
        .drawBehind {
            val rect = bounds.value
            val lightLocal = toElementLocal(light.lightPositionPx(screen), rect.topLeft)
            shader.setUniform("iResolution", size)
            shader.setUniform("uTint", tint)
            shader.setUniform("uHighlightColor", Color.White)
            shader.setUniform("uLightPos", lightLocal)
            shader.setUniform("uRadius", cornerPx)
            shader.setUniform("iTime", light.shaderTimeSec())
            drawRect(brush)
        }
}

/** API 26〜32 の板。面色の塗りと、上辺の 1dp の光の線。 */
private fun DrawScope.drawFallbackGlass(tint: Color, light: LightState) {
    drawRect(tint)
    val half = FallbackEdgeLine.toPx() / 2f
    drawLine(
        color = Color.White.copy(alpha = if (light.isDark) 0.12f else 0.7f),
        start = Offset(0f, half),
        end = Offset(size.width, half),
        strokeWidth = FallbackEdgeLine.toPx()
    )
}
