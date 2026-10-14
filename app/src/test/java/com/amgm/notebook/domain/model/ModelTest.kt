package com.amgm.notebook.domain.model

import com.amgm.notebook.domain.defOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ModelTest {

    private fun describe(id: NoteId): String = "note:${id.value}"
    private fun describe(id: TagId): String = "tag:${id.value}"

    @Test
    fun `同じ文字列のNoteIdとTagIdが型として区別される`() {
        assertEquals("note:x", describe(NoteId("x")))
        assertEquals("tag:x", describe(TagId("x")))
    }

    @Test
    fun `IDの値が保持され同値比較できる`() {
        assertEquals(NoteId("a"), NoteId("a"))
        assertEquals("a", TagId("a").value)
    }

    @Test
    fun `空のIDは拒否される`() {
        assertThrows(IllegalArgumentException::class.java) { NoteId("") }
        assertThrows(IllegalArgumentException::class.java) { NoteId("  ") }
        assertThrows(IllegalArgumentException::class.java) { TagId("") }
        assertThrows(IllegalArgumentException::class.java) { TagId(" \t") }
    }

    @Test
    fun `PropertyNameはtrim済みの非空のみ`() {
        assertEquals("price", PropertyName("price").value)
        assertThrows(IllegalArgumentException::class.java) { PropertyName("") }
        assertThrows(IllegalArgumentException::class.java) { PropertyName(" price") }
        assertThrows(IllegalArgumentException::class.java) { PropertyName("price ") }
        assertThrows(IllegalArgumentException::class.java) { PropertyName("  ") }
    }

    @Test
    fun `PropertyNameは大文字小文字を区別する`() {
        assertFalse(PropertyName("Price") == PropertyName("price"))
    }

    @Test
    fun `Numberは非有限値を拒否する`() {
        assertThrows(IllegalArgumentException::class.java) { PropertyValue.Number(Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { PropertyValue.Number(Double.POSITIVE_INFINITY) }
        assertThrows(IllegalArgumentException::class.java) { PropertyValue.Number(Double.NEGATIVE_INFINITY) }
        assertEquals(1.5, PropertyValue.Number(1.5).value, 0.0)
    }

    @Test
    fun `matchesは型ごとに判定する`() {
        val text = PropertyValue.Text("a")
        val number = PropertyValue.Number(1.0)
        val bool = PropertyValue.Bool(true)
        val date = PropertyValue.Date(LocalDate.of(2024, 1, 1))

        assertTrue(text.matches(PropertyType.Text))
        assertFalse(text.matches(PropertyType.Number))
        assertTrue(number.matches(PropertyType.Number))
        assertFalse(number.matches(PropertyType.Text))
        assertTrue(bool.matches(PropertyType.Boolean))
        assertFalse(bool.matches(PropertyType.Date))
        assertTrue(date.matches(PropertyType.Date))
        assertFalse(date.matches(PropertyType.Boolean))
    }

    @Test
    fun `Emptyはどの型にも適合する`() {
        listOf(PropertyType.Text, PropertyType.Number, PropertyType.Boolean, PropertyType.Date).forEach {
            assertTrue(PropertyValue.Empty.matches(it))
        }
    }

    @Test
    fun `タグ名は空にできない`() {
        assertThrows(IllegalArgumentException::class.java) {
            Tag(TagId("t"), "  ", emptySet(), emptyList())
        }
    }

    @Test
    fun `同一タグ内のプロパティ名重複は拒否される`() {
        assertThrows(IllegalArgumentException::class.java) {
            Tag(TagId("t"), "T", emptySet(), listOf(defOf("a"), defOf("a", PropertyType.Number)))
        }
    }

    @Test
    fun `自己参照を含むタグも値としては生成できる`() {
        // 自己参照の拒否はリポジトリ保存時の責務
        val tag = Tag(TagId("t"), "T", setOf(TagId("t")), emptyList())
        assertEquals(setOf(TagId("t")), tag.parentIds)
    }
}
