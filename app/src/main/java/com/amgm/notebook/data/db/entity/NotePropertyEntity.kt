package com.amgm.notebook.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "note_properties",
    primaryKeys = ["note_id", "name"],
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NotePropertyEntity(
    @ColumnInfo(name = "note_id") val noteId: String,
    @ColumnInfo(name = "name") val name: String,
    /** TEXT / NUMBER / BOOLEAN / DATE / EMPTY */
    @ColumnInfo(name = "value_type") val valueType: String,
    @ColumnInfo(name = "value_text") val valueText: String
)
