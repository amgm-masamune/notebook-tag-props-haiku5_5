package com.amgm.notebook.domain.repository

import com.amgm.notebook.domain.model.Tag
import com.amgm.notebook.domain.model.TagId
import kotlinx.coroutines.flow.Flow

/** タグの永続化。 */
interface TagRepository {
    /** 名前の昇順（大文字小文字無視）。 */
    fun observeTags(): Flow<List<Tag>>

    fun observeTag(id: TagId): Flow<Tag?>

    suspend fun getTag(id: TagId): Tag?

    suspend fun getAllTags(): List<Tag>

    /**
     * 保存(upsert)。
     * @throws com.amgm.notebook.domain.error.TagNotFoundException 親が存在しない
     * @throws com.amgm.notebook.domain.error.CyclicInheritanceException 自己参照・循環
     * @throws com.amgm.notebook.domain.error.DuplicateTagNameException 名前重複
     */
    suspend fun save(tag: Tag)

    /** 削除。子の parentIds から除去、ノートのタグリンクも除去。存在しなくても何もしない。 */
    suspend fun delete(id: TagId)
}
