package com.amgm.notebook.domain.model

/** プロパティ定義の型。 */
sealed interface PropertyType {
    data object Text : PropertyType
    data object Number : PropertyType
    data object Boolean : PropertyType

    /** ISO-8601 の LocalDate (yyyy-MM-dd)。 */
    data object Date : PropertyType

    /**
     * 他ノートへのリンク（複数可）。値は [PropertyValue.NoteRefs]。
     * [targetTagId] は UI が候補を絞り込む既定のタグ。null なら絞り込みなし。検証では強制しない。
     */
    data class NoteLink(val targetTagId: TagId? = null) : PropertyType
}
