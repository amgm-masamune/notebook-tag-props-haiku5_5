package com.amgm.notebook.ui.lighting

import android.graphics.RuntimeShader
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 接地影（近い層）の基準のぼかし幅。elevation によらず、これだけは常に柔らかくする。 */
private val ContactBaseBlur = 1.5f.dp
/** 接地影（近い層）のぼかし幅の増分。elevation に掛ける。 */
private const val CONTACT_BLUR_RATIO = 0.35f
/** 接地影のずれ。環境影のずれ（[shadowOffset]）に掛ける。 */
private const val CONTACT_OFFSET_RATIO = 0.25f
/** 環境影（遠い層）のぼかし幅。elevation に掛ける。 */
private const val AMBIENT_BLUR_RATIO = 3.0f
/** 影のずれの上限（dp）。 */
private val MAX_SHADOW_OFFSET = 16.dp

/**
 * 要素の周りに、光源と反対向きへ落ちる柔らかい影を描く。影は要素の外へはみ出して描く。
 *
 * - API 33 以上: [NotebookShaders.SHADOW] を 2 層（接地影と環境影）。
 *   接地影はぼかし 1.5dp + elevation × 0.35、ずれ 0.25 倍。環境影はぼかし elevation × 3.0、
 *   光源と反対方向へ elevation × 0.9 ずらす（[shadowOffset]）。
 * - API 26〜32: [Modifier.shadow]（色は影色と [shadowAlpha]）。
 *
 * 形は角丸矩形（[cornerRadius]）。影の強さは [elevation] で決まる。
 * 影の描画と光源の位置は描画フェーズでだけ読む。
 */
fun Modifier.softShadow(cornerRadius: Dp, elevation: Dp): Modifier = composed {
    val light = LocalLightState.current
    val density = LocalDensity.current
    val screen = rememberScreenSizePx()
    val bounds = rememberRootBounds()
    val cornerPx = with(density) { cornerRadius.toPx() }
    val elevationPx = with(density) { elevation.toPx() }
    val maxOffsetPx = with(density) { MAX_SHADOW_OFFSET.toPx() }
    val contactBaseBlurPx = with(density) { ContactBaseBlur.toPx() }
    val alpha = shadowAlpha(elevation.value, light.isDark)
    if (ShaderSupport.isRuntimeShaderSupported) {
        Modifier.trackRootBounds(bounds).shadowLayers(
            light = light,
            bounds = bounds,
            screen = screen,
            cornerPx = cornerPx,
            elevationDp = elevation.value,
            elevationPx = elevationPx,
            maxOffsetPx = maxOffsetPx,
            contactBlurPx = contactBaseBlurPx + elevationPx * CONTACT_BLUR_RATIO
        )
    } else {
        Modifier.shadow(
            elevation = elevation,
            shape = RoundedCornerShape(cornerRadius),
            clip = false,
            ambientColor = light.shadowColor.copy(alpha = alpha),
            spotColor = light.shadowColor.copy(alpha = alpha)
        )
    }
}

/**
 * 2 層の影を描くモディファイア。層ごとに別の RuntimeShader を使う（uniform を層ごとに設定するため）。
 */
@RequiresApi(33)
@Composable
private fun Modifier.shadowLayers(
    light: LightState,
    bounds: MutableState<Rect>,
    screen: Size,
    cornerPx: Float,
    elevationDp: Float,
    elevationPx: Float,
    maxOffsetPx: Float,
    contactBlurPx: Float
): Modifier {
    val contactShader = rememberRuntimeShader(NotebookShaders.SHADOW)
    val ambientShader = rememberRuntimeShader(NotebookShaders.SHADOW)
    val contactBrush = remember(contactShader) { ShaderBrush(contactShader) }
    val ambientBrush = remember(ambientShader) { ShaderBrush(ambientShader) }
    return drawBehind {
        val rect = bounds.value
        if (rect.width <= 0f || rect.height <= 0f) return@drawBehind

        // 光源と要素中心は、どちらも画面座標（root 座標）
        val lightPos = light.lightPositionPx(screen)
        val offset = shadowOffset(lightPos, rect.center, elevationPx, maxOffsetPx)
        val alpha = shadowAlpha(elevationDp, light.isDark)

        drawShadowLayer(
            shader = contactShader,
            brush = contactBrush,
            elementSize = size,
            offset = offset * CONTACT_OFFSET_RATIO,
            blurPx = contactBlurPx,
            cornerPx = cornerPx,
            color = light.shadowColor.copy(alpha = contactShadowAlpha(alpha))
        )
        drawShadowLayer(
            shader = ambientShader,
            brush = ambientBrush,
            elementSize = size,
            offset = offset,
            blurPx = elevationPx * AMBIENT_BLUR_RATIO,
            cornerPx = cornerPx,
            color = light.shadowColor.copy(alpha = alpha)
        )
    }
}

/**
 * 1 層の影を、要素の外側まで含む描画枠の中で描く。
 * 枠の原点へ translate し、シェーダーの座標（fragCoord）と iResolution をその枠に合わせる。
 */
@RequiresApi(33)
private fun DrawScope.drawShadowLayer(
    shader: RuntimeShader,
    brush: ShaderBrush,
    elementSize: Size,
    offset: Offset,
    blurPx: Float,
    cornerPx: Float,
    color: Color
) {
    val expansion = shadowExpansion(blurPx, offset)
    val frame = shaderFrame(
        elementSize = elementSize,
        expansion = expansion,
        offset = offset
    )
    // 要素本体（ずらす前）。影のずれには依らず、枠の中で要素の中心に当たる
    val caster = casterCenterInFrame(elementSize, expansion)
    shader.setUniform("iResolution", frame.size)
    shader.setUniform("uRectCenter", frame.rectCenter)
    shader.setUniform("uRectSize", elementSize)
    shader.setUniform("uRadius", cornerPx)
    shader.setUniform("uBlur", blurPx)
    shader.setUniform("uShadowColor", color)
    shader.setUniform("uCasterCenter", caster)
    shader.setUniform("uCasterSize", elementSize)
    shader.setUniform("uCasterRadius", cornerPx)
    translate(left = frame.topLeft.x, top = frame.topLeft.y) {
        drawRect(brush, size = frame.size)
    }
}
