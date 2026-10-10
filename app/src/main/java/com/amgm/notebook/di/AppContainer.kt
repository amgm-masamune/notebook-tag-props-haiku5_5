package com.amgm.notebook.di

import android.content.Context
import com.amgm.notebook.data.db.NotebookDatabase
import com.amgm.notebook.data.repository.RoomNoteRepository
import com.amgm.notebook.data.repository.RoomTagRepository
import com.amgm.notebook.domain.repository.NoteRepository
import com.amgm.notebook.domain.repository.TagRepository
import com.amgm.notebook.domain.util.IdGenerator
import com.amgm.notebook.domain.util.UuidIdGenerator
import java.time.Clock

/** 手動DIのコンテナ。ViewModel の Factory はここから依存を受け取る。 */
interface AppContainer {
    val noteRepository: NoteRepository
    val tagRepository: TagRepository
    val idGenerator: IdGenerator
    val clock: Clock
}

class DefaultAppContainer(
    database: NotebookDatabase,
    override val idGenerator: IdGenerator = UuidIdGenerator(),
    override val clock: Clock = Clock.systemUTC()
) : AppContainer {
    override val noteRepository: NoteRepository = RoomNoteRepository(database)
    override val tagRepository: TagRepository = RoomTagRepository(database)

    companion object {
        fun create(context: Context): DefaultAppContainer =
            DefaultAppContainer(NotebookDatabase.create(context))
    }
}
