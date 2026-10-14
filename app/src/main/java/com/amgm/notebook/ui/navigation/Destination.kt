package com.amgm.notebook.ui.navigation

import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.TagId

/** 遷移先。ViewModel は [route] を直接扱わず、この型を UiEvent で発行する。 */
sealed interface Destination {
    val route: String

    data object NoteList : Destination {
        override val route: String get() = Routes.NOTE_LIST
    }

    data class NoteEdit(val noteId: NoteId?) : Destination {
        override val route: String get() = Routes.noteEdit(noteId)
    }

    data object TagList : Destination {
        override val route: String get() = Routes.TAG_LIST
    }

    data class TagEdit(val tagId: TagId?) : Destination {
        override val route: String get() = Routes.tagEdit(tagId)
    }

    data class TagTable(val tagId: TagId) : Destination {
        override val route: String get() = Routes.tagTable(tagId)
    }
}
