package com.amgm.notebook.data.mapper

import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.model.TagId
import java.time.LocalDate

/**
 * [PropertyValue] / [PropertyType] と DB 列（文字列）の相互変換。純Kotlin。
 * 復元に失敗した値は例外にせず [PropertyValue.Null] へフォールバックする。
 */
object PropertyValueCodec {
    const val TYPE_TEXT = "TEXT"
    const val TYPE_NUMBER = "NUMBER"
    const val TYPE_BOOLEAN = "BOOLEAN"
    const val TYPE_DATE = "DATE"
    /** 未設定（Null）。`value_text` は "" 固定（列は NOT NULL のまま）。 */
    const val TYPE_NULL = "NULL"

    /** 旧バージョンが書いた Empty。読み込み時は Null として扱う。 */
    const val TYPE_EMPTY = "EMPTY"

    /** `value_type` / 定義 `type` 列: テキストのリスト。値は `value_text` にカンマ区切り。 */
    const val TYPE_TEXT_LIST = "TEXT_LIST"

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
        PropertyType.TextList -> TYPE_TEXT_LIST
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
        TYPE_TEXT_LIST -> PropertyType.TextList
        else -> null
    }

    /** `value_type` 列の名前。 */
    fun valueTypeName(value: PropertyValue): String = when (value) {
        is PropertyValue.Text -> TYPE_TEXT
        is PropertyValue.Number -> TYPE_NUMBER
        is PropertyValue.Bool -> TYPE_BOOLEAN
        is PropertyValue.Date -> TYPE_DATE
        is PropertyValue.TextList -> TYPE_TEXT_LIST
        is PropertyValue.NoteRefs -> TYPE_NOTE_REFS
        is PropertyValue.Null -> TYPE_NULL
    }

    /** `value_text` 列の文字列。 */
    fun valueText(value: PropertyValue): String = when (value) {
        is PropertyValue.Text -> value.value
        is PropertyValue.Number -> value.value.toString()
        is PropertyValue.Bool -> if (value.value) "true" else "false"
        is PropertyValue.Date -> value.value.toString()
        is PropertyValue.TextList -> value.items.joinToString(",")
        is PropertyValue.NoteRefs -> {
            require(value.ids.none { it.value.contains(',') }) { "NoteId にカンマは使えません" }
            value.ids.joinToString(",") { it.value }
        }
        is PropertyValue.Null -> ""
    }

    /** (value_type, value_text) の組へ符号化。 */
    fun encode(value: PropertyValue): Pair<String, String> = valueTypeName(value) to valueText(value)

    /** (value_type, value_text) から復元。未知の型や不正な文字列は Null。NULL / 旧 EMPTY は Null、TEXT+"" は Text("")。 */
    fun decode(valueType: String, valueText: String): PropertyValue = try {
        when (valueType) {
            TYPE_TEXT -> PropertyValue.Text(valueText)
            TYPE_NUMBER -> {
                val number = valueText.toDouble()
                if (number.isFinite()) PropertyValue.Number(number) else PropertyValue.Null
            }
            TYPE_BOOLEAN -> when (valueText) {
                "true" -> PropertyValue.Bool(true)
                "false" -> PropertyValue.Bool(false)
                else -> PropertyValue.Null
            }
            TYPE_DATE -> PropertyValue.Date(LocalDate.parse(valueText))
            TYPE_TEXT_LIST -> PropertyValue.TextList.of(valueText.split(','))
            TYPE_NOTE_REFS -> PropertyValue.ofNoteRefs(
                valueText.split(',').filter { it.isNotBlank() }.map { NoteId(it) }
            )
            TYPE_NULL, TYPE_EMPTY -> PropertyValue.Null
            else -> PropertyValue.Null
        }
    } catch (e: RuntimeException) {
        PropertyValue.Null
    }
}
