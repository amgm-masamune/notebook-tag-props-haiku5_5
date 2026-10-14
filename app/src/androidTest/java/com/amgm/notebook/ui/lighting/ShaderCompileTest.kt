package com.amgm.notebook.ui.lighting

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 全 AGSL を実機（API 33 以上）でコンパイルし、宣言された全 uniform に値を設定できることを確認する。
 * AGSL の文法エラーは RuntimeShader の生成で例外になる。失敗メッセージにはシェーダー名を含める。
 */
@RunWith(AndroidJUnit4::class)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class ShaderCompileTest {

    @Test
    fun ambient_compilesAndAcceptsAllUniforms() = assertCompiles(NotebookShaders.AMBIENT)

    @Test
    fun surface_compilesAndAcceptsAllUniforms() = assertCompiles(NotebookShaders.SURFACE)

    @Test
    fun shadow_compilesAndAcceptsAllUniforms() = assertCompiles(NotebookShaders.SHADOW)

    @Test
    fun glass_compilesAndAcceptsAllUniforms() = assertCompiles(NotebookShaders.GLASS)

    @Test
    fun glow_compilesAndAcceptsAllUniforms() = assertCompiles(NotebookShaders.GLOW)

    private fun assertCompiles(spec: ShaderSpec) {
        Assume.assumeTrue(
            "RuntimeShader は API 33 以上でのみ使える",
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        )
        val shader = try {
            RuntimeShader(spec.source)
        } catch (e: Exception) {
            throw AssertionError("AGSL のコンパイルに失敗: ${spec.name}", e)
        }
        spec.uniforms.forEach { uniform ->
            try {
                shader.setFloatUniform(uniform.name, FloatArray(uniform.floatCount) { 0.5f })
            } catch (e: Exception) {
                throw AssertionError(
                    "uniform の設定に失敗: shader=${spec.name}, uniform=${uniform.name}",
                    e
                )
            }
        }
    }
}
