package com.amgm.notebook.ui.navigation

import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.TagId
import java.net.URLEncoder

/** ルート文字列（純Kotlin。JUnit から検証可能）。 */
object Routes {
    const val ARG_NOTE_ID = "noteId"
    const val ARG_TAG_ID = "tagId"

    const val NOTE_LIST = "note_list"
    const val NOTE_EDIT = "note_edit?noteId={noteId}"
    const val TAG_LIST = "tag_list"
    const val TAG_EDIT = "tag_edit?tagId={tagId}"
    const val TAG_TABLE = "tag_table/{tagId}"

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    fun noteEdit(id: NoteId?): String =
        if (id == null) "note_edit" else "note_edit?noteId=${encode(id.value)}"

    fun tagEdit(id: TagId?): String =
        if (id == null) "tag_edit" else "tag_edit?tagId=${encode(id.value)}"

    fun tagTable(id: TagId): String = "tag_table/${encode(id.value)}"
}
