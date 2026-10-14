package com.amgm.notebook.ui.lighting

import androidx.annotation.RequiresApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 後光の広がり（dp）。FAB の縁から外へ、指数減衰で消えていく。 */
private val GlowSpread = 24.dp
/** 後光の形の角丸（dp）。FAB は円形の pill なので、高さの半分に丸める。シェーダー側でも上限を取る。 */
private val GlowRadius = 28.dp
/** 後光の不透明度（光源色のアルファ）。 */
private const val GLOW_ALPHA = 0.45f
/** API 26〜32 の後光（Modifier.shadow）の高さ。 */
private val FallbackGlowElevation = 12.dp

/**
 * 後光つきの拡張 FAB。FAB の背後に [NotebookShaders.GLOW] の光を描く。
 *
 * [modifier] は FAB 本体に付く（testTag もそこへ届く）。後光は FAB の描画の背後に入るだけで、
 * 配置や大きさは変えない。API 26〜32 では spotColor に光源の後光色を使う [Modifier.shadow] で近似する。
 */
@Composable
fun GlowFab(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    ExtendedFloatingActionButton(
        text = { Text(text) },
        icon = icon,
        onClick = onClick,
        modifier = modifier.glowBehind(GlowRadius, GlowSpread),
        shape = CircleShape,
        containerColor = scheme.primary,
        contentColor = scheme.onPrimary,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 2.dp,
            pressedElevation = 1.dp
        )
    )
}

/** 角丸の形の背後に後光を描く。API 33 以上は GLOW、未満は glowColor を使った影。 */
internal fun Modifier.glowBehind(cornerRadius: Dp, spread: Dp): Modifier = composed {
    val light = LocalLightState.current
    val density = LocalDensity.current
    val cornerPx = with(density) { cornerRadius.toPx() }
    val spreadPx = with(density) { spread.toPx() }
    if (ShaderSupport.isRuntimeShaderSupported) {
        Modifier.glowShader(light = light, cornerPx = cornerPx, spreadPx = spreadPx)
    } else {
        val glow = light.glowColor.copy(alpha = GLOW_ALPHA)
        Modifier.shadow(
            elevation = FallbackGlowElevation,
            shape = RoundedCornerShape(cornerRadius),
            clip = false,
            ambientColor = glow,
            spotColor = glow
        )
    }
}

@RequiresApi(33)
@Composable
private fun Modifier.glowShader(light: LightState, cornerPx: Float, spreadPx: Float): Modifier {
    val shader = rememberRuntimeShader(NotebookShaders.GLOW)
    val brush = remember(shader) { ShaderBrush(shader) }
    return drawBehind {
        val frame = shaderFrame(
            elementSize = size,
            expansion = glowExpansion(spreadPx)
        )
        shader.setUniform("iResolution", frame.size)
        shader.setUniform("uRectCenter", frame.rectCenter)
        shader.setUniform("uRectSize", size)
        shader.setUniform("uRadius", cornerPx)
        shader.setUniform("uGlowColor", light.glowColor.copy(alpha = GLOW_ALPHA))
        shader.setUniform("uSpread", spreadPx)
        shader.setUniform("iTime", light.shaderTimeSec())
        translate(left = frame.topLeft.x, top = frame.topLeft.y) {
            drawRect(brush, size = frame.size)
        }
    }
}
