package com.amgm.notebook.domain.rules

import com.amgm.notebook.domain.defOf
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.noteOf
import com.amgm.notebook.domain.tagMap
import com.amgm.notebook.domain.tagOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NoteValidatorTest {

    private val tags = tagMap(
        tagOf(
            "Book",
            defs = listOf(
                defOf("price", PropertyType.Number, required = true),
                defOf("author", PropertyType.Text),
                defOf("read", PropertyType.Boolean),
                defOf("published", PropertyType.Date)
            )
        ),
        tagOf("A", defs = listOf(defOf("x", PropertyType.Text))),
        tagOf("B", defs = listOf(defOf("x", PropertyType.Number))),
        tagOf("AB", parents = listOf("A", "B"))
    )
    private val resolver = EffectivePropertyResolver(tags)
    private val validator = NoteValidator()

    private fun validate(tag: String, props: Map<String, PropertyValue>): ValidationResult {
        val note = noteOf("n", tags = listOf(tag), props = props)
        return validator.validate(note, resolver.resolveForTags(note.tagIds))
    }

    @Test
    fun `全て正しければ問題なし`() {
        val result = validate(
            "Book",
            mapOf(
                "price" to PropertyValue.Number(10.0),
                "author" to PropertyValue.Text("me"),
                "read" to PropertyValue.Bool(true),
                "published" to PropertyValue.Date(LocalDate.of(2024, 1, 1))
            )
        )
        assertTrue(result.issues.isEmpty())
        assertFalse(result.hasErrors)
    }

    @Test
    fun `型不一致はERRORで保存不可を示す`() {
        val result = validate("Book", mapOf("price" to PropertyValue.Text("高い")))
        assertTrue(result.hasErrors)
        val issue = result.errors.single() as ValidationIssue.TypeMismatch
        assertEquals(PropertyName("price"), issue.name)
        assertEquals(PropertyType.Number, issue.expected)
        assertEquals(PropertyValue.Text("高い"), issue.actual)
        assertEquals(Severity.ERROR, issue.severity)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `必須未入力はWARNINGで保存可能`() {
        val result = validate("Book", emptyMap())
        assertFalse(result.hasErrors)
        val issue = result.warnings.single() as ValidationIssue.RequiredMissing
        assertEquals(PropertyName("price"), issue.name)
        assertEquals(Severity.WARNING, issue.severity)
    }

    @Test
    fun `Nullの必須プロパティも未入力として警告される`() {
        val result = validate("Book", mapOf("price" to PropertyValue.Null))
        assertEquals(listOf<ValidationIssue>(ValidationIssue.RequiredMissing(PropertyName("price"))), result.issues)
    }

    @Test
    fun `Nullは型検証の対象外`() {
        val result = validate(
            "Book",
            mapOf(
                "price" to PropertyValue.Number(1.0),
                "author" to PropertyValue.Null,
                "read" to PropertyValue.Null
            )
        )
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun `任意項目の未入力は問題なし`() {
        val result = validate("Book", mapOf("price" to PropertyValue.Number(1.0)))
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun `自由プロパティは型に関わらず許容される`() {
        val result = validate(
            "Book",
            mapOf(
                "price" to PropertyValue.Number(1.0),
                "memo" to PropertyValue.Bool(true),
                "free" to PropertyValue.Date(LocalDate.of(2020, 2, 29))
            )
        )
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun `衝突はWARNINGとして報告されエラーにはならない`() {
        val result = validate("AB", mapOf("x" to PropertyValue.Bool(true)))
        assertFalse(result.hasErrors)
        val issue = result.warnings.single() as ValidationIssue.Conflict
        assertEquals(PropertyName("x"), issue.conflict.name)
        assertEquals(Severity.WARNING, issue.severity)
    }

    @Test
    fun `エラーと警告が同時に出る`() {
        val result = validate("Book", mapOf("author" to PropertyValue.Number(1.0)))
        assertEquals(1, result.errors.size)
        assertEquals(1, result.warnings.size)
        assertTrue(result.hasErrors)
    }

    @Test
    fun `タグがなければ何も検証されない`() {
        val note = noteOf("n", props = mapOf("price" to PropertyValue.Text("x")))
        val result = NoteValidator.validate(note, resolver.resolveForTags(note.tagIds))
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun `タグ付け替えで型が合わなくなった値はERRORになる`() {
        val note = noteOf("n", tags = listOf("Book"), props = mapOf("published" to PropertyValue.Text("昨日")))
        val result = validator.validate(note, resolver.resolve(TagId("Book")))
        assertEquals(PropertyName("published"), (result.errors.single() as ValidationIssue.TypeMismatch).name)
    }

    @Test
    fun `必須はNullも空文字Textも未入力として警告されるが空白文字は入力扱い`() {
        val price = PropertyName("price")
        val nullResult = validate("Book", mapOf("price" to PropertyValue.Null))
        assertEquals(listOf<ValidationIssue>(ValidationIssue.RequiredMissing(price)), nullResult.issues)
        val emptyTextResult = validate("Book", mapOf("price" to PropertyValue.Text("")))
        assertEquals(listOf<ValidationIssue>(ValidationIssue.RequiredMissing(price)), emptyTextResult.issues)
        assertFalse(emptyTextResult.hasErrors)
        val authorTags = tagMap(tagOf("Req", defs = listOf(defOf("author", PropertyType.Text, required = true))))
        val note = noteOf("n", tags = listOf("Req"), props = mapOf("author" to PropertyValue.Text("")))
        val result = validator.validate(note, EffectivePropertyResolver(authorTags).resolveForTags(note.tagIds))
        assertEquals(1, result.warnings.size)
        val filled = noteOf("n", tags = listOf("Req"), props = mapOf("author" to PropertyValue.Text("x")))
        assertTrue(validator.validate(filled, EffectivePropertyResolver(authorTags).resolveForTags(filled.tagIds)).issues.isEmpty())
    }
}
