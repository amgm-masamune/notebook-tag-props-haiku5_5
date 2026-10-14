package com.amgm.notebook.domain.rules

import com.amgm.notebook.domain.model.PropertyDefinition
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.Tag
import com.amgm.notebook.domain.model.TagId

/** 実効プロパティ定義と、その定義元タグ。 */
data class EffectiveProperty(val definition: PropertyDefinition, val definedBy: TagId)

/** 衝突の候補（定義元タグと型）。 */
data class ConflictCandidate(val tagId: TagId, val type: PropertyType)

/** 親同士で型が異なる同名プロパティの衝突。 */
data class PropertyConflict(val name: PropertyName, val candidates: List<ConflictCandidate>)

/** 解決結果。conflicts の名前は definitions に含まれない（自由プロパティ扱い）。 */
data class EffectiveSchema(
    val definitions: List<EffectiveProperty>,
    val conflicts: List<PropertyConflict>
) {
    /** 名前で実効定義を引く。 */
    fun find(name: PropertyName): EffectiveProperty? = definitions.firstOrNull { it.definition.name == name }

    companion object {
        val EMPTY = EffectiveSchema(emptyList(), emptyList())
    }
}

/** タグの継承を解決して実効プロパティ定義を求める（data-model.md 3.4）。 */
class EffectivePropertyResolver(private val hierarchy: TagHierarchy) {

    constructor(tags: Map<TagId, Tag>) : this(TagHierarchy(tags))

    private companion object {
        /** 衝突判定で NoteLink 同士をまとめるための目印。 */
        val NOTE_LINK_KIND = Any()
    }

    private data class Origin(val tagId: TagId, val definition: PropertyDefinition)

    fun resolve(tagId: TagId): EffectiveSchema = resolveForTags(setOf(tagId))

    fun resolveForTags(tagIds: Set<TagId>): EffectiveSchema {
        val byName = LinkedHashMap<PropertyName, MutableList<Origin>>()
        for (id in scanOrder(tagIds)) {
            val tag = hierarchy.tags[id] ?: continue
            for (definition in tag.ownPropertyDefinitions) {
                byName.getOrPut(definition.name) { mutableListOf() }.add(Origin(id, definition))
            }
        }

        val definitions = mutableListOf<EffectiveProperty>()
        val conflicts = mutableListOf<PropertyConflict>()
        for ((name, origins) in byName) {
            // 子優先: より具体的(厳密な子孫)な定義が別にあるものは除く
            val minimal = origins.filter { candidate ->
                origins.none { other ->
                    other.tagId != candidate.tagId && hierarchy.isSubtypeOf(other.tagId, candidate.tagId)
                }
            }
            // NoteLink は targetTagId が違っても同じ型とみなす（衝突させない）
            val kinds = minimal.map { origin ->
                val type = origin.definition.type
                if (type is PropertyType.NoteLink) NOTE_LINK_KIND else type
            }.distinct()
            if (kinds.size == 1) {
                val mergedType: PropertyType = if (kinds.first() == NOTE_LINK_KIND) {
                    // 子優先で残った定義のうち、対象タグを持つ最初のものを採用
                    val links = minimal.map { it.definition.type }.filterIsInstance<PropertyType.NoteLink>()
                    links.firstOrNull { it.targetTagId != null } ?: links.first()
                } else {
                    minimal.first().definition.type
                }
                val merged = PropertyDefinition(
                    name = name,
                    type = mergedType,
                    required = minimal.any { it.definition.required }
                )
                definitions.add(EffectiveProperty(merged, minimal.first().tagId))
            } else {
                conflicts.add(
                    PropertyConflict(name, minimal.map { ConflictCandidate(it.tagId, it.definition.type) })
                )
            }
        }
        return EffectiveSchema(definitions, conflicts)
    }

    /** 走査順: 指定タグを 名前→ID 昇順に並べ、各タグについて 祖先(線形化順)→自身 を既出除外で連結。 */
    private fun scanOrder(tagIds: Set<TagId>): List<TagId> {
        val ordered = tagIds
            .mapNotNull { hierarchy.tags[it] }
            .sortedWith(compareBy<Tag>({ it.name.lowercase() }, { it.id.value }))
            .map { it.id }
        val seen = LinkedHashSet<TagId>()
        for (id in ordered) {
            seen.addAll(hierarchy.ancestorsOf(id))
            seen.add(id)
        }
        return seen.toList()
    }
}
