package com.amgm.notebook.domain.service

import com.amgm.notebook.domain.defOf
import com.amgm.notebook.domain.error.TagNotFoundException
import com.amgm.notebook.domain.model.NoteId
import com.amgm.notebook.domain.model.PropertyName
import com.amgm.notebook.domain.model.PropertyType
import com.amgm.notebook.domain.model.PropertyValue
import com.amgm.notebook.domain.model.TagId
import com.amgm.notebook.domain.noteOf
import com.amgm.notebook.domain.tagMap
import com.amgm.notebook.domain.tagOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TableViewBuilderTest {

    private val tags = tagMap(
        tagOf("Book", defs = listOf(defOf("price", PropertyType.Number, true), defOf("author"))),
        tagOf("Ebook", parents = listOf("Book"), defs = listOf(defOf("size", PropertyType.Number))),
        tagOf("Other", defs = listOf(defOf("zzz"))),
        tagOf("Gift", defs = listOf(defOf("price", PropertyType.Date))),
        tagOf("Hybrid", parents = listOf("Book", "Gift"))
    )
    private val builder = TableViewBuilder()

    private fun num(v: Double) = PropertyValue.Number(v)
    private fun txt(v: String) = PropertyValue.Text(v)
    private fun ids(view: TableView): List<String> = view.rows.map { it.noteId.value }
    private val price = PropertyName("price")

    @Test
    fun `列は実効定義の順でタイトルは含まれない`() {
        val view = builder.build(TagId("Ebook"), tags, emptyList())
        assertEquals(
            listOf(
                TableColumn(price, PropertyType.Number, true),
                TableColumn(PropertyName("author"), PropertyType.Text, false),
                TableColumn(PropertyName("size"), PropertyType.Number, false)
            ),
            view.columns
        )
        assertEquals(TagId("Ebook"), view.tag.id)
        assertTrue(view.rows.isEmpty())
    }

    @Test
    fun `行は自タグと子孫タグのノートだけで無関係タグは除外`() {
        val notes = listOf(
            noteOf("book", tags = listOf("Book"), updated = 1),
            noteOf("ebook", tags = listOf("Ebook"), updated = 2),
            noteOf("other", tags = listOf("Other"), updated = 3),
            noteOf("none", updated = 4)
        )
        assertEquals(listOf("ebook", "book"), ids(builder.build(TagId("Book"), tags, notes)))
        assertEquals(listOf("ebook"), ids(builder.build(TagId("Ebook"), tags, notes)))
        assertEquals(listOf("other"), ids(builder.build(TagId("Other"), tags, notes)))
    }

    @Test
    fun `複数タグを持つノートも一度だけ現れる`() {
        val notes = listOf(noteOf("n", tags = listOf("Book", "Ebook", "Other")))
        assertEquals(listOf("n"), ids(builder.build(TagId("Book"), tags, notes)))
    }

    @Test
    fun `デフォルト順は更新日時の降順`() {
        val notes = listOf(
            noteOf("a", tags = listOf("Book"), updated = 10),
            noteOf("b", tags = listOf("Book"), updated = 30),
            noteOf("c", tags = listOf("Book"), updated = 20)
        )
        assertEquals(listOf("b", "c", "a"), ids(builder.build(TagId("Book"), tags, notes)))
    }

    @Test
    fun `セルは列順で値が無ければNull`() {
        val notes = listOf(
            noteOf("n", tags = listOf("Ebook"), props = mapOf("price" to num(9.0), "size" to num(2.0), "free" to txt("x")))
        )
        val row = builder.build(TagId("Ebook"), tags, notes).rows.single()
        assertEquals(NoteId("n"), row.noteId)
        assertEquals(listOf<PropertyValue>(num(9.0), PropertyValue.Null, num(2.0)), row.cells)
    }

    @Test
    fun `型が列と合わない値もそのままセルに入る`() {
        val notes = listOf(noteOf("n", tags = listOf("Book"), props = mapOf("price" to txt("高い"))))
        assertEquals(txt("高い"), builder.build(TagId("Book"), tags, notes).rows.single().cells[0])
    }

    @Test
    fun `Numberは数値順でNullは昇順降順どちらでも末尾`() {
        val notes = listOf(
            noteOf("p10", tags = listOf("Book"), props = mapOf("price" to num(10.0)), updated = 1),
            noteOf("p2", tags = listOf("Book"), props = mapOf("price" to num(2.0)), updated = 2),
            noteOf("none", tags = listOf("Book"), updated = 3),
            noteOf("p5", tags = listOf("Book"), props = mapOf("price" to num(5.0)), updated = 4)
        )
        val asc = builder.build(TagId("Book"), tags, notes, TableSort(TableSortKey.Property(price), true))
        val desc = builder.build(TagId("Book"), tags, notes, TableSort(TableSortKey.Property(price), false))
        assertEquals(listOf("p2", "p5", "p10", "none"), ids(asc))
        assertEquals(listOf("p10", "p5", "p2", "none"), ids(desc))
    }

    @Test
    fun `Textは大文字小文字を無視して並ぶ`() {
        val author = PropertyName("author")
        val notes = listOf(
            noteOf("b", tags = listOf("Book"), props = mapOf("author" to txt("banana"))),
            noteOf("A", tags = listOf("Book"), props = mapOf("author" to txt("Apple"))),
            noteOf("c", tags = listOf("Book"), props = mapOf("author" to txt("cherry")))
        )
        val asc = builder.build(TagId("Book"), tags, notes, TableSort(TableSortKey.Property(author), true))
        assertEquals(listOf("A", "b", "c"), ids(asc))
    }

    @Test
    fun `Booleanはfalseがtrueより前`() {
        val h = tagMap(tagOf("T", defs = listOf(defOf("flag", PropertyType.Boolean))))
        val notes = listOf(
            noteOf("yes", tags = listOf("T"), props = mapOf("flag" to PropertyValue.Bool(true))),
            noteOf("no", tags = listOf("T"), props = mapOf("flag" to PropertyValue.Bool(false)))
        )
        val asc = builder.build(TagId("T"), h, notes, TableSort(TableSortKey.Property(PropertyName("flag")), true))
        assertEquals(listOf("no", "yes"), ids(asc))
    }

    @Test
    fun `Dateは日付順`() {
        val h = tagMap(tagOf("T", defs = listOf(defOf("due", PropertyType.Date))))
        fun d(day: Int) = PropertyValue.Date(LocalDate.of(2024, 3, day))
        val notes = listOf(
            noteOf("late", tags = listOf("T"), props = mapOf("due" to d(20))),
            noteOf("early", tags = listOf("T"), props = mapOf("due" to d(2)))
        )
        val asc = builder.build(TagId("T"), h, notes, TableSort(TableSortKey.Property(PropertyName("due")), true))
        assertEquals(listOf("early", "late"), ids(asc))
    }

    @Test
    fun `タイトルでソートできる`() {
        val notes = listOf(
            noteOf("1", tags = listOf("Book"), title = "beta"),
            noteOf("2", tags = listOf("Book"), title = "Alpha"),
            noteOf("3", tags = listOf("Book"), title = "gamma")
        )
        val asc = builder.build(TagId("Book"), tags, notes, TableSort(TableSortKey.Title, true))
        val desc = builder.build(TagId("Book"), tags, notes, TableSort(TableSortKey.Title, false))
        assertEquals(listOf("2", "1", "3"), ids(asc))
        assertEquals(listOf("3", "1", "2"), ids(desc))
    }

    @Test
    fun `同値は更新日時の降順で安定する`() {
        val notes = listOf(
            noteOf("old", tags = listOf("Book"), props = mapOf("price" to num(1.0)), updated = 1),
            noteOf("new", tags = listOf("Book"), props = mapOf("price" to num(1.0)), updated = 9),
            noteOf("mid", tags = listOf("Book"), props = mapOf("price" to num(1.0)), updated = 5)
        )
        val asc = builder.build(TagId("Book"), tags, notes, TableSort(TableSortKey.Property(price), true))
        val desc = builder.build(TagId("Book"), tags, notes, TableSort(TableSortKey.Property(price), false))
        assertEquals(listOf("new", "mid", "old"), ids(asc))
        assertEquals(listOf("new", "mid", "old"), ids(desc))
    }

    @Test
    fun `衝突は表に伝搬し衝突した列は含まれない`() {
        val view = builder.build(TagId("Hybrid"), tags, emptyList())
        assertEquals(PropertyName("price"), view.conflicts.single().name)
        assertEquals(listOf("author"), view.columns.map { it.name.value })
    }

    @Test
    fun `存在しないタグはTagNotFoundException`() {
        assertThrows(TagNotFoundException::class.java) {
            builder.build(TagId("ghost"), tags, emptyList())
        }
    }

    @Test
    fun `Nullは末尾でTextの空文字は空文字として並び両方とも未入力扱い`() {
        val notes = listOf(
            noteOf("null", tags = listOf("Book"), props = mapOf("author" to PropertyValue.Null), updated = 1),
            noteOf("b", tags = listOf("Book"), props = mapOf("author" to txt("b")), updated = 2),
            noteOf("empty", tags = listOf("Book"), props = mapOf("author" to txt("")), updated = 3),
            noteOf("a", tags = listOf("Book"), props = mapOf("author" to txt("a")), updated = 4)
        )
        val key = TableSortKey.Property(PropertyName("author"))
        val asc = builder.build(TagId("Book"), tags, notes, TableSort(key, true))
        val desc = builder.build(TagId("Book"), tags, notes, TableSort(key, false))
        assertEquals(listOf("empty", "a", "b", "null"), ids(asc))
        assertEquals(listOf("b", "a", "empty", "null"), ids(desc))
        val cells = asc.rows.map { it.cells[1] }
        assertEquals(listOf(true, false, false, true), cells.map { it.isUnset })
    }
}
