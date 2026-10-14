package com.amgm.notebook.data.mapper

import com.amgm.notebook.data.db.entity.NoteEntity
import com.amgm.notebook.data.db.entity.NotePropertyEntity
import com.amgm.notebook.data.db.entity.NoteTagEntity
import com.amgm.notebook.data.db.entity.NoteWithRelations
import com.amgm.notebook.data.db.entity.TagEntity
import com.amgm.notebook.data.db.entity.TagParentEntity
import com.amgm.notebook.data.db.entity.TagPropertyDefinitionEntity
import com.amgm.notebook.data.db.entity.TagWithRelations
import com.amgm.notebook.domain.defOf
import com.amgm.notebook.domain.model.Note
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.tagOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class EntityMappersTest {

    private fun sampleNote() = Note(
        id = NoteId("n1"),
        title = "題名",
        body = "# 本文",
        tagIds = setOf(TagId("t1"), TagId("t2")),
        properties = mapOf(
            PropertyName("price") to PropertyValue.Number(12.5),
            PropertyName("due") to PropertyValue.Date(LocalDate.of(2024, 5, 6)),
            PropertyName("memo") to PropertyValue.Text("x"),
            PropertyName("none") to PropertyValue.Null
        ),
        createdAt = Instant.ofEpochMilli(1_700_000_000_123L),
        updatedAt = Instant.ofEpochMilli(1_700_000_999_456L)
    )

    @Test
    fun `ノートがEntityを経由して往復する`() {
        val note = sampleNote()
        val restored = NoteWithRelations(
            note = note.toEntity(),
            tagLinks = note.toTagLinkEntities(),
            properties = note.toPropertyEntities()
        ).toDomain()
        assertEquals(note, restored)
    }

    @Test
    fun `日時はエポックミリ秒で保存される`() {
        val entity = sampleNote().toEntity()
        assertEquals(1_700_000_000_123L, entity.createdAt)
        assertEquals(1_700_000_999_456L, entity.updatedAt)
    }

    @Test
    fun `プロパティはvalue_typeとvalue_textに符号化される`() {
        val byName = sampleNote().toPropertyEntities().associateBy { it.name }
        assertEquals(NotePropertyEntity("n1", "price", "NUMBER", "12.5"), byName["price"])
        assertEquals(NotePropertyEntity("n1", "due", "DATE", "2024-05-06"), byName["due"])
        assertEquals(NotePropertyEntity("n1", "none", "NULL", ""), byName["none"])
    }

    @Test
    fun `復元できないプロパティ値はNullになる`() {
        val restored = NoteWithRelations(
            note = NoteEntity("n", "t", "b", 0L, 0L),
            tagLinks = emptyList<NoteTagEntity>(),
            properties = listOf(NotePropertyEntity("n", "p", "NUMBER", "oops"))
        ).toDomain()
        assertEquals(PropertyValue.Null, restored.properties[PropertyName("p")])
    }

    @Test
    fun `タグがEntityを経由して往復し定義順が保たれる`() {
        val tag = tagOf(
            "t1",
            parents = listOf("p1", "p2"),
            defs = listOf(
                defOf("z", PropertyType.Date),
                defOf("a", PropertyType.Number, required = true),
                defOf("m", PropertyType.Boolean)
            ),
            name = "書籍"
        )
        val definitions = tag.toDefinitionEntities()
        assertEquals(listOf(0, 1, 2), definitions.map { it.position })
        // DB からの取得順が不定でも position で並び直る
        val restored = TagWithRelations(
            tag = tag.toEntity(createdAt = 42L),
            parentLinks = tag.toParentEntities(),
            definitions = definitions.reversed()
        ).toDomain()
        assertEquals(tag, restored)
    }

    @Test
    fun `タグEntityの型名と必須フラグ`() {
        val tag = tagOf("t", defs = listOf(defOf("a", PropertyType.Boolean, required = true)))
        assertEquals(
            listOf(TagPropertyDefinitionEntity("t", "a", "BOOLEAN", true, 0)),
            tag.toDefinitionEntities()
        )
        assertEquals(TagEntity("t", "t", 7L), tag.toEntity(7L))
        assertTrue(tag.toParentEntities().isEmpty())
    }

    @Test
    fun `未知の型名の定義はTextにフォールバックする`() {
        val restored = TagWithRelations(
            tag = TagEntity("t", "T", 0L),
            parentLinks = listOf(TagParentEntity("t", "p")),
            definitions = listOf(TagPropertyDefinitionEntity("t", "a", "???", false, 0))
        ).toDomain()
        assertEquals(PropertyType.Text, restored.ownPropertyDefinitions.single().type)
        assertEquals(setOf(TagId("p")), restored.parentIds)
    }
}
