package com.amgm.notebook.testing

import com.amgm.notebook.domain.model.Tag
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.repository.TagRepository
import com.amgm.notebook.domain.rules.TagRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * [TagRepository] のインメモリ実装（テスト用フェイク）。
 * 保存時の検証は本物（Room 実装）と同じ [TagRules] を使う。
 * [InMemoryNoteRepository] と連動させるには同じインスタンスを渡す。
 */
class InMemoryTagRepository(initial: Collection<Tag> = emptyList()) : TagRepository {

    private val state = MutableStateFlow<Map<TagId, Tag>>(initial.associateBy { it.id })
    private val lock = Any()
    private val deletionListeners = mutableListOf<(TagId) -> Unit>()

    /** 現在の全タグ（スナップショット）。 */
    val snapshot: Map<TagId, Tag> get() = state.value

    /** 検証を通さずに直接書き込む（循環などの異常状態をテストで仕込む用途）。 */
    fun putUnchecked(tag: Tag) {
        synchronized(lock) { state.value = state.value + (tag.id to tag) }
    }

    /** タグ削除時に呼ばれるリスナーを登録する（ノート側のリンク除去用）。 */
    fun addDeletionListener(listener: (TagId) -> Unit) {
        synchronized(lock) { deletionListeners.add(listener) }
    }

    private fun sorted(tags: Collection<Tag>): List<Tag> =
        tags.sortedWith(compareBy<Tag>({ it.name.lowercase() }, { it.id.value }))

    override fun observeTags(): Flow<List<Tag>> = state.map { sorted(it.values) }

    override fun observeTag(id: TagId): Flow<Tag?> = state.map { it[id] }

    override suspend fun getTag(id: TagId): Tag? = state.value[id]

    override suspend fun getAllTags(): List<Tag> = sorted(state.value.values)

    override suspend fun save(tag: Tag) {
        synchronized(lock) {
            TagRules.validateForSave(tag, state.value.values)
            state.value = state.value + (tag.id to tag)
        }
    }

    override suspend fun delete(id: TagId) {
        val listeners = synchronized(lock) {
            val current = state.value
            if (id !in current) return
            val updated = LinkedHashMap<TagId, Tag>()
            for ((key, value) in current) {
                if (key == id) continue
                updated[key] = if (id in value.parentIds) value.copy(parentIds = value.parentIds - id) else value
            }
            state.value = updated
            deletionListeners.toList()
        }
        listeners.forEach { it(id) }
    }
}
