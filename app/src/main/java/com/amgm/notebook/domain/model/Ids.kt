package com.amgm.notebook.domain.model

/** ノートの公称型ID。[TagId] と取り違えるとコンパイルエラーになる。 */
@JvmInline
value class NoteId(val value: String) {
    init {
        require(value.isNotBlank()) { "NoteId は空にできません" }
    }
}

/** タグの公称型ID。 */
@JvmInline
value class TagId(val value: String) {
    init {
        require(value.isNotBlank()) { "TagId は空にできません" }
    }
}

/** プロパティ名。trim 済みの非空文字列で、比較は大文字小文字を区別する完全一致。 */
@JvmInline
value class PropertyName(val value: String) {
    init {
        require(value.isNotEmpty() && value == value.trim()) {
            "PropertyName は trim 済みの非空文字列である必要があります: '$value'"
        }
    }
}
