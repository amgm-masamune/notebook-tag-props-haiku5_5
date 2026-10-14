package com.amgm.notebook.testing

import com.amgm.notebook.domain.error.TagNotFoundException
import com.amgm.notebook.domain.model.Note
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * [NoteRepository] のインメモリ実装（テスト用フェイク）。
 * 未知の TagId の検査とタグ削除時のリンク除去のため [tagRepository] を参照する。
 * ノートのプロパティ値はタグ削除後も残る（自由プロパティ化）。
 */
class InMemoryNoteRepository(
    private val tagRepository: InMemoryTagRepository,
    initial: Collection<Note> = emptyList()
) : NoteRepository {

    private val state = MutableStateFlow<Map<NoteId, Note>>(initial.associateBy { it.id })
    private val lock = Any()

    init {
        tagRepository.addDeletionListener { removed -> removeTagFromNotes(removed) }
    }

    /** 現在の全ノート（スナップショット）。 */
    val snapshot: Map<NoteId, Note> get() = state.value

    private fun removeTagFromNotes(tagId: TagId) {
        synchronized(lock) {
            val updated = LinkedHashMap<NoteId, Note>()
            for ((key, note) in state.value) {
                updated[key] = if (tagId in note.tagIds) note.copy(tagIds = note.tagIds - tagId) else note
            }
            state.value = updated
        }
    }

    private fun sortedByUpdated(notes: Collection<Note>): List<Note> =
        notes.sortedByDescending { it.updatedAt }

    override fun observeNotes(query: String): Flow<List<Note>> = state.map { map ->
        sortedByUpdated(
            map.values.filter {
                query.isEmpty() ||
                    it.title.contains(query, ignoreCase = true) ||
                    it.body.contains(query, ignoreCase = true)
            }
        )
    }

    override fun observeNote(id: NoteId): Flow<Note?> = state.map { it[id] }

    override suspend fun getNote(id: NoteId): Note? = state.value[id]

    override fun observeNotesByTagIds(tagIds: Set<TagId>): Flow<List<Note>> {
        if (tagIds.isEmpty()) return flowOf(emptyList())
        return state.map { map -> sortedByUpdated(map.values.filter { note -> note.tagIds.any { it in tagIds } }) }
    }

    override suspend fun save(note: Note) {
        synchronized(lock) {
            val known = tagRepository.snapshot
            val missing = note.tagIds.sortedBy { it.value }.firstOrNull { it !in known }
            if (missing != null) throw TagNotFoundException(missing)
            state.value = state.value + (note.id to note)
        }
    }

    override suspend fun delete(id: NoteId) {
        synchronized(lock) {
            if (id in state.value) state.value = state.value - id
        }
    }
}
