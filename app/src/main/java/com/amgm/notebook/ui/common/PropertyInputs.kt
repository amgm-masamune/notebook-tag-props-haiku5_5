package com.amgm.notebook.ui.common

import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.math.abs

/** プロパティ入力文字列と [PropertyValue] の相互変換、表示用整形（純Kotlin）。 */
object PropertyInputs {

    /** 入力を型に従って解釈する。空白のみは [PropertyValue.Empty]。解釈不能なら null。 */
    fun parse(type: PropertyType, input: String): PropertyValue? {
        if (input.isBlank()) return PropertyValue.Empty
        return when (type) {
            PropertyType.Text -> PropertyValue.Text(input)
            PropertyType.Number -> {
                val d = input.trim().toDoubleOrNull()
                if (d != null && d.isFinite()) PropertyValue.Number(d) else null
            }
            PropertyType.Boolean -> when (input.trim()) {
                "true" -> PropertyValue.Bool(true)
                "false" -> PropertyValue.Bool(false)
                else -> null
            }
            PropertyType.Date -> try {
                PropertyValue.Date(LocalDate.parse(input.trim()))
            } catch (e: DateTimeParseException) {
                null
            }
        }
    }

    /** 値を入力欄の文字列にする。 */
    fun toInput(value: PropertyValue): String = when (value) {
        is PropertyValue.Text -> value.value
        is PropertyValue.Number -> formatNumber(value.value)
        is PropertyValue.Bool -> if (value.value) "true" else "false"
        is PropertyValue.Date -> value.value.toString()
        is PropertyValue.Empty -> ""
    }

    /** 表などの表示用文字列。 */
    fun display(value: PropertyValue): String = when (value) {
        is PropertyValue.Bool -> if (value.value) "はい" else "いいえ"
        else -> toInput(value)
    }

    fun formatNumber(d: Double): String =
        if (d == Math.rint(d) && abs(d) < 1e15) d.toLong().toString() else d.toString()

    /** 自由プロパティの型を値から推定する（Empty は Text）。 */
    fun inferType(value: PropertyValue): PropertyType = when (value) {
        is PropertyValue.Text -> PropertyType.Text
        is PropertyValue.Number -> PropertyType.Number
        is PropertyValue.Bool -> PropertyType.Boolean
        is PropertyValue.Date -> PropertyType.Date
        is PropertyValue.Empty -> PropertyType.Text
    }

    val allTypes: List<PropertyType> =
        listOf(PropertyType.Text, PropertyType.Number, PropertyType.Boolean, PropertyType.Date)

    /** testTag 等に使う安定キー。 */
    fun key(type: PropertyType): String = when (type) {
        PropertyType.Text -> "Text"
        PropertyType.Number -> "Number"
        PropertyType.Boolean -> "Boolean"
        PropertyType.Date -> "Date"
    }

    fun label(type: PropertyType): String = when (type) {
        PropertyType.Text -> "テキスト"
        PropertyType.Number -> "数値"
        PropertyType.Boolean -> "真偽"
        PropertyType.Date -> "日付"
    }
}
