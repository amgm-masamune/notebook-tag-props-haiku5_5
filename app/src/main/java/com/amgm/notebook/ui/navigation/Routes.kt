package com.amgm.notebook.ui.navigation

import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.TagId
import java.net.URLEncoder

/** ルート文字列（純Kotlin。JUnit から検証可能）。 */
object Routes {
    const val ARG_NOTE_ID = "noteId"
    const val ARG_TAG_ID = "tagId"

    const val NOTE_LIST = "note_list"
    const val NOTE_EDIT = "note_edit?noteId={noteId}&tagId={tagId}"
    const val TAG_LIST = "tag_list"
    const val TAG_EDIT = "tag_edit?tagId={tagId}"
    const val TAG_TABLE = "tag_table/{tagId}"
    /** 表タブ（タグごとの表への入口）。 */
    const val TABLES = "tables"

    /** ボトムナビゲーションを表示するトップレベルのルート。 */
    val TOP_LEVEL: List<String> = listOf(NOTE_LIST, TAG_LIST, TABLES)

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    /**
     * ノート編集。[id] が null なら新規。[initialTagId] は新規作成時に初期選択するタグ（省略可）。
     * 例: `note_edit` / `note_edit?noteId=a` / `note_edit?tagId=t` / `note_edit?noteId=a&tagId=t`
     */
    fun noteEdit(id: NoteId?, initialTagId: TagId? = null): String {
        val query = buildList {
            if (id != null) add("noteId=${encode(id.value)}")
            if (initialTagId != null) add("tagId=${encode(initialTagId.value)}")
        }
        return if (query.isEmpty()) "note_edit" else "note_edit?" + query.joinToString("&")
    }

    fun tagEdit(id: TagId?): String =
        if (id == null) "tag_edit" else "tag_edit?tagId=${encode(id.value)}"

    fun tagTable(id: TagId): String = "tag_table/${encode(id.value)}"
}
