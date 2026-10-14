package com.amgm.notebook.domain.repository

import com.amgm.notebook.domain.model.Note
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.TagId
import kotlinx.coroutines.flow.Flow

/** ノートの永続化。検証は行わない（ViewModel の責務）。 */
interface NoteRepository {
    /** updatedAt 降順。query は title/body の部分一致（大文字小文字無視）。空文字は全件。 */
    fun observeNotes(query: String = ""): Flow<List<Note>>

    fun observeNote(id: NoteId): Flow<Note?>

    suspend fun getNote(id: NoteId): Note?

    /** いずれかのタグが付いたノート（updatedAt 降順）。空集合なら空リスト。 */
    fun observeNotesByTagIds(tagIds: Set<TagId>): Flow<List<Note>>

    /** 新規/更新(upsert)。未知の TagId は [com.amgm.notebook.domain.error.TagNotFoundException]。 */
    suspend fun save(note: Note)

    /** 存在しなくても何もしない。 */
    suspend fun delete(id: NoteId)
}
