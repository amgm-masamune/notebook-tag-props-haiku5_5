package com.amgm.notebook.ui.lighting

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** コンポーネント（影・面・後光）が使う描画枠と座標変換の計算。 */
class LightComponentMathTest {

    private val delta = 1e-3f

    private fun assertOffset(expected: Offset, actual: Offset) {
        assertEquals("x", expected.x, actual.x, delta)
        assertEquals("y", expected.y, actual.y, delta)
    }

    private fun assertSize(expected: Size, actual: Size) {
        assertEquals("width", expected.width, actual.width, delta)
        assertEquals("height", expected.height, actual.height, delta)
    }

    // ---- toElementLocal ----

    @Test
    fun `画面座標は要素の左上を引いた要素ローカル座標になる`() {
        assertOffset(Offset(30f, 20f), toElementLocal(Offset(130f, 220f), Offset(100f, 200f)))
    }

    @Test
    fun `要素の外の光源は負の座標になる`() {
        assertOffset(Offset(-40f, -10f), toElementLocal(Offset(60f, 190f), Offset(100f, 200f)))
    }

    // ---- shadowExpansion / glowExpansion ----

    @Test
    fun `影の拡張量はぼかし幅の 3 倍にずれの絶対値を足す`() {
        assertOffset(Offset(33f, 34f), shadowExpansion(10f, Offset(3f, -4f)))
    }

    @Test
    fun `ずれが 0 の影の拡張量はぼかし幅の 3 倍だけ`() {
        assertOffset(Offset(30f, 30f), shadowExpansion(10f, Offset.Zero))
    }

    @Test
    fun `後光の拡張量は広がりの 4 倍で両軸とも同じ`() {
        assertOffset(Offset(96f, 96f), glowExpansion(24f))
    }

    // ---- shaderFrame ----

    @Test
    fun `描画枠は要素の各辺へ拡張量ぶん外へ広がる`() {
        val frame = shaderFrame(Size(100f, 50f), Offset(10f, 20f))
        assertOffset(Offset(-10f, -20f), frame.topLeft)
        assertSize(Size(120f, 90f), frame.size)
    }

    @Test
    fun `ずれのない枠では形の中心は要素の中心を枠の中へ移したもの`() {
        val frame = shaderFrame(Size(100f, 50f), Offset(10f, 20f))
        // 要素中心（50, 25）を枠の原点（-10, -20）からの座標にすると（60, 45）
        assertOffset(Offset(60f, 45f), frame.rectCenter)
    }

    @Test
    fun `影のずれは形の中心だけを動かし枠の大きさは変えない`() {
        val frame = shaderFrame(Size(100f, 50f), Offset(10f, 20f), Offset(5f, -3f))
        assertOffset(Offset(65f, 42f), frame.rectCenter)
        assertSize(Size(120f, 90f), frame.size)
    }

    @Test
    fun `ずれた影の形は枠の内側に収まる`() {
        val expansion = shadowExpansion(blurPx = 4f, offset = Offset(-6f, 5f))
        val frame = shaderFrame(Size(100f, 50f), expansion, Offset(-6f, 5f))
        // 形の左上と右下（ずれ込み）が枠の中にあること
        val left = frame.rectCenter.x - 50f
        val top = frame.rectCenter.y - 25f
        val right = frame.rectCenter.x + 50f
        val bottom = frame.rectCenter.y + 25f
        assertTrue("左上が枠の外: ($left, $top)", left >= 0f && top >= 0f)
        assertTrue(
            "右下が枠の外: ($right, $bottom) / ${frame.size}",
            right <= frame.size.width && bottom <= frame.size.height
        )
    }

    // ---- contactShadowAlpha ----

    @Test
    fun `接地影の不透明度は環境影の 1_3 倍で上限は 1`() {
        assertEquals(0.13f, contactShadowAlpha(0.1f), delta)
        assertEquals(1f, contactShadowAlpha(0.9f), delta)
    }
}
