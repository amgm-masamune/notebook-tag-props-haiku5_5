package com.amgm.notebook.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amgm.notebook.data.db.NotebookDatabase
import com.amgm.notebook.data.repository.RoomNoteRepository
import com.amgm.notebook.data.repository.RoomTagRepository
import com.amgm.notebook.domain.repository.NoteRepository
import com.amgm.notebook.domain.repository.NoteRepositoryContract
import com.amgm.notebook.domain.repository.TagRepository
import com.amgm.notebook.domain.repository.TagRepositoryContract
import org.junit.After
import org.junit.runner.RunWith

private fun newInMemoryDatabase(): NotebookDatabase =
    NotebookDatabase.inMemory(InstrumentationRegistry.getInstrumentation().targetContext)

/** RoomNoteRepository が NoteRepository 契約を満たすことを検証する。 */
@RunWith(AndroidJUnit4::class)
class RoomNoteRepositoryContractTest : NoteRepositoryContract() {
    private var database: NotebookDatabase? = null

    override fun createRepositories(): Pair<NoteRepository, TagRepository> {
        val db = newInMemoryDatabase()
        database = db
        return RoomNoteRepository(db) to RoomTagRepository(db)
    }

    @After
    fun tearDown() {
        database?.close()
    }
}

/** RoomTagRepository が TagRepository 契約を満たすことを検証する。 */
@RunWith(AndroidJUnit4::class)
class RoomTagRepositoryContractTest : TagRepositoryContract() {
    private var database: NotebookDatabase? = null

    override fun createRepositories(): Pair<NoteRepository, TagRepository> {
        val db = newInMemoryDatabase()
        database = db
        return RoomNoteRepository(db) to RoomTagRepository(db)
    }

    @After
    fun tearDown() {
        database?.close()
    }
}
