package com.amgm.notebook.domain.rules

import com.amgm.notebook.domain.error.CyclicInheritanceException
import com.amgm.notebook.domain.error.DuplicateTagNameException
import com.amgm.notebook.domain.error.TagNotFoundException
import com.amgm.notebook.domain.model.Tag

/** タグ保存時の整合性検査（Room 実装とフェイクで共有する）。 */
object TagRules {

    /**
     * [tag] を保存してよいか検査する。順序は 親の存在 → 循環 → 名前重複。
     * @param existing 保存前の全タグ（[tag] 自身の旧版が含まれていてもよい）
     */
    fun validateForSave(tag: Tag, existing: Collection<Tag>) {
        val knownIds = existing.map { it.id }.toSet()
        val missing = tag.parentIds
            .sortedBy { it.value }
            .firstOrNull { it != tag.id && it !in knownIds }
        if (missing != null) throw TagNotFoundException(missing)

        val hierarchy = TagHierarchy(existing.associateBy { it.id })
        val path = hierarchy.findCyclePath(tag.id, tag.parentIds)
        if (path != null) throw CyclicInheritanceException(tag.id, path)

        val key = normalizeName(tag.name)
        if (existing.any { it.id != tag.id && normalizeName(it.name) == key }) {
            throw DuplicateTagNameException(tag.name)
        }
    }

    /** タグ名の一意性判定用キー（trim + 小文字化）。 */
    fun normalizeName(name: String): String = name.trim().lowercase()
}
