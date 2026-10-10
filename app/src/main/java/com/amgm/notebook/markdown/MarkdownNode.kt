package com.amgm.notebook.markdown

data class MarkdownDocument(val blocks: List<MarkdownBlock>)

sealed interface MarkdownBlock {
    data class Heading(val level: Int, val inlines: List<MarkdownInline>) : MarkdownBlock
    data class Paragraph(val inlines: List<MarkdownInline>) : MarkdownBlock
    data class CodeBlock(val language: String?, val code: String) : MarkdownBlock
    data class BulletList(val items: List<List<MarkdownInline>>) : MarkdownBlock
    data class OrderedList(val start: Int, val items: List<List<MarkdownInline>>) : MarkdownBlock
    data class Quote(val blocks: List<MarkdownBlock>) : MarkdownBlock
    data object ThematicBreak : MarkdownBlock
}

sealed interface MarkdownInline {
    data class Text(val text: String) : MarkdownInline
    data class Bold(val children: List<MarkdownInline>) : MarkdownInline
    data class Italic(val children: List<MarkdownInline>) : MarkdownInline
    data class Code(val text: String) : MarkdownInline
    data object LineBreak : MarkdownInline
}
