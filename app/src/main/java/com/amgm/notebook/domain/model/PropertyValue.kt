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

    /**
     * 他ノートへの参照（順序保持・重複なし）。ID にカンマは使えない（永続化がカンマ区切りのため）。
     * 重複を含む入力は [of] で除去できる。コンストラクタは重複を拒否する。
     */
    data class NoteRefs(val ids: List<NoteId>) : PropertyValue {
        init {
            require(ids.none { it.value.contains(',') }) { "NoteRefs の ID にカンマは使えません" }
            require(ids.size == ids.toSet().size) { "NoteRefs に重複した ID があります" }
        }

        companion object {
            /** 重複を除去（最初の出現を残す）して生成する。 */
            fun of(ids: Iterable<NoteId>): NoteRefs = NoteRefs(ids.distinct())
        }
    }

    /** 値なし。型検証の対象外で、必須判定では「未入力」として扱う。 */
    data object Empty : PropertyValue

    /** この値が [type] に適合するか。[Empty] は常に true。 */
    fun matches(type: PropertyType): Boolean = when (this) {
        is Text -> type == PropertyType.Text
        is Number -> type == PropertyType.Number
        is Bool -> type == PropertyType.Boolean
        is Date -> type == PropertyType.Date
        is NoteRefs -> type is PropertyType.NoteLink
        is Empty -> true
    }
}
