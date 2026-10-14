package com.amgm.notebook.domain.service

import com.amgm.notebook.domain.model.Tag
import com.amgm.notebook.domain.model.TagId

/**
 * タグ一覧のツリー表示用の1行。
 * [isAdditionalParentOccurrence] は、複数親のタグの2回目以降の出現で true。
 */
data class TagTreeNode(val tag: Tag, val depth: Int, val isAdditionalParentOccurrence: Boolean)

/**
 * タグを親子関係に沿って深さ優先で平坦化する（表示用の純粋関数）。
 * - 根: tags 内に存在する親を持たないタグ。名前昇順（大文字小文字無視、同名は ID 順）。
 * - 子: そのタグを親に持つタグ。同じ順序で並べる。複数親のタグは親ごとに出現する。
 * - 欠落した親（tags に無い）は無視する。他に親が無ければ根になる。
 * - 循環データでも例外や無限ループは起こさない。経路上に既出のタグは出現させない。
 *   どの根からも到達できない循環のみのタグは、名前順で根として補って出す。
 */
fun flattenTagTree(tags: Collection<Tag>): List<TagTreeNode> {
    val byId = LinkedHashMap<TagId, Tag>()
    for (tag in tags) byId[tag.id] = tag

    val order = compareBy<Tag>({ it.name.lowercase() }, { it.id.value })
    val childrenByParent = HashMap<TagId, MutableList<Tag>>()
    for (tag in byId.values) {
        for (parentId in tag.parentIds) {
            if (parentId == tag.id || parentId !in byId) continue
            childrenByParent.getOrPut(parentId) { mutableListOf() }.add(tag)
        }
    }
    for (list in childrenByParent.values) list.sortWith(order)

    val result = ArrayList<TagTreeNode>()
    val emitted = HashSet<TagId>()
    val path = LinkedHashSet<TagId>()

    fun visit(tag: Tag, depth: Int) {
        result.add(TagTreeNode(tag, depth, isAdditionalParentOccurrence = !emitted.add(tag.id)))
        path.add(tag.id)
        for (child in childrenByParent[tag.id].orEmpty()) {
            if (child.id in path) continue
            visit(child, depth + 1)
        }
        path.remove(tag.id)
    }

    val roots = byId.values
        .filter { tag -> tag.parentIds.none { it != tag.id && it in byId } }
        .sortedWith(order)
    for (root in roots) visit(root, 0)

    // 循環のみで根から到達できないタグ
    for (tag in byId.values.sortedWith(order)) {
        if (tag.id !in emitted) visit(tag, 0)
    }
    return result
}
