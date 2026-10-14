package com.amgm.notebook.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.amgm.notebook.di.AppContainer
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.ui.noteedit.NoteEditViewModel
import com.amgm.notebook.ui.notelist.NoteListViewModel
import com.amgm.notebook.ui.tagedit.TagEditViewModel
import com.amgm.notebook.ui.taglist.TagListViewModel
import com.amgm.notebook.ui.tagtable.TagTableViewModel

private fun factoryOf(block: () -> ViewModel): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = block() as T
    }

fun noteListViewModelFactory(container: AppContainer): ViewModelProvider.Factory =
    factoryOf { NoteListViewModel(container.noteRepository, container.tagRepository) }

fun noteEditViewModelFactory(
    container: AppContainer,
    noteId: NoteId?,
    initialTagId: TagId? = null
): ViewModelProvider.Factory =
    factoryOf {
        NoteEditViewModel(
            noteId = noteId,
            noteRepository = container.noteRepository,
            tagRepository = container.tagRepository,
            idGenerator = container.idGenerator,
            clock = container.clock,
            initialTagId = initialTagId
        )
    }

fun tagListViewModelFactory(container: AppContainer): ViewModelProvider.Factory =
    factoryOf { TagListViewModel(container.tagRepository) }

fun tagEditViewModelFactory(container: AppContainer, tagId: TagId?): ViewModelProvider.Factory =
    factoryOf { TagEditViewModel(tagId, container.tagRepository, container.idGenerator) }

fun tagTableViewModelFactory(container: AppContainer, tagId: TagId): ViewModelProvider.Factory =
    factoryOf { TagTableViewModel(tagId, container.tagRepository, container.noteRepository) }
