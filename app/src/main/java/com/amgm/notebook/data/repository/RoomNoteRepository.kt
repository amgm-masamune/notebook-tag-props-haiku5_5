package com.amgm.notebook.data.repository

import androidx.room.withTransaction
import com.amgm.notebook.data.db.NotebookDatabase
import com.amgm.notebook.data.mapper.toDomain
import com.amgm.notebook.data.mapper.toEntity
import com.amgm.notebook.data.mapper.toPropertyEntities
import com.amgm.notebook.data.mapper.toTagLinkEntities
import com.amgm.notebook.domain.error.TagNotFoundException
import com.amgm.notebook.domain.model.Note
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Room による [NoteRepository] 実装。 */
class RoomNoteRepository(private val database: NotebookDatabase) : NoteRepository {

    override fun observeNotes(query: String): Flow<List<Note>> =
        database.noteDao().observeAll().map { list ->
            list.map { it.toDomain() }
                .filter { query.isEmpty() || it.title.contains(query, ignoreCase = true) || it.body.contains(query, ignoreCase = true) }
        }

    override fun observeNote(id: NoteId): Flow<Note?> =
        database.noteDao().observeById(id.value).map { it?.toDomain() }

    override suspend fun getNote(id: NoteId): Note? =
        database.noteDao().getById(id.value)?.toDomain()

    override fun observeNotesByTagIds(tagIds: Set<TagId>): Flow<List<Note>> {
        if (tagIds.isEmpty()) return flowOf(emptyList())
        return database.noteDao().observeByTagIds(tagIds.map { it.value }).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun save(note: Note) {
        database.withTransaction {
            if (note.tagIds.isNotEmpty()) {
                val existing = database.tagDao().findExistingIds(note.tagIds.map { it.value }).toSet()
                val missing = note.tagIds.sortedBy { it.value }.firstOrNull { it.value !in existing }
                if (missing != null) throw TagNotFoundException(missing)
            }
            database.noteDao().replaceNote(note.toEntity(), note.toTagLinkEntities(), note.toPropertyEntities())
        }
    }

    override suspend fun delete(id: NoteId) {
        database.noteDao().deleteById(id.value)
    }
}
