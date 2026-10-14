package com.amgm.notebook.domain.model

/** ノートが持つプロパティ値。 */
sealed interface PropertyValue {
    data class Text(val value: String) : PropertyValue

    data class Number(val value: Double) : PropertyValue {
        init {
            require(value.isFinite()) { "Number は有限値である必要があります: $value" }
        }
    }

    data class Bool(val value: Boolean) : PropertyValue
    data class Date(val value: java.time.LocalDate) : PropertyValue

    /** 値なし。型検証の対象外で、必須判定では「未入力」として扱う。 */
    data object Empty : PropertyValue

    /** この値が [type] に適合するか。[Empty] は常に true。 */
    fun matches(type: PropertyType): Boolean = when (this) {
        is Text -> type == PropertyType.Text
        is Number -> type == PropertyType.Number
        is Bool -> type == PropertyType.Boolean
        is Date -> type == PropertyType.Date
        is Empty -> true
    }
}
