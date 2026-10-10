package com.amgm.notebook.ui.lighting

import androidx.annotation.RequiresApi
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.amgm.notebook.ui.theme.GlassTintDark
import com.amgm.notebook.ui.theme.GlassTintLight

/** API 26〜32 の板の上辺に引く光の線の太さ。 */
private val FallbackEdgeLine = 1.dp

/**
 * 半透明の板（ボトムバー等）。背景のぼかしはせず、面色を少し透かして、上辺に細い光の線を引く。
 *
 * - API 33 以上: [NotebookShaders.GLASS]。板は白 0.72（ダーク #1C1E25 の 0.78）、上辺の 1px の線は
 *   白 0.95（ダーク 暖白 0.25）。
 * - API 26〜32: 板の色と上辺の 1dp の線（シェーダー版と同じ色）。
 */
fun Modifier.glassPanel(cornerRadius: Dp): Modifier = composed {
    val light = LocalLightState.current
    val density = LocalDensity.current
    val screen = rememberScreenSizePx()
    val bounds = rememberRootBounds()
    val cornerPx = with(density) { cornerRadius.toPx() }
    val shape = RoundedCornerShape(cornerRadius)
    val tint = glassTint(light.isDark)
    if (ShaderSupport.isRuntimeShaderSupported) {
        Modifier.trackRootBounds(bounds).glassShader(
            light = light,
            bounds = bounds,
            screen = screen,
            cornerPx = cornerPx,
            tint = tint,
            edgeColor = glassEdgeColor(light),
            shape = shape
        )
    } else {
        Modifier
            .clip(shape)
            .drawBehind { drawFallbackGlass(tint, glassEdgeColor(light)) }
    }
}

/** 板の色。ライトは白 0.72、ダークは #1C1E25 の 0.78。 */
private fun glassTint(isDark: Boolean): Color =
    if (isDark) GlassTintDark.copy(alpha = 0.78f) else GlassTintLight.copy(alpha = 0.72f)

/** 上辺の光の線の色（アルファを含む）。ライトは白 0.95、ダークは暖白 0.25。 */
private fun glassEdgeColor(light: LightState): Color =
    if (light.isDark) light.highlightColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.95f)

@RequiresApi(33)
@Composable
private fun Modifier.glassShader(
    light: LightState,
    bounds: MutableState<Rect>,
    screen: Size,
    cornerPx: Float,
    tint: Color,
    edgeColor: Color,
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
            shader.setUniform("uHighlightColor", edgeColor)
            shader.setUniform("uLightPos", lightLocal)
            shader.setUniform("uRadius", cornerPx)
            shader.setUniform("iTime", light.shaderTimeSec())
            drawRect(brush)
        }
}

/** API 26〜32 の板。板の色の塗りと、上辺の 1dp の光の線。 */
private fun DrawScope.drawFallbackGlass(tint: Color, edgeColor: Color) {
    drawRect(tint)
    val half = FallbackEdgeLine.toPx() / 2f
    drawLine(
        color = edgeColor,
        start = Offset(0f, half),
        end = Offset(size.width, half),
        strokeWidth = FallbackEdgeLine.toPx()
    )
}
