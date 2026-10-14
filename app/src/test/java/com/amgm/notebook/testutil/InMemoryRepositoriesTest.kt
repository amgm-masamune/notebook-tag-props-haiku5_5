package com.amgm.notebook.testutil

import com.amgm.notebook.domain.defOf
import com.amgm.notebook.domain.error.CyclicInheritanceException
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.noteOf
import com.amgm.notebook.domain.tagOf
import com.amgm.notebook.testing.InMemoryRepositories
import com.amgm.notebook.testing.assertThrowsInstance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** フェイク固有の振る舞い（契約テストに含まれないもの）。 */
class InMemoryRepositoriesTest {

    @Test
    fun `putUncheckedは検証を通さず循環状態を仕込める`() = runBlocking<Unit> {
        val repos = InMemoryRepositories()
        repos.tags.putUnchecked(tagOf("a"))
        repos.tags.putUnchecked(tagOf("b", parents = listOf("a")))
        repos.tags.putUnchecked(tagOf("a", parents = listOf("b")))
        assertEquals(setOf(TagId("b")), repos.tags.getTag(TagId("a"))!!.parentIds)
    }

    @Test
    fun `仕込んだ循環状態に対する保存は拒否される`() {
        val repos = InMemoryRepositories()
        repos.tags.putUnchecked(tagOf("a"))
        repos.tags.putUnchecked(tagOf("b", parents = listOf("a")))
        assertThrowsInstance<CyclicInheritanceException> {
            runBlocking { repos.tags.save(tagOf("a", parents = listOf("b"))) }
        }
    }

    @Test
    fun `タグ削除後も定義済みだった値は自由プロパティとして残る`() = runBlocking<Unit> {
        val repos = InMemoryRepositories()
        repos.tags.save(tagOf("t", defs = listOf(defOf("p"))))
        repos.notes.save(noteOf("n", tags = listOf("t"), props = mapOf("p" to PropertyValue.Text("v"))))
        repos.tags.delete(TagId("t"))
        val note = repos.notes.snapshot.values.single()
        assertTrue(note.tagIds.isEmpty())
        assertEquals(PropertyValue.Text("v"), note.properties[PropertyName("p")])
    }
}
