package com.amgm.notebook.domain.model

/**
 * タグ（型）。複数の親を持てる（継承）。
 * 自己参照・循環はリポジトリの保存時に拒否されるため、ここでは検査しない。
 */
data class Tag(
    val id: TagId,
    val name: String,
    val parentIds: Set<TagId>,
    val ownPropertyDefinitions: List<PropertyDefinition>
) {
    init {
        require(name.isNotBlank()) { "タグ名は空にできません" }
        val names = ownPropertyDefinitions.map { it.name }
        require(names.size == names.toSet().size) { "同一タグ内でプロパティ名が重複しています" }
    }
}
