package com.amgm.notebook.data.repository

import androidx.room.withTransaction
import com.amgm.notebook.data.db.NotebookDatabase
import com.amgm.notebook.data.mapper.toDefinitionEntities
import com.amgm.notebook.data.mapper.toDomain
import com.amgm.notebook.data.mapper.toEntity
import com.amgm.notebook.data.mapper.sanitizeLinkTargets
import com.amgm.notebook.data.mapper.toParentEntities
import com.amgm.notebook.domain.model.Tag
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.repository.TagRepository
import com.amgm.notebook.domain.rules.TagRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room による [TagRepository] 実装。保存時の検証と書き込みは1トランザクション内で行う。 */
class RoomTagRepository(
    private val database: NotebookDatabase,
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) : TagRepository {

    private fun sortByName(tags: List<Tag>): List<Tag> =
        tags.sortedWith(compareBy<Tag>({ it.name.lowercase() }, { it.id.value }))

    override fun observeTags(): Flow<List<Tag>> =
        database.tagDao().observeAll().map { list -> sortByName(sanitizeLinkTargets(list.map { it.toDomain() })) }

    // NoteLink の対象タグ存在確認のため全タグから引く（対象タグの削除にも追従する）
    override fun observeTag(id: TagId): Flow<Tag?> =
        database.tagDao().observeAll().map { list ->
            sanitizeLinkTargets(list.map { it.toDomain() }).firstOrNull { it.id == id }
        }

    override suspend fun getTag(id: TagId): Tag? =
        sanitizeLinkTargets(database.tagDao().getAll().map { it.toDomain() }).firstOrNull { it.id == id }

    override suspend fun getAllTags(): List<Tag> =
        sortByName(sanitizeLinkTargets(database.tagDao().getAll().map { it.toDomain() }))

    override suspend fun save(tag: Tag) {
        database.withTransaction {
            val dao = database.tagDao()
            val existing = dao.getAll().map { it.toDomain() }
            TagRules.validateForSave(tag, existing)
            val createdAt = dao.getById(tag.id.value)?.tag?.createdAt ?: nowMillis()
            dao.replaceTag(tag.toEntity(createdAt), tag.toParentEntities(), tag.toDefinitionEntities())
        }
    }

    override suspend fun delete(id: TagId) {
        database.tagDao().deleteById(id.value)
    }
}
