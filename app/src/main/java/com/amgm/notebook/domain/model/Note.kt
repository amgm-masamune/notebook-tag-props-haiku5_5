package com.amgm.notebook.domain.model

import java.time.Instant

/** ノート。properties は定義済み・自由プロパティの両方を含む。 */
data class Note(
    val id: NoteId,
    val title: String,
    val body: String,
    val tagIds: Set<TagId>,
    val properties: Map<PropertyName, PropertyValue>,
    val createdAt: Instant,
    val updatedAt: Instant
)
