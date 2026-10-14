package com.amgm.notebook.markdown

import com.amgm.notebook.markdown.MarkdownBlock.BulletList
import com.amgm.notebook.markdown.MarkdownBlock.CodeBlock
import com.amgm.notebook.markdown.MarkdownBlock.Heading
import com.amgm.notebook.markdown.MarkdownBlock.OrderedList
import com.amgm.notebook.markdown.MarkdownBlock.Paragraph
import com.amgm.notebook.markdown.MarkdownBlock.Quote
import com.amgm.notebook.markdown.MarkdownBlock.ThematicBreak
import com.amgm.notebook.markdown.MarkdownInline.Bold
import com.amgm.notebook.markdown.MarkdownInline.Code
import com.amgm.notebook.markdown.MarkdownInline.Italic
import com.amgm.notebook.markdown.MarkdownInline.LineBreak
import com.amgm.notebook.markdown.MarkdownInline.Text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    private fun blocks(src: String): List<MarkdownBlock> = MarkdownParser.parse(src).blocks

    @Test
    fun emptyInput() {
        assertEquals(emptyList<MarkdownBlock>(), blocks(""))
        assertEquals(emptyList<MarkdownBlock>(), blocks("\n\n  \n"))
    }

    @Test
    fun headings() {
        assertEquals(
            listOf(
                Heading(1, listOf(Text("a"))),
                Heading(2, listOf(Text("b"))),
                Heading(3, listOf(Text("c"))),
            ),
            blocks("# a\n## b\n### c")
        )
    }

    @Test
    fun headingLevelBoundary() {
        assertEquals(listOf(Paragraph(listOf(Text("#### x")))), blocks("#### x"))
        assertEquals(listOf(Paragraph(listOf(Text("#x")))), blocks("#x"))
    }

    @Test
    fun paragraphWithLineBreak() {
        assertEquals(
            listOf(Paragraph(listOf(Text("a"), LineBreak, Text("b")))),
            blocks("a\nb")
        )
    }

    @Test
    fun boldItalicCode() {
        assertEquals(
            listOf(Paragraph(listOf(Bold(listOf(Text("b"))), Text(" "), Italic(listOf(Text("i"))), Text(" "), Code("c")))),
            blocks("**b** *i* `c`")
        )
    }

    @Test
    fun codeContentIsNotParsed() {
        assertEquals(listOf(Paragraph(listOf(Code("**x**")))), blocks("`**x**`"))
    }

    @Test
    fun boldAndItalicMixed() {
        assertEquals(
            listOf(Paragraph(listOf(Bold(listOf(Text("a "), Italic(listOf(Text("b"))), Text(" c")))))),
            blocks("**a *b* c**")
        )
        assertEquals(
            listOf(Paragraph(listOf(Italic(listOf(Text("a "), Bold(listOf(Text("b"))), Text(" c")))))),
            blocks("*a **b** c*")
        )
    }

    @Test
    fun unclosedMarkersStayAsText() {
        assertEquals(listOf(Paragraph(listOf(Text("**a")))), blocks("**a"))
        assertEquals(listOf(Paragraph(listOf(Text("a*")))), blocks("a*"))
        assertEquals(listOf(Paragraph(listOf(Text("`a")))), blocks("`a"))
    }

    @Test
    fun escapes() {
        assertEquals(listOf(Paragraph(listOf(Text("*a*")))), blocks("\\*a\\*"))
    }

    @Test
    fun codeBlockWithLanguage() {
        assertEquals(
            listOf(CodeBlock("kotlin", "val a = 1\n\nval b = 2")),
            blocks("```kotlin\nval a = 1\n\nval b = 2\n```")
        )
    }

    @Test
    fun codeBlockWithoutLanguage() {
        assertEquals(listOf(CodeBlock(null, "x")), blocks("```\nx\n```"))
    }

    @Test
    fun unclosedCodeBlock() {
        assertEquals(listOf(CodeBlock("js", "a\nb")), blocks("```js\na\nb"))
        assertEquals(listOf(CodeBlock(null, "")), blocks("```"))
    }

    @Test
    fun bulletList() {
        assertEquals(
            listOf(BulletList(listOf(listOf(Text("a")), listOf(Text("b"))))),
            blocks("- a\n- b")
        )
    }

    @Test
    fun indentedBulletIsSameLevel() {
        assertEquals(
            listOf(BulletList(listOf(listOf(Text("a")), listOf(Text("b"))))),
            blocks("- a\n  - b")
        )
    }

    @Test
    fun orderedListKeepsStart() {
        assertEquals(
            listOf(OrderedList(3, listOf(listOf(Text("a")), listOf(Text("b"))))),
            blocks("3. a\n4. b")
        )
    }

    @Test
    fun quoteIsRecursive() {
        assertEquals(
            listOf(Quote(listOf(Heading(1, listOf(Text("t"))), Paragraph(listOf(Bold(listOf(Text("x")))))))),
            blocks("> # t\n> **x**")
        )
        assertEquals(
            listOf(Quote(listOf(Paragraph(listOf(Text("a"))), Paragraph(listOf(Text("b")))))),
            blocks("> a\n>\n> b")
        )
    }

    @Test
    fun thematicBreak() {
        assertEquals(
            listOf(Paragraph(listOf(Text("a"))), ThematicBreak, Paragraph(listOf(Text("b")))),
            blocks("a\n---\nb")
        )
        assertEquals(listOf(ThematicBreak), blocks("-----"))
    }

    @Test
    fun listAndParagraphBoundary() {
        assertEquals(
            listOf(Paragraph(listOf(Text("p"))), BulletList(listOf(listOf(Text("a")))), Paragraph(listOf(Text("q")))),
            blocks("p\n- a\n\nq")
        )
        assertEquals(
            listOf(BulletList(listOf(listOf(Text("a")))), OrderedList(1, listOf(listOf(Text("b"))))),
            blocks("- a\n1. b")
        )
    }

    @Test
    fun consecutiveBlankLines() {
        assertEquals(
            listOf(Paragraph(listOf(Text("a"))), Paragraph(listOf(Text("b")))),
            blocks("a\n\n\n\n\nb")
        )
    }

    @Test
    fun crlfAndCr() {
        val expected = listOf(Heading(1, listOf(Text("a"))), Paragraph(listOf(Text("b"), LineBreak, Text("c"))))
        assertEquals(expected, blocks("# a\r\nb\r\nc"))
        assertEquals(expected, blocks("# a\rb\rc"))
    }

    @Test
    fun unsupportedSyntaxIsText() {
        assertEquals(listOf(Paragraph(listOf(Text("[a](http://x)")))), blocks("[a](http://x)"))
        assertEquals(listOf(Paragraph(listOf(Text("![a](http://x)")))), blocks("![a](http://x)"))
        assertEquals(listOf(Paragraph(listOf(Text("| a | b |")))), blocks("| a | b |"))
        assertEquals(listOf(Paragraph(listOf(Text("<b>x</b>")))), blocks("<b>x</b>"))
    }

    @Test
    fun spacedStarsAreNotEmphasis() {
        assertEquals(listOf(Paragraph(listOf(Text("2 * 3 * 4")))), blocks("2 * 3 * 4"))
    }

    @Test
    fun invalidInputsDoNotThrow() {
        val inputs = listOf(
            "*", "**", "***", "****", "`", "``", "\\", "#", "# ", "-", "- ", ">", "> ", "1.", "1. ",
            "99999999999999999999. x", "```", "``` ```", "*`*`*", "**`**", "\\\\\\", "\u0000\uD800",
            "*".repeat(5000), ">".repeat(5000), "> ".repeat(3000) + "x", "**a".repeat(3000),
            "*a ".repeat(3000), "`".repeat(1001), "\n".repeat(1000),
        )
        for (s in inputs) {
            val doc = MarkdownParser.parse(s)
            assertTrue(doc.blocks.size >= 0)
        }
        val rnd = java.util.Random(42)
        val alphabet = "*`\\#->1. \n\r_[]()|<>abc"
        repeat(500) {
            val sb = StringBuilder()
            repeat(rnd.nextInt(60)) { sb.append(alphabet[rnd.nextInt(alphabet.length)]) }
            MarkdownParser.parse(sb.toString())
        }
    }
}
