package com.amgm.notebook.domain

import com.amgm.notebook.domain.model.Note
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.PropertyDefinition
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.model.Tag
import com.amgm.notebook.domain.model.TagId
import java.time.Instant

/** ドメイン層テスト用の簡易ビルダ（ID と名前を同じ文字列にする）。 */
internal fun defOf(
    name: String,
    type: PropertyType = PropertyType.Text,
    required: Boolean = false
): PropertyDefinition = PropertyDefinition(PropertyName(name), type, required)

internal fun tagOf(
    id: String,
    parents: List<String> = emptyList(),
    defs: List<PropertyDefinition> = emptyList(),
    name: String = id
): Tag = Tag(TagId(id), name, parents.map { TagId(it) }.toSet(), defs)

internal fun noteOf(
    id: String,
    tags: List<String> = emptyList(),
    props: Map<String, PropertyValue> = emptyMap(),
    title: String = id,
    updated: Long = 0L
): Note = Note(
    id = NoteId(id),
    title = title,
    body = "",
    tagIds = tags.map { TagId(it) }.toSet(),
    properties = props.entries.associate { PropertyName(it.key) to it.value },
    createdAt = Instant.ofEpochMilli(0L),
    updatedAt = Instant.ofEpochMilli(updated)
)

internal fun tagMap(vararg tags: Tag): Map<TagId, Tag> = tags.associateBy { it.id }
