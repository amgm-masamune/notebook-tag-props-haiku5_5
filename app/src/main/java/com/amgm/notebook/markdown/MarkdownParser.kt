package com.amgm.notebook.markdown

object MarkdownParser {

    private const val MAX_DEPTH = 20

    private val headingRegex = Regex("^(#{1,3}) +(.*)$")
    private val hrRegex = Regex("^-{3,}\\s*$")
    private val orderedRegex = Regex("^\\s*(\\d+)\\. (.*)$")

    fun parse(source: String): MarkdownDocument {
        return try {
            val normalized = source.replace("\r\n", "\n").replace('\r', '\n')
            MarkdownDocument(parseBlocks(normalized.split("\n"), 0))
        } catch (e: Exception) {
            MarkdownDocument(listOf(MarkdownBlock.Paragraph(listOf(MarkdownInline.Text(source)))))
        } catch (e: StackOverflowError) {
            MarkdownDocument(listOf(MarkdownBlock.Paragraph(listOf(MarkdownInline.Text(source)))))
        }
    }

    // ---------- blocks ----------

    private fun isFence(line: String) = line.startsWith("```")
    private fun isQuote(line: String) = line == ">" || line.startsWith("> ")
    private fun isBullet(line: String) = line.trimStart().startsWith("- ")
    private fun isOrdered(line: String) = orderedRegex.matches(line)

    private fun isBlockStart(line: String): Boolean =
        isFence(line) || headingRegex.matches(line) || hrRegex.matches(line) ||
            isQuote(line) || isBullet(line) || isOrdered(line)

    private fun parseBlocks(lines: List<String>, depth: Int): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (isFence(line)) {
                val lang = line.substring(3).trim().ifEmpty { null }
                val code = mutableListOf<String>()
                i++
                while (i < lines.size && lines[i].trim() != "```") {
                    code.add(lines[i])
                    i++
                }
                if (i < lines.size) i++ // closing fence
                blocks.add(MarkdownBlock.CodeBlock(lang, code.joinToString("\n")))
                continue
            }
            val heading = headingRegex.find(line)
            if (heading != null) {
                val level = heading.groupValues[1].length
                blocks.add(
                    MarkdownBlock.Heading(level, parseInlines(heading.groupValues[2].trim(), 0))
                )
                i++
                continue
            }
            if (hrRegex.matches(line)) {
                blocks.add(MarkdownBlock.ThematicBreak)
                i++
                continue
            }
            if (isQuote(line)) {
                val inner = mutableListOf<String>()
                while (i < lines.size && isQuote(lines[i])) {
                    inner.add(lines[i].drop(2))
                    i++
                }
                val children = if (depth >= MAX_DEPTH) {
                    listOf(MarkdownBlock.Paragraph(listOf(MarkdownInline.Text(inner.joinToString("\n")))))
                } else {
                    parseBlocks(inner, depth + 1)
                }
                blocks.add(MarkdownBlock.Quote(children))
                continue
            }
            if (isBullet(line) && !hrRegex.matches(line)) {
                val items = mutableListOf<List<MarkdownInline>>()
                while (i < lines.size && isBullet(lines[i]) && !hrRegex.matches(lines[i])) {
                    items.add(parseInlines(lines[i].trimStart().substring(2).trim(), 0))
                    i++
                }
                blocks.add(MarkdownBlock.BulletList(items))
                continue
            }
            if (isOrdered(line)) {
                val start = orderedRegex.find(line)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                val items = mutableListOf<List<MarkdownInline>>()
                while (i < lines.size) {
                    val m = orderedRegex.find(lines[i]) ?: break
                    items.add(parseInlines(m.groupValues[2].trim(), 0))
                    i++
                }
                blocks.add(MarkdownBlock.OrderedList(start, items))
                continue
            }
            if (line.isBlank()) {
                i++
                continue
            }
            // paragraph
            val inlines = mutableListOf<MarkdownInline>()
            var first = true
            while (i < lines.size && lines[i].isNotBlank() && (first || !isBlockStart(lines[i]))) {
                if (!first) inlines.add(MarkdownInline.LineBreak)
                inlines.addAll(parseInlines(lines[i].trim(), 0))
                first = false
                i++
            }
            blocks.add(MarkdownBlock.Paragraph(inlines))
        }
        return blocks
    }

    // ---------- inlines ----------

    private fun parseInlines(s: String, depth: Int): List<MarkdownInline> {
        if (depth > MAX_DEPTH) return if (s.isEmpty()) emptyList() else listOf(MarkdownInline.Text(s))
        val result = mutableListOf<MarkdownInline>()
        val sb = StringBuilder()

        fun flush() {
            if (sb.isNotEmpty()) {
                result.add(MarkdownInline.Text(sb.toString()))
                sb.setLength(0)
            }
        }

        var pos = 0
        while (pos < s.length) {
            val c = s[pos]
            if (c == '\\' && pos + 1 < s.length && (s[pos + 1] == '*' || s[pos + 1] == '`' || s[pos + 1] == '\\')) {
                sb.append(s[pos + 1])
                pos += 2
                continue
            }
            if (c == '`') {
                val close = s.indexOf('`', pos + 1)
                if (close > pos + 1) {
                    flush()
                    result.add(MarkdownInline.Code(s.substring(pos + 1, close)))
                    pos = close + 1
                } else {
                    sb.append(c)
                    pos++
                }
                continue
            }
            if (c == '*') {
                val bold = s.startsWith("**", pos)
                val delim = if (bold) "**" else "*"
                val from = pos + delim.length
                val opensOk = from < s.length && !s[from].isWhitespace()
                val close = if (opensOk) findClosing(s, from, delim) else -1
                if (close > from) {
                    flush()
                    val children = parseInlines(s.substring(from, close), depth + 1)
                    result.add(if (bold) MarkdownInline.Bold(children) else MarkdownInline.Italic(children))
                    pos = close + delim.length
                } else {
                    sb.append(delim)
                    pos = from
                }
                continue
            }
            sb.append(c)
            pos++
        }
        flush()
        return result
    }

    /** Index of the closing [delim] at or after [from], skipping escapes and code spans; -1 if none. */
    private fun findClosing(s: String, from: Int, delim: String): Int {
        var j = from
        while (j < s.length) {
            val ch = s[j]
            if (ch == '\\') {
                j += 2
                continue
            }
            if (ch == '`') {
                val end = s.indexOf('`', j + 1)
                if (end > j + 1) {
                    j = end + 1
                    continue
                }
                j++
                continue
            }
            if (ch == '*') {
                if (delim == "**") {
                    if (s.startsWith("**", j) && j > from && !s[j - 1].isWhitespace()) return j
                    j++
                    continue
                }
                // single-star closer: skip over a "**" pair (nested bold)
                if (s.startsWith("**", j)) {
                    val inner = findClosing(s, j + 2, "**")
                    if (inner >= 0) {
                        j = inner + 2
                        continue
                    }
                    j += 2
                    continue
                }
                if (j > from && !s[j - 1].isWhitespace()) return j
                j++
                continue
            }
            j++
        }
        return -1
    }
}
