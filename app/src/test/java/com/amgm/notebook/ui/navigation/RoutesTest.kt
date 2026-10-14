package com.amgm.notebook.ui.navigation

import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.TagId
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {

    @Test
    fun `ルート定数は設計書どおり`() {
        assertEquals("note_list", Routes.NOTE_LIST)
        assertEquals("note_edit?noteId={noteId}", Routes.NOTE_EDIT)
        assertEquals("tag_list", Routes.TAG_LIST)
        assertEquals("tag_edit?tagId={tagId}", Routes.TAG_EDIT)
        assertEquals("tag_table/{tagId}", Routes.TAG_TABLE)
        assertEquals("noteId", Routes.ARG_NOTE_ID)
        assertEquals("tagId", Routes.ARG_TAG_ID)
    }

    @Test
    fun `ルート生成関数はnullを新規扱いにする`() {
        assertEquals("note_edit", Routes.noteEdit(null))
        assertEquals("note_edit?noteId=abc", Routes.noteEdit(NoteId("abc")))
        assertEquals("tag_edit", Routes.tagEdit(null))
        assertEquals("tag_edit?tagId=t1", Routes.tagEdit(TagId("t1")))
        assertEquals("tag_table/t1", Routes.tagTable(TagId("t1")))
    }

    @Test
    fun `特殊文字を含むIDはエンコードされる`() {
        assertEquals("note_edit?noteId=a%20b%2Fc", Routes.noteEdit(NoteId("a b/c")))
    }

    @Test
    fun `Destinationのrouteは生成関数と一致する`() {
        assertEquals(Routes.NOTE_LIST, Destination.NoteList.route)
        assertEquals(Routes.noteEdit(null), Destination.NoteEdit(null).route)
        assertEquals(Routes.noteEdit(NoteId("n")), Destination.NoteEdit(NoteId("n")).route)
        assertEquals(Routes.TAG_LIST, Destination.TagList.route)
        assertEquals(Routes.tagEdit(TagId("t")), Destination.TagEdit(TagId("t")).route)
        assertEquals(Routes.tagTable(TagId("t")), Destination.TagTable(TagId("t")).route)
    }
}
