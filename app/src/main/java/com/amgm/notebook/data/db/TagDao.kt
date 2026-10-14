package com.amgm.notebook.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.amgm.notebook.data.db.entity.TagEntity
import com.amgm.notebook.data.db.entity.TagParentEntity
import com.amgm.notebook.data.db.entity.TagPropertyDefinitionEntity
import com.amgm.notebook.data.db.entity.TagWithRelations
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TagDao {

    @Transaction
    @Query("SELECT * FROM tags")
    abstract fun observeAll(): Flow<List<TagWithRelations>>

    @Transaction
    @Query("SELECT * FROM tags WHERE id = :id")
    abstract fun observeById(id: String): Flow<TagWithRelations?>

    @Transaction
    @Query("SELECT * FROM tags WHERE id = :id")
    abstract suspend fun getById(id: String): TagWithRelations?

    @Transaction
    @Query("SELECT * FROM tags")
    abstract suspend fun getAll(): List<TagWithRelations>

    @Query("SELECT id FROM tags WHERE id IN (:ids)")
    abstract suspend fun findExistingIds(ids: List<String>): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertTag(tag: TagEntity): Long

    @Update
    abstract suspend fun updateTag(tag: TagEntity)

    @Query("DELETE FROM tag_parents WHERE tag_id = :tagId")
    abstract suspend fun deleteParentLinks(tagId: String)

    @Query("DELETE FROM tag_property_definitions WHERE tag_id = :tagId")
    abstract suspend fun deleteDefinitions(tagId: String)

    @Insert
    abstract suspend fun insertParentLinks(links: List<TagParentEntity>)

    @Insert
    abstract suspend fun insertDefinitions(definitions: List<TagPropertyDefinitionEntity>)

    /** 削除。外部キーの CASCADE で親リンク・ノートのタグリンク・定義も消える。 */
    @Query("DELETE FROM tags WHERE id = :id")
    abstract suspend fun deleteById(id: String)

    /** tags upsert → tag_parents 入替 → tag_property_definitions 入替 を1トランザクションで行う。 */
    @Transaction
    open suspend fun replaceTag(
        tag: TagEntity,
        parentLinks: List<TagParentEntity>,
        definitions: List<TagPropertyDefinitionEntity>
    ) {
        if (insertTag(tag) == -1L) {
            updateTag(tag)
        }
        deleteParentLinks(tag.id)
        deleteDefinitions(tag.id)
        insertParentLinks(parentLinks)
        insertDefinitions(definitions)
    }
}
