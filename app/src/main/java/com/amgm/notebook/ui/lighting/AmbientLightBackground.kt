package com.amgm.notebook.ui.lighting

import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.hypot

/**
 * 画面全体の背景。背面に光と影のある紙を描き、その上に [content] を載せる。
 *
 * - API 33 以上: [NotebookShaders.AMBIENT]（紙色・光源からの減衰・木漏れ日・ビネット・粒状感）。
 * - API 26〜32: 紙色の上に、光源位置を中心とする放射状のグラデーションと、下端の薄い影。
 *
 * 光源の位置と時間は描画フェーズでだけ読むので、画面の再コンポーズは起きない。
 */
@Composable
fun AmbientLightBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    if (ShaderSupport.isRuntimeShaderSupported) {
        AmbientShaderBox(modifier = modifier, content = content)
    } else {
        AmbientFallbackBox(modifier = modifier, content = content)
    }
}

@RequiresApi(33)
@Composable
private fun AmbientShaderBox(
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val light = LocalLightState.current
    val shader = rememberRuntimeShader(NotebookShaders.AMBIENT)
    val brush = remember(shader) { ShaderBrush(shader) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                shader.setUniform("iResolution", size)
                shader.setUniform("iTime", light.shaderTimeSec())
                shader.setUniform("uLightPos", light.lightPositionPx(size))
                shader.setUniform("uBaseColor", light.baseColor)
                shader.setUniform("uKeyColor", light.keyColor)
                shader.setUniform("uShadowColor", light.shadowColor)
                shader.setUniform("uIntensity", AMBIENT_INTENSITY)
                shader.setUniform("uGrain", ambientGrain(light.isDark))
                shader.setUniform("uDark", if (light.isDark) 1f else 0f)
                drawRect(brush)
            },
        content = content
    )
}

@Composable
private fun AmbientFallbackBox(
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val light = LocalLightState.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawFallbackAmbient(light) },
        content = content
    )
}

/** API 26〜32 の背景。紙色、光源を中心とする暖色のにじみ、下端の薄い影。 */
private fun DrawScope.drawFallbackAmbient(light: LightState) {
    drawRect(light.baseColor)
    val center = light.lightPositionPx(size)
    val radius = hypot(size.width, size.height) * 0.9f
    val keyAlpha = if (light.isDark) 0.22f else 0.35f
    drawRect(
        Brush.radialGradient(
            colors = listOf(light.keyColor.copy(alpha = keyAlpha), Color.Transparent),
            center = center,
            radius = radius
        )
    )
    val shadowAlpha = if (light.isDark) 0.25f else 0.05f
    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, light.shadowColor.copy(alpha = shadowAlpha)),
            startY = size.height * 0.5f,
            endY = size.height
        )
    )
}

/** 光の強さ。ダークは光源の溜まりを絞る（シェーダー側の reach）ので、強さは 1 のまま。 */
private const val AMBIENT_INTENSITY = 1f

private fun ambientGrain(isDark: Boolean): Float = if (isDark) 0.012f else 0.018f
