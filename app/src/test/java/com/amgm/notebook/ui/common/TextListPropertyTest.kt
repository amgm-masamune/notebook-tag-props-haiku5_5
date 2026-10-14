package com.amgm.notebook.ui.common

import com.amgm.notebook.data.mapper.PropertyValueCodec
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextListPropertyTest {

    @Test
    fun `入力はカンマ区切りで解釈し空要素と前後空白を除く`() {
        val v = PropertyInputs.parse(PropertyType.TextList, " りんご , ,バナナ ,")
        assertEquals(PropertyValue.TextList(listOf("りんご", "バナナ")), v)
    }

    @Test
    fun `空入力はNullになる`() {
        assertEquals(PropertyValue.Null, PropertyInputs.parse(PropertyType.TextList, "  ,  "))
    }

    @Test
    fun `符号化して復元できる`() {
        val value = PropertyValue.TextList(listOf("a", "b", "a"))
        val (type, text) = PropertyValueCodec.encode(value)
        assertEquals("TEXT_LIST", type)
        assertEquals(value, PropertyValueCodec.decode(type, text))
    }

    @Test
    fun `型の名前が往復し入力欄の文字列になる`() {
        assertEquals(PropertyType.TextList, PropertyValueCodec.parseType("TEXT_LIST"))
        assertEquals("a, b", PropertyInputs.toInput(PropertyValue.TextList(listOf("a", "b"))))
    }

    @Test
    fun `型に適合する`() {
        assertTrue(PropertyValue.TextList(listOf("x")).matches(PropertyType.TextList))
        assertFalse(PropertyValue.TextList(listOf("x")).matches(PropertyType.Text))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `要素にカンマを含めない`() {
        PropertyValue.TextList(listOf("a,b"))
    }
}
