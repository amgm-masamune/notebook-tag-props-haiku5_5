package com.amgm.notebook.domain.rules

import com.amgm.notebook.domain.model.Note
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.model.Tag
import com.amgm.notebook.domain.model.TagId

/*
 * タグの付け外しに伴うノートのプロパティ更新規則（純粋ロジック）。
 *
 * - タグを付けただけではプロパティ値は [PropertyValue.Null]（未入力）。
 * - タグを外した時、外したタグ由来のプロパティは、値が Null なら削除、それ以外（空文字列の Text を含む）は
 *   自由プロパティとして残す。残ったタグが同名を定義していればそちらを優先し、判定の対象外とする。
 */
/**
 * タグ追加後のプロパティ一覧を返す。
 *
 * @param note 追加前のノート（[Note.properties] が基準。既存の値は変更しない）
 * @param allTagsAfter 追加後の状態で解決に使う全タグ
 * @param addedTagIds 新たに付いたタグ。それらの実効定義のうち未登録の名前を Null で追加する
 */
fun onTagsAdded(
    note: Note,
    allTagsAfter: Map<TagId, Tag>,
    addedTagIds: Set<TagId>
): Map<PropertyName, PropertyValue> {
    if (addedTagIds.isEmpty()) return note.properties
    val schema = EffectivePropertyResolver(allTagsAfter).resolveForTags(addedTagIds)
    val result = LinkedHashMap(note.properties)
    for (effective in schema.definitions) {
        val name = effective.definition.name
        if (name !in result) result[name] = PropertyValue.Null
    }
    return result
}

/**
 * タグ取り外し後のプロパティ一覧を返す。
 *
 * @param properties 取り外し前のノートのプロパティ
 * @param remainingTagIds 取り外し後に残るタグ
 * @param removedTagIds 取り外すタグ
 * @param allTags 取り外し前のタグ集合を含む全タグ
 */
fun onTagsRemoved(
    properties: Map<PropertyName, PropertyValue>,
    remainingTagIds: Set<TagId>,
    removedTagIds: Set<TagId>,
    allTags: Map<TagId, Tag>
): Map<PropertyName, PropertyValue> {
    if (removedTagIds.isEmpty()) return properties
    val resolver = EffectivePropertyResolver(allTags)
    val before = resolver.resolveForTags(remainingTagIds + removedTagIds)
    val after = resolver.resolveForTags(remainingTagIds)

    // 残ったタグが（衝突中も含め）扱っている名前は触らない
    val keptNames = HashSet<PropertyName>()
    after.definitions.mapTo(keptNames) { it.definition.name }
    after.conflicts.mapTo(keptNames) { it.name }

    val removedOnlyNames = before.definitions
        .map { it.definition.name }
        .filter { it !in keptNames }
        .toSet()

    val result = LinkedHashMap<PropertyName, PropertyValue>()
    for ((name, value) in properties) {
        if (name in removedOnlyNames && value is PropertyValue.Null) continue
        result[name] = value
    }
    return result
}
