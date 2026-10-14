package com.amgm.notebook.domain.service

import com.amgm.notebook.domain.error.TagNotFoundException
import com.amgm.notebook.domain.model.Note
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.model.Tag
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.rules.EffectivePropertyResolver
import com.amgm.notebook.domain.rules.PropertyConflict
import com.amgm.notebook.domain.rules.TagHierarchy

data class TableColumn(val name: PropertyName, val type: PropertyType, val required: Boolean)

/** cells は columns と同順・同数。値が無ければ Null。Null と Text("") はどちらも UI では空欄表示（[PropertyValue.isUnset]）。 */
data class TableRow(val noteId: NoteId, val title: String, val cells: List<PropertyValue>)

data class TableView(
    val tag: Tag,
    val columns: List<TableColumn>,
    val rows: List<TableRow>,
    val conflicts: List<PropertyConflict>
)

sealed interface TableSortKey {
    data object Title : TableSortKey
    data class Property(val name: PropertyName) : TableSortKey
}

data class TableSort(val column: TableSortKey, val ascending: Boolean)

/** タグ T の一員ノートの表ビューを組み立てる純粋ロジック。 */
class TableViewBuilder {

    /**
     * @throws TagNotFoundException [tagId] が [allTags] に無い
     */
    fun build(
        tagId: TagId,
        allTags: Map<TagId, Tag>,
        notes: List<Note>,
        sort: TableSort? = null
    ): TableView {
        val tag = allTags[tagId] ?: throw TagNotFoundException(tagId)
        val hierarchy = TagHierarchy(allTags)
        val schema = EffectivePropertyResolver(hierarchy).resolve(tagId)

        val columns = schema.definitions.map {
            TableColumn(it.definition.name, it.definition.type, it.definition.required)
        }

        val memberTagIds = hierarchy.descendantsOf(tagId) + tagId
        val members = notes.filter { note -> note.tagIds.any { it in memberTagIds } }
        val sorted = members.sortedWith(comparatorFor(sort))

        val rows = sorted.map { note ->
            TableRow(
                noteId = note.id,
                title = note.title,
                cells = columns.map { column -> note.properties[column.name] ?: PropertyValue.Null }
            )
        }
        return TableView(tag, columns, rows, schema.conflicts)
    }

    private fun comparatorFor(sort: TableSort?): Comparator<Note> {
        val byUpdatedDesc = Comparator<Note> { a, b -> b.updatedAt.compareTo(a.updatedAt) }
        if (sort == null) return byUpdatedDesc

        val direction = if (sort.ascending) 1 else -1
        val primary = Comparator<Note> { a, b ->
            when (val key = sort.column) {
                is TableSortKey.Title ->
                    a.title.compareTo(b.title, ignoreCase = true) * direction
                is TableSortKey.Property -> {
                    val va = a.properties[key.name] ?: PropertyValue.Null
                    val vb = b.properties[key.name] ?: PropertyValue.Null
                    val emptyA = va is PropertyValue.Null // Text("") は空文字として通常比較
                    val emptyB = vb is PropertyValue.Null
                    when {
                        emptyA && emptyB -> 0
                        emptyA -> 1 // Null は昇順・降順どちらでも末尾
                        emptyB -> -1
                        else -> compareNonNull(va, vb) * direction
                    }
                }
            }
        }
        return primary.then(byUpdatedDesc)
    }

    /** Null 以外の値同士の比較。型が異なる場合は型の種類順。 */
    private fun compareNonNull(a: PropertyValue, b: PropertyValue): Int = when {
        a is PropertyValue.Text && b is PropertyValue.Text -> a.value.compareTo(b.value, ignoreCase = true)
        a is PropertyValue.Number && b is PropertyValue.Number -> a.value.compareTo(b.value)
        a is PropertyValue.Bool && b is PropertyValue.Bool -> a.value.compareTo(b.value)
        a is PropertyValue.Date && b is PropertyValue.Date -> a.value.compareTo(b.value)
        else -> rank(a).compareTo(rank(b))
    }

    private fun rank(value: PropertyValue): Int = when (value) {
        is PropertyValue.Text -> 0
        is PropertyValue.Number -> 1
        is PropertyValue.Bool -> 2
        is PropertyValue.Date -> 3
        is PropertyValue.Null -> 4
        is PropertyValue.NoteRefs -> 5
    }
}
