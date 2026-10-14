package com.amgm.notebook.data.mapper

import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.model.TagId
import java.time.LocalDate

/**
 * [PropertyValue] / [PropertyType] と DB 列（文字列）の相互変換。純Kotlin。
 * 復元に失敗した値は例外にせず [PropertyValue.Empty] へフォールバックする。
 */
object PropertyValueCodec {
    const val TYPE_TEXT = "TEXT"
    const val TYPE_NUMBER = "NUMBER"
    const val TYPE_BOOLEAN = "BOOLEAN"
    const val TYPE_DATE = "DATE"
    const val TYPE_EMPTY = "EMPTY"

    /** 定義 `type` 列: ノートリンク。対象タグは別列 `link_target_tag_id`。 */
    const val TYPE_NOTE_LINK = "NOTE_LINK"

    /** `value_type` 列: ノート参照。`value_text` はカンマ区切りの NoteId。 */
    const val TYPE_NOTE_REFS = "NOTE_REFS"

    /** `type` 列の名前。 */
    fun typeName(type: PropertyType): String = when (type) {
        PropertyType.Text -> TYPE_TEXT
        PropertyType.Number -> TYPE_NUMBER
        PropertyType.Boolean -> TYPE_BOOLEAN
        PropertyType.Date -> TYPE_DATE
        is PropertyType.NoteLink -> TYPE_NOTE_LINK
    }

    /** `link_target_tag_id` 列の値。NoteLink 以外は null。 */
    fun linkTargetOf(type: PropertyType): String? =
        (type as? PropertyType.NoteLink)?.targetTagId?.value

    /** `type` 列から復元。未知の名前は null。NOTE_LINK の対象タグは [linkTarget] で指定する。 */
    fun parseType(name: String, linkTarget: String? = null): PropertyType? = when (name) {
        TYPE_NOTE_LINK -> PropertyType.NoteLink(linkTarget?.takeIf { it.isNotBlank() }?.let { TagId(it) })
        TYPE_TEXT -> PropertyType.Text
        TYPE_NUMBER -> PropertyType.Number
        TYPE_BOOLEAN -> PropertyType.Boolean
        TYPE_DATE -> PropertyType.Date
        else -> null
    }

    /** `value_type` 列の名前。 */
    fun valueTypeName(value: PropertyValue): String = when (value) {
        is PropertyValue.Text -> TYPE_TEXT
        is PropertyValue.Number -> TYPE_NUMBER
        is PropertyValue.Bool -> TYPE_BOOLEAN
        is PropertyValue.Date -> TYPE_DATE
        is PropertyValue.NoteRefs -> TYPE_NOTE_REFS
        is PropertyValue.Empty -> TYPE_EMPTY
    }

    /** `value_text` 列の文字列。 */
    fun valueText(value: PropertyValue): String = when (value) {
        is PropertyValue.Text -> value.value
        is PropertyValue.Number -> value.value.toString()
        is PropertyValue.Bool -> if (value.value) "true" else "false"
        is PropertyValue.Date -> value.value.toString()
        is PropertyValue.NoteRefs -> {
            require(value.ids.none { it.value.contains(',') }) { "NoteId にカンマは使えません" }
            value.ids.joinToString(",") { it.value }
        }
        is PropertyValue.Empty -> ""
    }

    /** (value_type, value_text) の組へ符号化。 */
    fun encode(value: PropertyValue): Pair<String, String> = valueTypeName(value) to valueText(value)

    /** (value_type, value_text) から復元。未知の型や不正な文字列は Empty。 */
    fun decode(valueType: String, valueText: String): PropertyValue = try {
        when (valueType) {
            TYPE_TEXT -> PropertyValue.Text(valueText)
            TYPE_NUMBER -> {
                val number = valueText.toDouble()
                if (number.isFinite()) PropertyValue.Number(number) else PropertyValue.Empty
            }
            TYPE_BOOLEAN -> when (valueText) {
                "true" -> PropertyValue.Bool(true)
                "false" -> PropertyValue.Bool(false)
                else -> PropertyValue.Empty
            }
            TYPE_DATE -> PropertyValue.Date(LocalDate.parse(valueText))
            TYPE_NOTE_REFS -> PropertyValue.NoteRefs.of(
                valueText.split(',').filter { it.isNotBlank() }.map { NoteId(it) }
            )
            else -> PropertyValue.Empty
        }
    } catch (e: RuntimeException) {
        PropertyValue.Empty
    }
}
