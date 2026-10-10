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
    /** TEXT / NUMBER / BOOLEAN / DATE */
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "required") val required: Boolean,
    /** 定義順（0始まり） */
    @ColumnInfo(name = "position") val position: Int
)
