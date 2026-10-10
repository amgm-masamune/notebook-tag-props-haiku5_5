package com.amgm.notebook.data.mapper

import com.amgm.notebook.data.db.entity.NoteEntity
import com.amgm.notebook.data.db.entity.NotePropertyEntity
import com.amgm.notebook.data.db.entity.NoteTagEntity
import com.amgm.notebook.data.db.entity.NoteWithRelations
import com.amgm.notebook.data.db.entity.TagEntity
import com.amgm.notebook.data.db.entity.TagParentEntity
import com.amgm.notebook.data.db.entity.TagPropertyDefinitionEntity
import com.amgm.notebook.data.db.entity.TagWithRelations
import com.amgm.notebook.domain.model.Note
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.PropertyDefinition
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.model.Tag
import com.amgm.notebook.domain.model.TagId
import java.time.Instant

/** Room の Entity とドメインモデルの相互変換（Room ランタイムに依存しない純関数）。 */

fun NoteWithRelations.toDomain(): Note {
    val properties = LinkedHashMap<PropertyName, PropertyValue>()
    for (entity in this.properties) {
        if (entity.name.isEmpty() || entity.name != entity.name.trim()) continue
        properties[PropertyName(entity.name)] = PropertyValueCodec.decode(entity.valueType, entity.valueText)
    }
    return Note(
        id = NoteId(note.id),
        title = note.title,
        body = note.body,
        tagIds = tagLinks.map { TagId(it.tagId) }.toSet(),
        properties = properties,
        createdAt = Instant.ofEpochMilli(note.createdAt),
        updatedAt = Instant.ofEpochMilli(note.updatedAt)
    )
}

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id.value,
    title = title,
    body = body,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli()
)

fun Note.toTagLinkEntities(): List<NoteTagEntity> =
    tagIds.map { NoteTagEntity(noteId = id.value, tagId = it.value) }

fun Note.toPropertyEntities(): List<NotePropertyEntity> =
    properties.map { (name, value) ->
        val (valueType, valueText) = PropertyValueCodec.encode(value)
        NotePropertyEntity(noteId = id.value, name = name.value, valueType = valueType, valueText = valueText)
    }

fun TagWithRelations.toDomain(): Tag {
    val definitions = this.definitions
        .sortedBy { it.position }
        .map { entity ->
            PropertyDefinition(
                name = PropertyName(entity.name),
                type = PropertyValueCodec.parseType(entity.type) ?: PropertyType.Text,
                required = entity.required
            )
        }
    return Tag(
        id = TagId(tag.id),
        name = tag.name,
        parentIds = parentLinks.map { TagId(it.parentId) }.toSet(),
        ownPropertyDefinitions = definitions
    )
}

fun Tag.toEntity(createdAt: Long): TagEntity = TagEntity(id = id.value, name = name, createdAt = createdAt)

fun Tag.toParentEntities(): List<TagParentEntity> =
    parentIds.map { TagParentEntity(tagId = id.value, parentId = it.value) }

fun Tag.toDefinitionEntities(): List<TagPropertyDefinitionEntity> =
    ownPropertyDefinitions.mapIndexed { index, definition ->
        TagPropertyDefinitionEntity(
            tagId = id.value,
            name = definition.name.value,
            type = PropertyValueCodec.typeName(definition.type),
            required = definition.required,
            position = index
        )
    }
