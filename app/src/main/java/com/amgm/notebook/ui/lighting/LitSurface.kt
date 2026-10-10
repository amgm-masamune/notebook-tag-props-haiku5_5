package com.amgm.notebook.ui.lighting

import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 押下の光の広がりが消えるまでの時間（ms）。押下中は 1、離すと 0 へ戻る。 */
private const val PRESS_ANIMATION_MS = 220

/** API 26〜32 で、面の上辺に引く光の線の太さ。 */
private val FallbackEdgeLine = 1.dp

/**
 * カードなどの面を、光源に照らされた素材として描く。
 *
 * - API 33 以上: [NotebookShaders.SURFACE]。角丸で切ったうえで、柔らかなスペキュラ、光源側の縁のリムライト、
 *   押下点からのグローを描く。光源の位置は要素ローカルに変換して渡す。
 * - API 26〜32: [baseColor] の塗り、光源側（左上）から透明へ向かう白の半透明、上辺の 1dp の光の線。
 *
 * [interactionSource] を渡すと、その押下（[PressInteraction.Press]）の位置から光が広がり、離すと消える。
 * 押下の状態は描画フェーズでだけ読む（再コンポーズは押下の開始と終了の時だけ）。
 */
fun Modifier.litSurface(
    cornerRadius: Dp,
    baseColor: Color,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val light = LocalLightState.current
    val density = LocalDensity.current
    val screen = rememberScreenSizePx()
    val bounds = rememberRootBounds()
    val cornerPx = with(density) { cornerRadius.toPx() }
    val shape = RoundedCornerShape(cornerRadius)

    // 押下の状態と位置は State のまま描画へ渡し、描画フェーズでだけ読む
    val isPressed = remember { mutableStateOf(false) }
    val pressPos = remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(interactionSource) {
        interactionSource?.interactions?.collect { interaction ->
            if (interaction is PressInteraction.Press) {
                pressPos.value = interaction.pressPosition
                isPressed.value = true
            } else if (interaction is PressInteraction.Release || interaction is PressInteraction.Cancel) {
                isPressed.value = false
            }
        }
    }
    val press = animateFloatAsState(
        targetValue = if (isPressed.value) 1f else 0f,
        animationSpec = tween(durationMillis = PRESS_ANIMATION_MS),
        label = "litSurfacePress"
    )

    if (ShaderSupport.isRuntimeShaderSupported) {
        Modifier.trackRootBounds(bounds).surfaceShader(
            light = light,
            bounds = bounds,
            screen = screen,
            shape = shape,
            cornerPx = cornerPx,
            baseColor = baseColor,
            press = press,
            pressPos = pressPos
        )
    } else {
        Modifier
            .clip(shape)
            .drawBehind { drawFallbackSurface(light, baseColor) }
    }
}

/** API 33 以上の面。角丸で切り、SURFACE シェーダーで全体を描く。 */
@RequiresApi(33)
@Composable
private fun Modifier.surfaceShader(
    light: LightState,
    bounds: MutableState<Rect>,
    screen: Size,
    shape: Shape,
    cornerPx: Float,
    baseColor: Color,
    press: State<Float>,
    pressPos: State<Offset>
): Modifier {
    val shader = rememberRuntimeShader(NotebookShaders.SURFACE)
    val brush = remember(shader) { ShaderBrush(shader) }
    return this
        .clip(shape)
        .drawBehind {
            val rect = bounds.value
            val lightLocal = toElementLocal(light.lightPositionPx(screen), rect.topLeft)
            shader.setUniform("iResolution", size)
            shader.setUniform("uLightPos", lightLocal)
            shader.setUniform("uBaseColor", baseColor)
            shader.setUniform("uHighlightColor", light.highlightColor)
            shader.setUniform("uRimColor", light.rimColor.copy(alpha = rimAlpha(light.isDark)))
            shader.setUniform("uRadius", cornerPx)
            shader.setUniform("uPress", press.value)
            shader.setUniform("uPressPos", pressPos.value)
            shader.setUniform("iTime", light.shaderTimeSec())
            drawRect(brush)
        }
}

/** API 26〜32 の面。 */
private fun DrawScope.drawFallbackSurface(light: LightState, baseColor: Color) {
    drawRect(baseColor)
    val whiteAlpha = if (light.isDark) 0.08f else 0.35f
    drawRect(
        Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = whiteAlpha), Color.Transparent),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )
    )
    val half = FallbackEdgeLine.toPx() / 2f
    drawLine(
        color = Color.White.copy(alpha = whiteAlpha * 1.6f),
        start = Offset(0f, half),
        end = Offset(size.width, half),
        strokeWidth = FallbackEdgeLine.toPx()
    )
}

/** 縁のリムライトの強さ（uRimColor のアルファ）。ライト 0.6、ダーク 0.25。 */
internal fun rimAlpha(isDark: Boolean): Float = if (isDark) 0.25f else 0.6f
