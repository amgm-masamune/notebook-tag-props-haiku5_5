package com.amgm.notebook.domain.rules

import com.amgm.notebook.domain.defOf
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.noteOf
import com.amgm.notebook.domain.tagMap
import com.amgm.notebook.domain.tagOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteLinkTest {

    private fun ids(vararg v: String) = v.map { NoteId(it) }

    private fun validate(value: PropertyValue?, required: Boolean = false): ValidationResult {
        val tag = tagOf("book", defs = listOf(defOf("author", PropertyType.NoteLink(null), required)))
        val schema = EffectivePropertyResolver(tagMap(tag)).resolve(TagId("book"))
        val props = if (value == null) emptyMap() else mapOf("author" to value)
        return NoteValidator.validate(noteOf("n", tags = listOf("book"), props = props), schema)
    }

    @Test
    fun `ofは重複を除去し最初の出現順を保つ`() {
        assertEquals(ids("b", "a", "c"), PropertyValue.NoteRefs.of(ids("b", "a", "b", "c", "a")).ids)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `コンストラクタは重複を拒否する`() {
        PropertyValue.NoteRefs(ids("a", "a"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `カンマを含むIDは拒否される`() {
        PropertyValue.NoteRefs(ids("a,b"))
    }

    @Test
    fun `matchesはNoteLink型にNoteRefsとNullだけ一致する`() {
        val type = PropertyType.NoteLink(TagId("t"))
        assertTrue(PropertyValue.NoteRefs(ids("a")).matches(type))
        assertTrue(PropertyValue.Null.matches(type))
        assertFalse(PropertyValue.Text("a").matches(type))
        assertFalse(PropertyValue.Number(1.0).matches(type))
        assertFalse(PropertyValue.NoteRefs(ids("a")).matches(PropertyType.Text))
    }

    @Test
    fun `validatorはNoteLink定義へのText値を型不一致にする`() {
        val result = validate(PropertyValue.Text("x"))
        assertTrue(result.hasErrors)
        val issue = result.errors.single() as ValidationIssue.TypeMismatch
        assertEquals(PropertyName("author"), issue.name)
        assertEquals(PropertyType.NoteLink(null), issue.expected)
    }

    @Test
    fun `validatorはNoteRefsを受理し存在チェックをしない`() {
        assertTrue(validate(PropertyValue.NoteRefs(ids("missing"))).issues.isEmpty())
    }

    @Test
    fun `必須のNoteLinkが未入力なら警告になる`() {
        val missing = validate(null, required = true)
        assertFalse(missing.hasErrors)
        assertEquals(listOf<ValidationIssue>(ValidationIssue.RequiredMissing(PropertyName("author"))), missing.warnings)
        assertEquals(1, validate(PropertyValue.Null, required = true).warnings.size)
    }

    @Test
    fun `NoteLink同士はtargetTagIdが違っても衝突せず子の定義が優先される`() {
        val parent = tagOf("p", defs = listOf(defOf("ref", PropertyType.NoteLink(TagId("a")))))
        val child = tagOf("c", parents = listOf("p"), defs = listOf(defOf("ref", PropertyType.NoteLink(TagId("b")))))
        val schema = EffectivePropertyResolver(tagMap(parent, child)).resolve(TagId("c"))
        assertTrue(schema.conflicts.isEmpty())
        assertEquals(PropertyType.NoteLink(TagId("b")), schema.find(PropertyName("ref"))!!.definition.type)
    }

    @Test
    fun `別系統の親のNoteLinkも衝突しない`() {
        val p1 = tagOf("p1", defs = listOf(defOf("ref", PropertyType.NoteLink(null))))
        val p2 = tagOf("p2", defs = listOf(defOf("ref", PropertyType.NoteLink(TagId("x")))))
        val child = tagOf("c", parents = listOf("p1", "p2"))
        val schema = EffectivePropertyResolver(tagMap(p1, p2, child)).resolve(TagId("c"))
        assertTrue(schema.conflicts.isEmpty())
        assertEquals(PropertyType.NoteLink(TagId("x")), schema.find(PropertyName("ref"))!!.definition.type)
    }

    @Test
    fun `NoteLinkと他の型は従来どおり衝突する`() {
        val p1 = tagOf("p1", defs = listOf(defOf("ref", PropertyType.NoteLink(null))))
        val p2 = tagOf("p2", defs = listOf(defOf("ref", PropertyType.Text)))
        val child = tagOf("c", parents = listOf("p1", "p2"))
        val schema = EffectivePropertyResolver(tagMap(p1, p2, child)).resolve(TagId("c"))
        assertEquals(1, schema.conflicts.size)
        assertTrue(schema.definitions.isEmpty())
    }

    @Test
    fun `空のNoteRefsは作れずofNoteRefsはNullに正規化する`() {
        assertEquals(PropertyValue.Null, PropertyValue.ofNoteRefs(emptyList()))
        assertEquals(PropertyValue.NoteRefs(ids("a")), PropertyValue.ofNoteRefs(ids("a", "a")))
        assertThrows(IllegalArgumentException::class.java) { PropertyValue.NoteRefs(emptyList()) }
    }
}
