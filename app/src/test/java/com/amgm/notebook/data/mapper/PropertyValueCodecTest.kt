package com.amgm.notebook.data.mapper

import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PropertyValueCodecTest {

    private fun roundTrip(value: PropertyValue): PropertyValue {
        val (type, text) = PropertyValueCodec.encode(value)
        return PropertyValueCodec.decode(type, text)
    }

    @Test
    fun `Textが往復する`() {
        listOf("", "abc", " 前後 空白 ", "改行\nあり", "日本語🙂").forEach {
            assertEquals(PropertyValue.Text(it), roundTrip(PropertyValue.Text(it)))
        }
    }

    @Test
    fun `Numberが境界値を含め往復する`() {
        listOf(
            0.0, -0.0, 1.0, -1.5, 0.1, 1e-300, 1e300,
            Double.MAX_VALUE, Double.MIN_VALUE, 123456789.123456789
        ).forEach {
            assertEquals(PropertyValue.Number(it), roundTrip(PropertyValue.Number(it)))
        }
    }

    @Test
    fun `Booleanが往復し文字列はtrueとfalse`() {
        assertEquals("true", PropertyValueCodec.valueText(PropertyValue.Bool(true)))
        assertEquals("false", PropertyValueCodec.valueText(PropertyValue.Bool(false)))
        assertEquals(PropertyValue.Bool(true), roundTrip(PropertyValue.Bool(true)))
        assertEquals(PropertyValue.Bool(false), roundTrip(PropertyValue.Bool(false)))
    }

    @Test
    fun `DateがISO形式で往復する`() {
        val leap = PropertyValue.Date(LocalDate.of(2024, 2, 29))
        assertEquals("2024-02-29", PropertyValueCodec.valueText(leap))
        assertEquals(leap, roundTrip(leap))
        val min = PropertyValue.Date(LocalDate.of(1, 1, 1))
        assertEquals(min, roundTrip(min))
    }

    @Test
    fun `空文字のTextとEmptyは区別される`() {
        assertEquals("TEXT", PropertyValueCodec.valueTypeName(PropertyValue.Text("")))
        assertEquals("EMPTY", PropertyValueCodec.valueTypeName(PropertyValue.Empty))
        assertNotEquals(roundTrip(PropertyValue.Text("")), roundTrip(PropertyValue.Empty))
        assertEquals(PropertyValue.Empty, roundTrip(PropertyValue.Empty))
    }

    @Test
    fun `未知のvalue_typeはEmptyになる`() {
        assertEquals(PropertyValue.Empty, PropertyValueCodec.decode("UNKNOWN", "x"))
        assertEquals(PropertyValue.Empty, PropertyValueCodec.decode("", ""))
        assertEquals(PropertyValue.Empty, PropertyValueCodec.decode("text", "x"))
    }

    @Test
    fun `不正な文字列はEmptyになりクラッシュしない`() {
        assertEquals(PropertyValue.Empty, PropertyValueCodec.decode("NUMBER", "abc"))
        assertEquals(PropertyValue.Empty, PropertyValueCodec.decode("NUMBER", ""))
        assertEquals(PropertyValue.Empty, PropertyValueCodec.decode("NUMBER", "NaN"))
        assertEquals(PropertyValue.Empty, PropertyValueCodec.decode("NUMBER", "Infinity"))
        assertEquals(PropertyValue.Empty, PropertyValueCodec.decode("BOOLEAN", "yes"))
        assertEquals(PropertyValue.Empty, PropertyValueCodec.decode("BOOLEAN", "TRUE"))
        assertEquals(PropertyValue.Empty, PropertyValueCodec.decode("DATE", "2024-13-01"))
        assertEquals(PropertyValue.Empty, PropertyValueCodec.decode("DATE", "2023-02-29"))
        assertEquals(PropertyValue.Empty, PropertyValueCodec.decode("DATE", ""))
    }

    @Test
    fun `型名の変換が往復する`() {
        listOf(PropertyType.Text, PropertyType.Number, PropertyType.Boolean, PropertyType.Date).forEach {
            assertEquals(it, PropertyValueCodec.parseType(PropertyValueCodec.typeName(it)))
        }
        assertEquals("BOOLEAN", PropertyValueCodec.typeName(PropertyType.Boolean))
        assertNull(PropertyValueCodec.parseType("EMPTY"))
        assertNull(PropertyValueCodec.parseType("whatever"))
    }
}
