package com.amgm.notebook.domain.model

/** タグが宣言するプロパティ定義。 */
data class PropertyDefinition(
    val name: PropertyName,
    val type: PropertyType,
    val required: Boolean
)
