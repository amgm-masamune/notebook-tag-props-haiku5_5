package com.amgm.notebook.ui.lighting

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
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
 * 要素の影のずれ量。光源から要素中心へ向かう単位ベクトル方向に、
 * `elevationPx * 0.5` の長さ（[maxOffsetPx] で頭打ち）だけ落とす。
 * 光源と中心が一致するときは真下へ `elevationPx * 0.25` ずらす。
 */
fun shadowOffset(lightPos: Offset, elementCenter: Offset, elevationPx: Float, maxOffsetPx: Float): Offset {
    val dx = elementCenter.x - lightPos.x
    val dy = elementCenter.y - lightPos.y
    val length = sqrt(dx * dx + dy * dy)
    if (length <= EPSILON) return Offset(0f, elevationPx * 0.25f)
    val magnitude = min(elevationPx * 0.5f, maxOffsetPx)
    return Offset(dx / length * magnitude, dy / length * magnitude)
}

/**
 * 影の不透明度。ライト: `0.06 + elevation * 0.012`（上限 0.22）。
 * ダーク: `0.20 + elevation * 0.02`（上限 0.45）。
 */
fun shadowAlpha(elevationDp: Float, isDark: Boolean): Float =
    if (isDark) {
        min(0.20f + elevationDp * 0.02f, 0.45f)
    } else {
        min(0.06f + elevationDp * 0.012f, 0.22f)
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
