package com.amgm.notebook.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.amgm.notebook.data.db.entity.NoteEntity
import com.amgm.notebook.data.db.entity.NotePropertyEntity
import com.amgm.notebook.data.db.entity.NoteTagEntity
import com.amgm.notebook.data.db.entity.NoteWithRelations
import kotlinx.coroutines.flow.Flow

@Dao
abstract class NoteDao {

    @Transaction
    @Query("SELECT * FROM notes ORDER BY updated_at DESC")
    abstract fun observeAll(): Flow<List<NoteWithRelations>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    abstract fun observeById(id: String): Flow<NoteWithRelations?>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    abstract suspend fun getById(id: String): NoteWithRelations?

    @Transaction
    @Query(
        "SELECT DISTINCT n.* FROM notes AS n " +
            "INNER JOIN note_tags AS nt ON nt.note_id = n.id " +
            "WHERE nt.tag_id IN (:tagIds) ORDER BY n.updated_at DESC"
    )
    abstract fun observeByTagIds(tagIds: List<String>): Flow<List<NoteWithRelations>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertNote(note: NoteEntity): Long

    @Update
    abstract suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM note_tags WHERE note_id = :noteId")
    abstract suspend fun deleteNoteTags(noteId: String)

    @Query("DELETE FROM note_properties WHERE note_id = :noteId")
    abstract suspend fun deleteNoteProperties(noteId: String)

    @Insert
    abstract suspend fun insertNoteTags(links: List<NoteTagEntity>)

    @Insert
    abstract suspend fun insertNoteProperties(properties: List<NotePropertyEntity>)

    @Query("DELETE FROM notes WHERE id = :id")
    abstract suspend fun deleteById(id: String)

    /** ノート本体・タグリンク・プロパティを1トランザクションで入れ替える（upsert）。 */
    @Transaction
    open suspend fun replaceNote(
        note: NoteEntity,
        tagLinks: List<NoteTagEntity>,
        properties: List<NotePropertyEntity>
    ) {
        if (insertNote(note) == -1L) {
            updateNote(note)
        }
        deleteNoteTags(note.id)
        deleteNoteProperties(note.id)
        insertNoteTags(tagLinks)
        insertNoteProperties(properties)
    }
}
