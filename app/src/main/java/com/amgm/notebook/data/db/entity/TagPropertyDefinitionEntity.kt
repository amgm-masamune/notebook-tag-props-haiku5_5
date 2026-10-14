package com.amgm.notebook.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "tag_property_definitions",
    primaryKeys = ["tag_id", "name"],
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TagPropertyDefinitionEntity(
    @ColumnInfo(name = "tag_id") val tagId: String,
    @ColumnInfo(name = "name") val name: String,
    /** TEXT / NUMBER / BOOLEAN / DATE / NOTE_LINK */
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "required") val required: Boolean,
    /** 定義順（0始まり） */
    @ColumnInfo(name = "position") val position: Int,
    /** NOTE_LINK の既定絞り込みタグID。外部キーは張らない（削除済みは読み出し時に null 扱い）。 */
    @ColumnInfo(name = "link_target_tag_id") val linkTargetTagId: String? = null
)
