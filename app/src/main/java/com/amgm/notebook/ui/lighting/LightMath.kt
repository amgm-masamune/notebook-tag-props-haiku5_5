package com.amgm.notebook.ui.lighting

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * 光と影の計算を担う純粋関数。Android に依存しないので JVM の単体テストで検証する。
 * 座標はすべてピクセル（画面座標、または要素ローカル座標）。
 */

private const val EPSILON = 1e-6f

/**
 * 光源の位置。[anchor] を中心に、横 [driftRadius]・縦 [driftRadius] × 0.6 の楕円を
 * [periodSec] 秒で一周する。[periodSec] が 0 以下なら [anchor] をそのまま返す。
 */
fun lightPositionAt(timeSec: Float, anchor: Offset, driftRadius: Float, periodSec: Float): Offset {
    if (periodSec <= 0f) return anchor
    val angle = 2.0 * PI * timeSec.toDouble() / periodSec.toDouble()
    return Offset(
        x = anchor.x + driftRadius * cos(angle).toFloat(),
        y = anchor.y + driftRadius * ELLIPSE_RATIO * sin(angle).toFloat()
    )
}

private const val ELLIPSE_RATIO = 0.6f

/**
 * 要素の（環境）影のずれ量。光源から要素中心へ向かう単位ベクトル方向に、
 * `elevationPx * 0.9` の長さ（[maxOffsetPx] で頭打ち）だけ落とす。
 * 光源と中心が一致するときは真下へ `elevationPx * 0.25` ずらす。
 */
fun shadowOffset(lightPos: Offset, elementCenter: Offset, elevationPx: Float, maxOffsetPx: Float): Offset {
    val dx = elementCenter.x - lightPos.x
    val dy = elementCenter.y - lightPos.y
    val length = sqrt(dx * dx + dy * dy)
    if (length <= EPSILON) return Offset(0f, elevationPx * 0.25f)
    val magnitude = min(elevationPx * 0.9f, maxOffsetPx)
    return Offset(dx / length * magnitude, dy / length * magnitude)
}

/**
 * 環境影（遠い層）の不透明度。ライト: `0.06 + elevation * 0.0117`（上限 0.13）。
 * ダーク: `0.30 + elevation * 0.025`（上限 0.45）。
 */
fun shadowAlpha(elevationDp: Float, isDark: Boolean): Float =
    if (isDark) {
        min(0.30f + elevationDp * 0.025f, 0.45f)
    } else {
        min(0.06f + elevationDp * 0.0117f, 0.13f)
    }

/**
 * 要素ローカル座標でのスペキュラの中心。要素中心から光源方向へ、
 * 半サイズの 0.6 倍だけ寄せた点（光源に最も近い側の内側）。
 */
fun highlightCenter(lightPos: Offset, elementTopLeft: Offset, elementSize: Size): Offset {
    val halfW = elementSize.width / 2f
    val halfH = elementSize.height / 2f
    val centerX = elementTopLeft.x + halfW
    val centerY = elementTopLeft.y + halfH
    val dx = lightPos.x - centerX
    val dy = lightPos.y - centerY
    val length = sqrt(dx * dx + dy * dy)
    val ux = if (length > EPSILON) dx / length else 0f
    val uy = if (length > EPSILON) dy / length else 0f
    return Offset(
        x = halfW + ux * halfW * HIGHLIGHT_REACH,
        y = halfH + uy * halfH * HIGHLIGHT_REACH
    )
}

private const val HIGHLIGHT_REACH = 0.6f

/** 色を sRGB のまま `[r, g, b, a]`（非乗算）の FloatArray にする。 */
fun colorToFloats(color: Color): FloatArray =
    floatArrayOf(color.red, color.green, color.blue, color.alpha)

/**
 * 接地影（近い層）の不透明度。環境影の [shadowAlpha] の 0.65 倍（ライト約 0.08、ダーク約 0.29）。
 */
fun contactShadowAlpha(ambientAlpha: Float): Float = min(1f, ambientAlpha * 0.65f)

/** 影のぼかし幅（px）に掛けて、描画矩形を広げる倍率。ガウス状の減衰が十分に 0 になる距離。 */
private const val SHADOW_REACH = 3f

/** 光の後光（GLOW）の広がり（px）に掛ける倍率。指数減衰が十分に小さくなる距離。 */
private const val GLOW_REACH = 4f

/**
 * 影シェーダーの描画矩形を、要素の各辺からどれだけ外へ広げるか（px、片側）。
 * ぼかし幅の [SHADOW_REACH] 倍に、影のずれの絶対値を足す（ずれた影の縁まで描けるように）。
 */
fun shadowExpansion(blurPx: Float, offset: Offset): Offset =
    Offset(blurPx * SHADOW_REACH + abs(offset.x), blurPx * SHADOW_REACH + abs(offset.y))

/** 後光シェーダーの描画矩形の拡張量（px、片側）。広がり [spreadPx] の [GLOW_REACH] 倍。 */
fun glowExpansion(spreadPx: Float): Offset = Offset(spreadPx * GLOW_REACH, spreadPx * GLOW_REACH)

/**
 * 要素の外側まで描くシェーダーの描画枠（要素ローカル座標）。
 * [topLeft] は枠の左上（負の値になる）。枠の中では [size] が iResolution、[rectCenter] が形の中心になる。
 * 描画は `translate(topLeft)` の中で行い、シェーダーの原点を枠の左上に合わせる。
 */
data class ShaderFrame(val topLeft: Offset, val size: Size, val rectCenter: Offset)

/**
 * 要素（[elementSize]、要素ローカルの原点が左上）を囲む描画枠を作る。
 * [expansion] は各辺の片側の拡張量、[offset] は形の中心のずれ（影の方向）。
 */
fun shaderFrame(elementSize: Size, expansion: Offset, offset: Offset = Offset.Zero): ShaderFrame {
    val topLeft = Offset(-expansion.x, -expansion.y)
    val size = Size(
        width = elementSize.width + expansion.x * 2f,
        height = elementSize.height + expansion.y * 2f
    )
    val rectCenter = Offset(
        x = elementSize.width / 2f + offset.x - topLeft.x,
        y = elementSize.height / 2f + offset.y - topLeft.y
    )
    return ShaderFrame(topLeft, size, rectCenter)
}

/** 画面座標の点を、要素ローカル座標（要素の左上が原点）へ変換する。要素の外の点でもよい。 */
fun toElementLocal(screenPoint: Offset, elementTopLeft: Offset): Offset =
    Offset(screenPoint.x - elementTopLeft.x, screenPoint.y - elementTopLeft.y)
