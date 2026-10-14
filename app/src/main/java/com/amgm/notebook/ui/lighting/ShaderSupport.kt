package com.amgm.notebook.ui.lighting

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

/** AGSL（RuntimeShader）が使えるかどうかの判定。 */
object ShaderSupport {
    /**
     * API 33 以上なら true。呼び出し側はこれで分岐し、false のときは
     * グラデーション Brush と Modifier.shadow による近似を使う（docs/lighting.md 参照）。
     * [rememberRuntimeShader] と [RuntimeShader] の拡張関数は、true の分岐の中でだけ呼ぶこと。
     */
    val isRuntimeShaderSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}

/**
 * [spec] から RuntimeShader を作って保持する。コンパイルエラーはここで例外になる。
 * 呼び出し側は [ShaderSupport.isRuntimeShaderSupported] が true の分岐でのみ呼ぶこと。
 * シェーダーの値（uniform）は描画フェーズで設定し、ここでは作り直さない。
 */
@RequiresApi(33)
@Composable
fun rememberRuntimeShader(spec: ShaderSpec): RuntimeShader =
    remember(spec) { RuntimeShader(spec.source) }

/** sRGB の [Color] を float4 uniform として設定する。 */
@RequiresApi(33)
fun RuntimeShader.setUniform(name: String, color: Color) {
    val c = colorToFloats(color)
    setFloatUniform(name, c[0], c[1], c[2], c[3])
}

/** float2 uniform（座標）として [offset] を設定する。 */
@RequiresApi(33)
fun RuntimeShader.setUniform(name: String, offset: Offset) {
    setFloatUniform(name, offset.x, offset.y)
}

/** float2 uniform（サイズ）として [size] を設定する。 */
@RequiresApi(33)
fun RuntimeShader.setUniform(name: String, size: Size) {
    setFloatUniform(name, size.width, size.height)
}

/** float uniform を設定する。 */
@RequiresApi(33)
fun RuntimeShader.setUniform(name: String, value: Float) {
    setFloatUniform(name, value)
}
