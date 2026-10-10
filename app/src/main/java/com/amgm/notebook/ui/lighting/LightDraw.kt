package com.amgm.notebook.ui.lighting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity

/*
 * コンポーネント（SoftShadow / LitSurface / GlassPanel / GlowFab）が共通で使う描画・計測の補助。
 * 時間と位置は描画フェーズでだけ読む（ここで作る State も、読むのは drawBehind の中）。
 */

/**
 * シェーダーに渡す時間（秒）。モーション低減のときは 0 に固定して、木漏れ日や脈動を止める。
 * 描画フェーズ（drawBehind 等）の中で呼ぶこと。
 */
fun LightState.shaderTimeSec(): Float = if (motionEnabled) time.value else 0f

/**
 * 画面サイズ（px）。光源の位置を求めるのに使う。[LocalConfiguration] の dp 値と密度から作る。
 * 光源位置は画面座標なので、要素の位置（[trackRootBounds]）と同じ座標系とみなす。
 */
@Composable
fun rememberScreenSizePx(): Size {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current.density
    return remember(configuration, density) {
        Size(
            width = configuration.screenWidthDp * density,
            height = configuration.screenHeightDp * density
        )
    }
}

/**
 * 要素の画面上の矩形（位置と大きさ、px）を保持する状態。読むのは描画フェーズだけにする。
 * 値が変わっても、描画の無効化だけが起きて再コンポーズは起きない。
 */
@Composable
fun rememberRootBounds(): MutableState<Rect> = remember { mutableStateOf(Rect.Zero) }

/** 要素の画面上の矩形を [bounds] に書き戻す。配置のあとに 1 回ずつ呼ばれる。 */
fun Modifier.trackRootBounds(bounds: MutableState<Rect>): Modifier =
    onGloballyPositioned { coordinates ->
        bounds.value = Rect(
            offset = coordinates.positionInRoot(),
            size = Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
        )
    }
