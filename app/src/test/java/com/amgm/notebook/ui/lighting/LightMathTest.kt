package com.amgm.notebook.ui.lighting

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class LightMathTest {

    private val delta = 1e-3f

    /**
     * Compose の sRGB の Color は各チャンネルを 8bit（1/255 単位）で保持するため、
     * 値は最大 0.5/255 ずれうる。その丸めを許す誤差。
     */
    private val colorDelta = 1f / 255f + 1e-4f

    private fun assertOffset(expected: Offset, actual: Offset) {
        assertEquals("x", expected.x, actual.x, delta)
        assertEquals("y", expected.y, actual.y, delta)
    }

    // ---- lightPositionAt ----

    @Test
    fun `光源は周期ごとに元の位置へ戻る`() {
        val anchor = Offset(100f, 200f)
        val start = lightPositionAt(3f, anchor, 50f, 28f)
        val oneCycleLater = lightPositionAt(3f + 28f, anchor, 50f, 28f)
        val twoCyclesLater = lightPositionAt(3f + 56f, anchor, 50f, 28f)
        assertOffset(start, oneCycleLater)
        assertOffset(start, twoCyclesLater)
    }

    @Test
    fun `時刻0では anchor の右側（横半径ぶん）にある`() {
        val p = lightPositionAt(0f, Offset(100f, 200f), 50f, 28f)
        assertOffset(Offset(150f, 200f), p)
    }

    @Test
    fun `縦方向の半径は横の 0_6 倍`() {
        // 周期の 1/4 で真下（sin = 1）になる
        val p = lightPositionAt(7f, Offset(0f, 0f), 50f, 28f)
        assertEquals(0f, p.x, delta)
        assertEquals(50f * 0.6f, p.y, delta)
    }

    @Test
    fun `周期が 0 以下なら anchor をそのまま返す`() {
        val anchor = Offset(12f, 34f)
        assertOffset(anchor, lightPositionAt(5f, anchor, 50f, 0f))
        assertOffset(anchor, lightPositionAt(5f, anchor, 50f, -1f))
    }

    @Test
    fun `半径 0 なら時間によらず anchor にある`() {
        val anchor = Offset(7f, 9f)
        assertOffset(anchor, lightPositionAt(13.5f, anchor, 0f, 28f))
    }

    // ---- shadowOffset ----

    @Test
    fun `影は光源と反対方向に落ちる`() {
        // 光源は左上、要素は右下 → 影は右下へ
        val offset = shadowOffset(Offset(0f, 0f), Offset(300f, 400f), 8f, 100f)
        assertTrue(offset.x > 0f)
        assertTrue(offset.y > 0f)
    }

    @Test
    fun `影の長さは elevation の半分`() {
        val offset = shadowOffset(Offset(0f, 0f), Offset(300f, 400f), 8f, 100f)
        val length = sqrt(offset.x * offset.x + offset.y * offset.y)
        assertEquals(4f, length, delta)
    }

    @Test
    fun `影の長さは maxOffsetPx で頭打ちになる`() {
        val offset = shadowOffset(Offset(0f, 0f), Offset(300f, 400f), 100f, 6f)
        val length = sqrt(offset.x * offset.x + offset.y * offset.y)
        assertEquals(6f, length, delta)
    }

    @Test
    fun `光源と中心が一致したら真下へ elevation の 0_25 倍`() {
        val offset = shadowOffset(Offset(50f, 50f), Offset(50f, 50f), 8f, 100f)
        assertOffset(Offset(0f, 2f), offset)
    }

    // ---- shadowAlpha ----

    @Test
    fun `ライトの影の不透明度は 0_22 で頭打ち`() {
        assertEquals(0.06f, shadowAlpha(0f, isDark = false), delta)
        assertEquals(0.06f + 4f * 0.012f, shadowAlpha(4f, isDark = false), delta)
        assertEquals(0.22f, shadowAlpha(100f, isDark = false), delta)
    }

    @Test
    fun `ダークの影の不透明度は 0_45 で頭打ち`() {
        assertEquals(0.20f, shadowAlpha(0f, isDark = true), delta)
        assertEquals(0.20f + 4f * 0.02f, shadowAlpha(4f, isDark = true), delta)
        assertEquals(0.45f, shadowAlpha(100f, isDark = true), delta)
    }

    // ---- highlightCenter ----

    @Test
    fun `スペキュラの中心は光源側へ寄る`() {
        val size = Size(200f, 100f)
        val topLeft = Offset(0f, 0f)

        // 光源が左側 → 中心より左（ローカル x が小さい）
        val left = highlightCenter(Offset(-500f, 50f), topLeft, size)
        assertTrue(left.x < size.width / 2f)
        assertEquals(size.height / 2f, left.y, delta)

        // 光源が右上 → 中心より右かつ上
        val upperRight = highlightCenter(Offset(1000f, -1000f), topLeft, size)
        assertTrue(upperRight.x > size.width / 2f)
        assertTrue(upperRight.y < size.height / 2f)
    }

    @Test
    fun `スペキュラの中心は半サイズの 0_6 倍だけ寄る`() {
        val size = Size(200f, 100f)
        // 光源が真左（方向 (-1, 0)）
        val p = highlightCenter(Offset(-1000f, 50f), Offset(0f, 0f), size)
        assertEquals(100f - 100f * 0.6f, p.x, delta)
        assertEquals(50f, p.y, delta)
    }

    @Test
    fun `要素位置を移すとローカル座標の結果は変わらない`() {
        val size = Size(120f, 60f)
        val a = highlightCenter(Offset(-100f, -100f), Offset(0f, 0f), size)
        val b = highlightCenter(Offset(-100f + 300f, -100f + 500f), Offset(300f, 500f), size)
        assertOffset(a, b)
    }

    // ---- colorToFloats ----

    @Test
    fun `色は sRGB のまま RGBA の順で取り出す`() {
        val color = Color(red = 0.1f, green = 0.2f, blue = 0.3f, alpha = 0.4f)
        val floats = colorToFloats(color)
        assertEquals(4, floats.size)
        assertEquals(0.1f, floats[0], colorDelta)
        assertEquals(0.2f, floats[1], colorDelta)
        assertEquals(0.3f, floats[2], colorDelta)
        assertEquals(0.4f, floats[3], colorDelta)
    }

    @Test
    fun `非乗算で返す（アルファを色に掛けない）`() {
        val floats = colorToFloats(Color(red = 1f, green = 0.5f, blue = 0f, alpha = 0.5f))
        assertEquals(1f, floats[0], colorDelta)
        assertEquals(0.5f, floats[1], colorDelta)
        assertEquals(0f, floats[2], colorDelta)
        assertEquals(0.5f, floats[3], colorDelta)
    }
}
