package com.amgm.notebook.domain.rules

import com.amgm.notebook.domain.error.CyclicInheritanceException
import com.amgm.notebook.domain.error.DuplicateTagNameException
import com.amgm.notebook.domain.error.TagNotFoundException
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.tagOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TagRulesTest {

    private val existing = listOf(
        tagOf("a", name = "Book"),
        tagOf("b", parents = listOf("a"), name = "Ebook"),
        tagOf("c", parents = listOf("b"), name = "Comic")
    )

    @Test
    fun `正常な保存は例外にならない`() {
        TagRules.validateForSave(tagOf("d", parents = listOf("a", "c"), name = "New"), existing)
        TagRules.validateForSave(tagOf("a", name = "Book"), existing)
    }

    @Test
    fun `存在しない親はTagNotFoundException`() {
        val e = assertThrows(TagNotFoundException::class.java) {
            TagRules.validateForSave(tagOf("d", parents = listOf("ghost"), name = "New"), existing)
        }
        assertEquals(TagId("ghost"), e.tagId)
    }

    @Test
    fun `自己参照は経路が自身の2連のCyclicInheritanceException`() {
        val e = assertThrows(CyclicInheritanceException::class.java) {
            TagRules.validateForSave(tagOf("new", parents = listOf("new")), existing)
        }
        assertEquals(listOf(TagId("new"), TagId("new")), e.path)
    }

    @Test
    fun `間接循環はCyclicInheritanceException`() {
        val e = assertThrows(CyclicInheritanceException::class.java) {
            TagRules.validateForSave(tagOf("a", parents = listOf("c"), name = "Book"), existing)
        }
        assertEquals(TagId("a"), e.tagId)
        assertEquals(listOf("a", "c", "b", "a"), e.path.map { it.value })
    }

    @Test
    fun `名前重複は大文字小文字を無視して検出される`() {
        val e = assertThrows(DuplicateTagNameException::class.java) {
            TagRules.validateForSave(tagOf("x", name = " eBOOK "), existing)
        }
        assertEquals(" eBOOK ", e.name)
    }
}
