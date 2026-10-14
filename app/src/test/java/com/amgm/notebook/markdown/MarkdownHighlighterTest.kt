package com.amgm.notebook.markdown

import com.amgm.notebook.markdown.MarkdownStyle.Bold
import com.amgm.notebook.markdown.MarkdownStyle.Code
import com.amgm.notebook.markdown.MarkdownStyle.CodeBlock
import com.amgm.notebook.markdown.MarkdownStyle.Heading
import com.amgm.notebook.markdown.MarkdownStyle.Italic
import com.amgm.notebook.markdown.MarkdownStyle.Marker
import com.amgm.notebook.markdown.MarkdownStyle.Quote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownHighlighterTest {

    private fun span(start: Int, end: Int, style: MarkdownStyle) = MarkdownStyleSpan(start, end, style)

    private fun highlight(src: String) = MarkdownHighlighter.highlight(src)

    private fun assertSpans(src: String, expected: List<MarkdownStyleSpan>) {
        assertEquals(expected.sortedWith(compareBy({ it.start }, { it.end }, { it.style })),
            highlight(src).sortedWith(compareBy({ it.start }, { it.end }, { it.style })))
    }

    @Test
    fun emptyAndPlainTextHaveNoSpans() {
        assertSpans("", emptyList())
        assertSpans("ただの文章です", emptyList())
    }

    @Test
    fun headingMarkerAndBody() {
        assertSpans("# 題", listOf(span(0, 2, Marker), span(2, 3, Heading)))
        assertSpans("### 小見出し", listOf(span(0, 4, Marker), span(4, 8, Heading)))
    }

    @Test
    fun fourHashesAreNotHeading() {
        assertTrue(highlight("#### x").none { it.style == Heading })
        assertTrue(highlight("#x").none { it.style == Heading })
    }

    @Test
    fun boldAndItalic() {
        assertSpans("a **b** c", listOf(span(2, 4, Marker), span(4, 5, Bold), span(5, 7, Marker)))
        assertSpans("*i*", listOf(span(0, 1, Marker), span(1, 2, Italic), span(2, 3, Marker)))
    }

    @Test
    fun inlineCode() {
        assertSpans("`c`", listOf(span(0, 1, Marker), span(1, 2, Code), span(2, 3, Marker)))
    }

    @Test
    fun codeBlockLinesAreStyled() {
        val src = "```kt\nval x = 1\n```"
        val expected = listOf(
            span(0, 5, Marker),
            span(6, 15, CodeBlock),
            span(16, 19, Marker),
        )
        assertSpans(src, expected)
    }

    @Test
    fun unclosedFenceStylesToEnd() {
        val spans = highlight("```\nabc")
        assertTrue(spans.any { it.style == CodeBlock && it.end == 7 })
    }

    @Test
    fun quoteMarkerAndBody() {
        assertSpans("> 引用", listOf(span(0, 2, Marker), span(2, 4, Quote)))
    }

    @Test
    fun bulletAndOrderedMarkers() {
        assertSpans("- 項目", listOf(span(0, 2, Marker)))
        assertSpans("  - 項目", listOf(span(0, 4, Marker)))
        assertSpans("12. 項目", listOf(span(0, 4, Marker)))
    }

    @Test
    fun thematicBreakIsMarkerOnly() {
        assertSpans("---", listOf(span(0, 3, Marker)))
    }

    @Test
    fun escapedStarsAreNotStyled() {
        assertTrue(highlight("\\*a\\*").none { it.style == Italic || it.style == Bold })
    }

    @Test
    fun unclosedEmphasisIsNotStyled() {
        assertTrue(highlight("**abc").none { it.style == Bold })
        assertTrue(highlight("*abc").none { it.style == Italic })
    }

    @Test
    fun emphasisDoesNotOpenBeforeWhitespace() {
        assertTrue(highlight("* a *").none { it.style == Italic })
    }

    @Test
    fun nestedBoldAndItalic() {
        val spans = highlight("**a *b* c**")
        assertTrue(spans.contains(span(2, 9, Bold)))
        assertTrue(spans.contains(span(5, 6, Italic)))
        assertTrue(spans.contains(span(0, 2, Marker)))
        assertTrue(spans.contains(span(9, 11, Marker)))
    }

    @Test
    fun emphasisDoesNotCrossLines() {
        val spans = highlight("*a\nb*")
        assertTrue(spans.none { it.style == Italic })
    }

    @Test
    fun codeSpanInsideBoldIsKept() {
        val spans = highlight("**`x`**")
        assertTrue(spans.any { it.style == Bold })
        assertTrue(spans.any { it.style == Code })
    }

    @Test
    fun everySpanIsInBoundsAndNonEmpty() {
        val src = "# 見出し\n- **太字** と *斜体* と `code`\n> 引用 **x**\n```\nfence\n```\n1. 番号\n---\n"
        highlight(src).forEach { s ->
            assertTrue("bad span $s", s.start >= 0 && s.end <= src.length && s.start < s.end)
        }
    }

    @Test
    fun oversizedSourceIsNotStyled() {
        val src = "**a** ".repeat(MarkdownHighlighter.MAX_CHARS / 3)
        assertTrue(src.length > MarkdownHighlighter.MAX_CHARS)
        assertTrue(highlight(src).isEmpty())
    }

    @Test
    fun pathologicalInputTerminatesWithinBounds() {
        // 閉じ記号の探索が入れ子で膨らむ入力。処理上限で打ち切られても範囲は必ず本文内に収まる。
        val src = "* ** ".repeat(8000)
        val spans = highlight(src)
        spans.forEach { s -> assertTrue(s.start >= 0 && s.end <= src.length && s.start < s.end) }
    }
}
