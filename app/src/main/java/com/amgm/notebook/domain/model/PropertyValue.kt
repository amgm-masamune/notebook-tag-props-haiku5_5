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
     * 空リストは作れない（「参照なし」は [Null] で表す）。空になり得る入力は [PropertyValue.ofNoteRefs] を使う。
     */
    data class NoteRefs(val ids: List<NoteId>) : PropertyValue {
        init {
            require(ids.isNotEmpty()) { "NoteRefs は空にできません（空は PropertyValue.Null）" }
            require(ids.none { it.value.contains(',') }) { "NoteRefs の ID にカンマは使えません" }
            require(ids.size == ids.toSet().size) { "NoteRefs に重複した ID があります" }
        }

        companion object {
            /** 重複を除去（最初の出現を残す）して生成する。空の場合は例外（空になり得るなら [PropertyValue.ofNoteRefs]）。 */
            fun of(ids: Iterable<NoteId>): NoteRefs = NoteRefs(ids.distinct())
        }
    }

    /**
     * 未設定（null）。タグを付けただけでまだ入力されていない状態。
     * 空文字列 `Text("")`（入力して空になった状態）とは区別する。型検証の対象外。
     */
    data object Null : PropertyValue

    /** 値が「未入力」か。[Null] と空文字列の [Text] が該当する（必須判定・空欄表示用）。 */
    val isUnset: Boolean
        get() = this is Null || (this is Text && value.isEmpty())

    /** この値が [type] に適合するか。[Null] は常に true。`Text("")` は Text 型に一致。 */
    fun matches(type: PropertyType): Boolean = when (this) {
        is Text -> type == PropertyType.Text
        is Number -> type == PropertyType.Number
        is Bool -> type == PropertyType.Boolean
        is Date -> type == PropertyType.Date
        is NoteRefs -> type is PropertyType.NoteLink
        is Null -> true
    }

    companion object {
        /** 参照リストから値を作る。重複は除去し、空なら [Null] に正規化する。 */
        fun ofNoteRefs(ids: Iterable<NoteId>): PropertyValue {
            val distinct = ids.distinct()
            return if (distinct.isEmpty()) Null else NoteRefs(distinct)
        }
    }
}
