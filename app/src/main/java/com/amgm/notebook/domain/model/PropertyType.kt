package com.amgm.notebook.domain.model

/** プロパティ定義の型。 */
sealed interface PropertyType {
    data object Text : PropertyType
    data object Number : PropertyType
    data object Boolean : PropertyType

    /** ISO-8601 の LocalDate (yyyy-MM-dd)。 */
    data object Date : PropertyType
}
