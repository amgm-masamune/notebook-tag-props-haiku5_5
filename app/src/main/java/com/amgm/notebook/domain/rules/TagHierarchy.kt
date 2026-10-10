package com.amgm.notebook.domain.rules

import com.amgm.notebook.domain.model.Note
import com.amgm.notebook.domain.model.Tag
import com.amgm.notebook.domain.model.TagId

/**
 * 全タグのスナップショットから作る不変の継承グラフ（DAG）。
 * スナップショット内に循環があっても無限ループしない（防御的実装）。
 */
class TagHierarchy(val tags: Map<TagId, Tag>) {

    private val childrenByParent: Map<TagId, List<TagId>> by lazy {
        val map = HashMap<TagId, MutableList<TagId>>()
        for (tag in tags.values) {
            for (parentId in tag.parentIds) {
                map.getOrPut(parentId) { mutableListOf() }.add(tag.id)
            }
        }
        map
    }

    /** 親を 名前(大文字小文字無視)→ID の昇順に並べる。スナップショットに無い親は無視。 */
    private fun sortedParents(id: TagId): List<TagId> {
        val tag = tags[id] ?: return emptyList()
        return tag.parentIds
            .mapNotNull { tags[it] }
            .sortedWith(compareBy<Tag>({ it.name.lowercase() }, { it.id.value }))
            .map { it.id }
    }

    /**
     * 全祖先（自身を含まない）を線形化順で返す。祖先は常に子孫より前に並ぶ。
     * 親を名前→ID昇順に並べ、各親について先に再帰してから親自身を出力する（後順走査）。
     */
    fun ancestorsOf(id: TagId): List<TagId> {
        val result = LinkedHashSet<TagId>()
        val visiting = HashSet<TagId>()
        visiting.add(id)

        fun visit(current: TagId) {
            for (parentId in sortedParents(current)) {
                if (parentId in result || !visiting.add(parentId)) continue
                visit(parentId)
                result.add(parentId)
            }
        }

        visit(id)
        return result.toList()
    }

    /** 全子孫（自身を含まない）の集合。 */
    fun descendantsOf(id: TagId): Set<TagId> {
        val result = LinkedHashSet<TagId>()
        val queue = ArrayDeque<TagId>()
        queue.addLast(id)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for (childId in childrenByParent[current].orEmpty()) {
                if (childId != id && result.add(childId)) {
                    queue.addLast(childId)
                }
            }
        }
        return result
    }

    /** sub == sup または sup が sub の祖先。 */
    fun isSubtypeOf(sub: TagId, sup: TagId): Boolean = sub == sup || sup in ancestorsOf(sub)

    /** ノートがタグ [tagId] の一員か（自タグまたは子孫タグを持つ）。 */
    fun isInstanceOf(note: Note, tagId: TagId): Boolean =
        note.tagIds.any { isSubtypeOf(it, tagId) }

    /**
     * [tagId] の親を [newParentIds] にすると循環するか。
     * 循環するなら [tagId] から始まり [tagId] で終わる経路を返す（自己参照は [tagId, tagId]）。
     */
    fun findCyclePath(tagId: TagId, newParentIds: Set<TagId>): List<TagId>? {
        if (tagId in newParentIds) return listOf(tagId, tagId)
        for (parentId in newParentIds.sortedBy { it.value }) {
            val path = pathToAncestor(parentId, tagId)
            if (path != null) return listOf(tagId) + path
        }
        return null
    }

    fun wouldCreateCycle(tagId: TagId, newParentIds: Set<TagId>): Boolean =
        findCyclePath(tagId, newParentIds) != null

    /** [from] から親をたどって [target] に着く経路 [from, ..., target]。無ければ null。 */
    private fun pathToAncestor(from: TagId, target: TagId): List<TagId>? {
        val visited = HashSet<TagId>()

        fun dfs(current: TagId): List<TagId>? {
            if (current == target) return listOf(current)
            if (!visited.add(current)) return null
            for (parentId in sortedParents(current)) {
                val rest = dfs(parentId)
                if (rest != null) return listOf(current) + rest
            }
            return null
        }

        return dfs(from)
    }
}
