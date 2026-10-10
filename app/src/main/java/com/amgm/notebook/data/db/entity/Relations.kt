package com.amgm.notebook.data.db.entity

import androidx.room.Embedded
import androidx.room.Relation

/** ノート本体と、そのタグリンク・プロパティ。 */
data class NoteWithRelations(
    @Embedded val note: NoteEntity,
    @Relation(parentColumn = "id", entityColumn = "note_id")
    val tagLinks: List<NoteTagEntity>,
    @Relation(parentColumn = "id", entityColumn = "note_id")
    val properties: List<NotePropertyEntity>
)

/** タグ本体と、その親リンク・プロパティ定義。 */
data class TagWithRelations(
    @Embedded val tag: TagEntity,
    @Relation(parentColumn = "id", entityColumn = "tag_id")
    val parentLinks: List<TagParentEntity>,
    @Relation(parentColumn = "id", entityColumn = "tag_id")
    val definitions: List<TagPropertyDefinitionEntity>
)
