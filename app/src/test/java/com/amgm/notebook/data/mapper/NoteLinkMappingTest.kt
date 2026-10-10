package com.amgm.notebook.data.mapper

import com.amgm.notebook.data.db.entity.NoteEntity
import com.amgm.notebook.data.db.entity.NoteWithRelations
import com.amgm.notebook.data.db.entity.TagEntity
import com.amgm.notebook.data.db.entity.TagPropertyDefinitionEntity
import com.amgm.notebook.data.db.entity.TagWithRelations
import com.amgm.notebook.domain.defOf
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.noteOf
import com.amgm.notebook.domain.tagOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class NoteLinkMappingTest {

    private fun roundTrip(value: PropertyValue): PropertyValue {
        val (type, text) = PropertyValueCodec.encode(value)
        return PropertyValueCodec.decode(type, text)
    }

    @Test
    fun `NoteRefsは空も複数も往復する`() {
        val empty = PropertyValue.NoteRefs(emptyList())
        val many = PropertyValue.NoteRefs(listOf(NoteId("b-2"), NoteId("a-1"), NoteId("c-3")))
        assertEquals(empty, roundTrip(empty))
        assertEquals(many, roundTrip(many))
    }

    @Test
    fun `NoteRefsはNOTE_REFSとカンマ区切りで符号化される`() {
        val value = PropertyValue.NoteRefs(listOf(NoteId("x"), NoteId("y")))
        assertEquals("NOTE_REFS" to "x,y", PropertyValueCodec.encode(value))
        assertEquals("NOTE_REFS" to "", PropertyValueCodec.encode(PropertyValue.NoteRefs(emptyList())))
    }

    @Test
    fun `復元時は重複と空要素を除く`() {
        assertEquals(
            PropertyValue.NoteRefs(listOf(NoteId("a"), NoteId("b"))),
            PropertyValueCodec.decode("NOTE_REFS", "a,,b,a")
        )
    }

    @Test
    fun `NoteLink型の名前と対象タグ列`() {
        assertEquals("NOTE_LINK", PropertyValueCodec.typeName(PropertyType.NoteLink(TagId("t"))))
        assertEquals("t", PropertyValueCodec.linkTargetOf(PropertyType.NoteLink(TagId("t"))))
        assertNull(PropertyValueCodec.linkTargetOf(PropertyType.NoteLink(null)))
        assertNull(PropertyValueCodec.linkTargetOf(PropertyType.Text))
        assertEquals(PropertyType.NoteLink(TagId("t")), PropertyValueCodec.parseType("NOTE_LINK", "t"))
        assertEquals(PropertyType.NoteLink(null), PropertyValueCodec.parseType("NOTE_LINK", null))
        assertEquals(PropertyType.NoteLink(null), PropertyValueCodec.parseType("NOTE_LINK", " "))
    }

    private fun roundTripTag(tag: com.amgm.notebook.domain.model.Tag) = TagWithRelations(
        tag = tag.toEntity(0L),
        parentLinks = tag.toParentEntities(),
        definitions = tag.toDefinitionEntities()
    ).toDomain()

    @Test
    fun `NoteLink定義は対象タグありなしでEntityを経由して往復する`() {
        val tag = tagOf(
            "t",
            defs = listOf(
                defOf("with", PropertyType.NoteLink(TagId("target"))),
                defOf("without", PropertyType.NoteLink(null), required = true)
            )
        )
        val entities = tag.toDefinitionEntities()
        assertEquals("NOTE_LINK", entities[0].type)
        assertEquals("target", entities[0].linkTargetTagId)
        assertNull(entities[1].linkTargetTagId)
        assertEquals(tag, roundTripTag(tag))
    }

    @Test
    fun `存在しない対象タグはsanitizeでnullになり存在すれば保たれる`() {
        val target = tagOf("target")
        val holder = tagOf(
            "holder",
            defs = listOf(
                defOf("ok", PropertyType.NoteLink(TagId("target"))),
                defOf("dangling", PropertyType.NoteLink(TagId("gone"))),
                defOf("plain", PropertyType.Text)
            )
        )
        val result = sanitizeLinkTargets(listOf(target, holder)).single { it.id.value == "holder" }
        assertEquals(
            listOf(PropertyType.NoteLink(TagId("target")), PropertyType.NoteLink(null), PropertyType.Text),
            result.ownPropertyDefinitions.map { it.type }
        )
    }

    @Test
    fun `NoteLinkのEntity定義の対象タグ列は省略可能`() {
        val entity = TagPropertyDefinitionEntity("t", "a", "NOTE_LINK", false, 0)
        assertNull(entity.linkTargetTagId)
        val restored = TagWithRelations(TagEntity("t", "T", 0L), emptyList(), listOf(entity)).toDomain()
        assertEquals(PropertyType.NoteLink(null), restored.ownPropertyDefinitions.single().type)
    }

    @Test
    fun `autoSyncTitleH1は規定ONでEntityを経由して往復する`() {
        assertEquals(true, noteOf("n").autoSyncTitleH1)
        assertEquals(true, NoteEntity("n", "t", "b", 0L, 0L).autoSyncTitleH1)
        val off = noteOf("n").copy(autoSyncTitleH1 = false)
        val entity = off.toEntity()
        assertFalse(entity.autoSyncTitleH1)
        val restored = NoteWithRelations(entity, off.toTagLinkEntities(), off.toPropertyEntities()).toDomain()
        assertEquals(off, restored)
    }

    @Test
    fun `NoteRefsを持つノートがEntityを経由して往復する`() {
        val note = noteOf("n", props = mapOf("refs" to PropertyValue.NoteRefs(listOf(NoteId("a"), NoteId("b")))))
        val restored = NoteWithRelations(note.toEntity(), note.toTagLinkEntities(), note.toPropertyEntities()).toDomain()
        assertEquals(note, restored)
    }
}
