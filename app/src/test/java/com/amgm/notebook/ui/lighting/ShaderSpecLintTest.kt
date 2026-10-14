package com.amgm.notebook.ui.lighting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AGSL を端末なしで静的に検査する。コンパイル自体は androidTest（ShaderCompileTest）で行う。
 * ここでは、uniform の宣言と [ShaderSpec.uniforms] の食い違い、定数でない for ループ、
 * 禁止された関数の混入を防ぐ。
 */
class ShaderSpecLintTest {

    private val declaration = Regex("""uniform\s+(float|float2|float3|float4|half|half2|half3|half4)\s+(\w+)\s*;""")

    private val componentCount = mapOf(
        "float" to 1, "float2" to 2, "float3" to 3, "float4" to 4,
        "half" to 1, "half2" to 2, "half3" to 3, "half4" to 4
    )

    private fun declaredUniforms(source: String): Map<String, String> =
        declaration.findAll(source).associate { it.groupValues[2] to it.groupValues[1] }

    private fun countWord(source: String, word: String): Int =
        Regex("""\b${Regex.escape(word)}\b""").findAll(source).count()

    @Test
    fun `全シェーダーは half4 main(float2 を持つ`() {
        NotebookShaders.all.forEach { spec ->
            assertTrue("${spec.name}: main が無い", spec.source.contains("half4 main(float2"))
        }
    }

    @Test
    fun `仕様の各 uniform は宣言されていて、宣言以外でも使われている`() {
        NotebookShaders.all.forEach { spec ->
            val declared = declaredUniforms(spec.source)
            spec.uniforms.forEach { u ->
                assertTrue("${spec.name}: ${u.name} が uniform 宣言に無い", declared.containsKey(u.name))
                assertTrue(
                    "${spec.name}: ${u.name} が宣言以外で使われていない（未使用 uniform は setFloatUniform が失敗する）",
                    countWord(spec.source, u.name) >= 2
                )
            }
        }
    }

    @Test
    fun `宣言されている uniform は全て仕様に含まれる`() {
        NotebookShaders.all.forEach { spec ->
            val specNames = spec.uniforms.map { it.name }.toSet()
            declaredUniforms(spec.source).keys.forEach { name ->
                assertTrue("${spec.name}: $name が ShaderSpec.uniforms に無い", name in specNames)
            }
        }
    }

    @Test
    fun `floatCount は宣言型の成分数と一致する`() {
        NotebookShaders.all.forEach { spec ->
            val declared = declaredUniforms(spec.source)
            spec.uniforms.forEach { u ->
                val type = declared[u.name] ?: return@forEach
                assertEquals("${spec.name}: ${u.name} の floatCount", componentCount.getValue(type), u.floatCount)
            }
        }
    }

    @Test
    fun `texture と uniform shader を含まない`() {
        NotebookShaders.all.forEach { spec ->
            assertFalse("${spec.name}: texture を使っている", spec.source.contains("texture"))
            assertFalse("${spec.name}: uniform shader を使っている", spec.source.contains("uniform shader"))
        }
    }

    @Test
    fun `波括弧の数が一致する`() {
        NotebookShaders.all.forEach { spec ->
            val open = spec.source.count { it == '{' }
            val close = spec.source.count { it == '}' }
            assertEquals("${spec.name}: { と } の数", open, close)
        }
    }

    @Test
    fun `for ループは定数の境界だけを使う`() {
        val loopHeader = Regex("""for\s*\(\s*int\s+\w+\s*=\s*\d+\s*;\s*\w+\s*<\s*\d+\s*;""")
        NotebookShaders.all.forEach { spec ->
            val loops = Regex("""for\s*\(""").findAll(spec.source).count()
            val constantLoops = loopHeader.findAll(spec.source).count()
            assertEquals("${spec.name}: 定数境界でない for ループがある", loops, constantLoops)
        }
    }

    @Test
    fun `SHADOW は要素本体（caster）の uniform を持つ`() {
        val names = NotebookShaders.SHADOW.uniforms.map { it.name }.toSet()
        assertEquals(
            setOf(
                "iResolution", "uRectCenter", "uRectSize", "uRadius", "uBlur", "uShadowColor",
                "uCasterCenter", "uCasterSize", "uCasterRadius"
            ),
            names
        )
    }

    @Test
    fun `uniform 名と shader 名に重複が無い`() {
        NotebookShaders.all.forEach { spec ->
            val names = spec.uniforms.map { it.name }
            assertEquals("${spec.name}: uniform 名の重複", names.size, names.toSet().size)
        }
        val shaderNames = NotebookShaders.all.map { it.name }
        assertEquals("ShaderSpec.name の重複", shaderNames.size, shaderNames.toSet().size)
        assertEquals(5, NotebookShaders.all.size)
    }
}
