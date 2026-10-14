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
import org.junit.Test

class NotePropertyRulesTest {

    private val tags = tagMap(
        tagOf("Parent", defs = listOf(defOf("pp", PropertyType.Text), defOf("shared", PropertyType.Text))),
        tagOf("Child", parents = listOf("Parent"), defs = listOf(defOf("cc", PropertyType.Number))),
        tagOf(
            "Book",
            defs = listOf(
                defOf("title2", PropertyType.Text),
                defOf("price", PropertyType.Number),
                defOf("read", PropertyType.Boolean),
                defOf("shared", PropertyType.Text)
            )
        ),
        tagOf("Other", defs = listOf(defOf("shared", PropertyType.Text)))
    )

    private fun p(name: String) = PropertyName(name)
    private fun ids(vararg s: String) = s.map { TagId(it) }.toSet()
    private val nul = PropertyValue.Null
    private fun txt(v: String) = PropertyValue.Text(v)

    @Test
    fun `タグ付与で定義名がNullで入る`() {
        val note = noteOf("n", tags = listOf("Book"))
        val result = onTagsAdded(note, tags, ids("Book"))
        assertEquals(
            mapOf(p("title2") to nul, p("price") to nul, p("read") to nul, p("shared") to nul),
            result
        )
    }

    @Test
    fun `タグ付与は既存の値を変えない`() {
        val note = noteOf(
            "n", tags = listOf("Book"),
            props = mapOf("price" to PropertyValue.Number(5.0), "free" to txt(""))
        )
        val result = onTagsAdded(note, tags, ids("Book"))
        assertEquals(PropertyValue.Number(5.0), result[p("price")])
        assertEquals(txt(""), result[p("free")])
        assertEquals(nul, result[p("read")])
        assertEquals(5, result.size)
    }

    @Test
    fun `継承した定義も付与でNullになる`() {
        val note = noteOf("n", tags = listOf("Child"))
        assertEquals(
            mapOf(p("pp") to nul, p("shared") to nul, p("cc") to nul),
            onTagsAdded(note, tags, ids("Child"))
        )
    }

    @Test
    fun `取り外しでNullは消えTextの空文字とNumberは残る`() {
        val props = mapOf(
            p("title2") to nul,
            p("price") to PropertyValue.Number(3.0),
            p("read") to nul,
            p("shared") to txt("")
        )
        val result = onTagsRemoved(props, emptySet(), ids("Book"), tags)
        assertEquals(
            mapOf(p("price") to PropertyValue.Number(3.0), p("shared") to txt("")),
            result
        )
    }

    @Test
    fun `Nullの文字列プロパティは取り外しで消えTextの値は自由プロパティで残る`() {
        val props = mapOf(p("title2") to nul, p("shared") to txt("v"), p("read") to PropertyValue.Bool(false))
        val result = onTagsRemoved(props, emptySet(), ids("Book"), tags)
        assertEquals(mapOf(p("shared") to txt("v"), p("read") to PropertyValue.Bool(false)), result)
    }

    @Test
    fun `残ったタグが定義する名前は触らない`() {
        val props = mapOf(p("shared") to nul, p("price") to nul, p("title2") to nul)
        val result = onTagsRemoved(props, ids("Other"), ids("Book"), tags)
        assertEquals(mapOf(p("shared") to nul), result)
    }

    @Test
    fun `自由プロパティは変更しない`() {
        val props = mapOf(p("free") to nul, p("price") to nul)
        val result = onTagsRemoved(props, emptySet(), ids("Book"), tags)
        assertEquals(mapOf(p("free") to nul), result)
    }

    @Test
    fun `子を外しても親由来は維持され子由来のNullだけ消える`() {
        val props = mapOf(p("pp") to nul, p("shared") to txt(""), p("cc") to nul)
        // Child を外して Parent だけ残す
        val result = onTagsRemoved(props, ids("Parent"), ids("Child"), tags)
        assertEquals(mapOf(p("pp") to nul, p("shared") to txt("")), result)
    }

    @Test
    fun `子由来の非Null値は子を外しても残る`() {
        val props = mapOf(p("pp") to nul, p("cc") to PropertyValue.Number(1.0))
        val result = onTagsRemoved(props, ids("Parent"), ids("Child"), tags)
        assertEquals(mapOf(p("pp") to nul, p("cc") to PropertyValue.Number(1.0)), result)
    }

    @Test
    fun `取り外し対象が無ければ何も変えない`() {
        val props = mapOf(p("price") to nul)
        assertEquals(props, onTagsRemoved(props, ids("Book"), emptySet(), tags))
    }
}
