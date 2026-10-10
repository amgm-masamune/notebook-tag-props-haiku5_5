package com.amgm.notebook.ui.lighting

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.amgm.notebook.ui.theme.LightHighlightDark
import com.amgm.notebook.ui.theme.LightKeyDark
import com.amgm.notebook.ui.theme.LightKeyLight
import com.amgm.notebook.ui.theme.LightRimDark
import com.amgm.notebook.ui.theme.PaperDark
import com.amgm.notebook.ui.theme.PaperLight
import com.amgm.notebook.ui.theme.TealDark
import com.amgm.notebook.ui.theme.TealLight
import com.amgm.notebook.ui.theme.TextLight

/**
 * 画面全体で 1 つの光源を表す状態。ライト「窓辺の朝の光」、ダーク「夜の卓上灯」。
 *
 * - [anchor] は画面に対する正規化座標（0..1 が画面内。負値は画面外）。
 * - [time] は秒。[rememberLightState] では 0 から [LOOP_SECONDS] まで循環する。
 * - [time] は**描画フェーズ（drawBehind / drawWithCache の onDraw 内）でのみ読むこと**。
 *   コンポジション中に読むと、毎フレームの再コンポーズが起きてしまう。
 */
@Stable
class LightState(
    val isDark: Boolean,
    val anchor: Offset,
    val time: State<Float>,
    val motionEnabled: Boolean,
    val keyColor: Color,
    val baseColor: Color,
    val shadowColor: Color,
    val highlightColor: Color,
    val rimColor: Color,
    val glowColor: Color
)

/** 光源の画面上の位置（ピクセル）。描画フェーズで呼ぶこと（[LightState.time] を読む）。 */
fun LightState.lightPositionPx(screenSize: Size): Offset {
    val t = if (motionEnabled) time.value else 0f
    return lightPositionAt(
        timeSec = t,
        anchor = Offset(anchor.x * screenSize.width, anchor.y * screenSize.height),
        driftRadius = screenSize.width * DRIFT_RATIO,
        periodSec = LIGHT_PERIOD_SEC
    )
}

/** 時間が 0 のまま固定されたライト（Composable の外、または Provider が無いときの既定値）。 */
private val ZeroTime: State<Float> = mutableFloatStateOf(0f)

private const val DRIFT_RATIO = 0.06f
private const val LIGHT_PERIOD_SEC = 28f

/**
 * [LightState] の既定値。Provider が無い場合に使われ、時間は 0 固定。
 * 通常は [rememberLightState] の結果を `CompositionLocalProvider` で渡す。
 */
val LocalLightState = staticCompositionLocalOf<LightState> {
    lightStateFor(isDark = false, time = ZeroTime, motionEnabled = false)
}

/**
 * テーマに応じた [LightState] を作って覚える。時間は [rememberInfiniteTransition] で進む。
 *
 * ループ長は [LOOP_SECONDS]（3584 秒 = 2^9 × 7）。光源の周期 28 秒、各種の脈動の周期 7 秒と 14 秒は
 * いずれもこの約数なので、ループの折り返しで光が跳ばない。
 *
 * モーション低減（ANIMATOR_DURATION_SCALE == 0）のときは [LightState.motionEnabled] が false になり、
 * 時間は 0 扱いで静止する。
 */
@Composable
fun rememberLightState(isDark: Boolean): LightState {
    val context = LocalContext.current
    val motionEnabled = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) != 0f
    }
    val transition = rememberInfiniteTransition(label = "lighting")
    val time = transition.animateFloat(
        initialValue = 0f,
        targetValue = LOOP_SECONDS,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = LOOP_MILLIS, easing = LinearEasing)
        ),
        label = "lightingTime"
    )
    return remember(isDark, motionEnabled, time) {
        lightStateFor(isDark = isDark, time = time, motionEnabled = motionEnabled)
    }
}

/** 時間ループの長さ（秒）。3584 = 2^9 × 7。 */
const val LOOP_SECONDS = 3584f
private const val LOOP_MILLIS = 3_584_000

private fun lightStateFor(isDark: Boolean, time: State<Float>, motionEnabled: Boolean): LightState =
    if (isDark) {
        LightState(
            isDark = true,
            anchor = Offset(0.5f, -0.25f),
            time = time,
            motionEnabled = motionEnabled,
            keyColor = LightKeyDark,
            baseColor = PaperDark,
            shadowColor = Color.Black,
            highlightColor = LightHighlightDark,
            rimColor = LightRimDark,
            glowColor = TealDark
        )
    } else {
        LightState(
            isDark = false,
            anchor = Offset(-0.15f, -0.20f),
            time = time,
            motionEnabled = motionEnabled,
            keyColor = LightKeyLight,
            baseColor = PaperLight,
            shadowColor = TextLight,
            highlightColor = Color.White,
            rimColor = Color.White,
            glowColor = TealLight
        )
    }
