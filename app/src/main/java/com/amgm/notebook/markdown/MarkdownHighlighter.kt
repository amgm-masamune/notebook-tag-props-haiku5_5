package com.amgm.notebook.markdown

/** 編集欄に重ねる見た目の種類。文字列は変えず、範囲ごとに装飾だけを付ける。 */
enum class MarkdownStyle {
    /** 記号（`#` `>` `- ` `**` `` ` `` ``` など）。薄く表示する。 */
    Marker,
    /** 見出しの本文。 */
    Heading,
    Bold,
    Italic,
    /** インラインコードの中身。 */
    Code,
    /** コードブロックの各行。 */
    CodeBlock,
    /** 引用の本文。 */
    Quote,
}

/** 元の文字列の [start, end) に [style] を付ける。 */
data class MarkdownStyleSpan(val start: Int, val end: Int, val style: MarkdownStyle)

/**
 * 編集欄向けの軽量な記法スキャナ。
 *
 * - [MarkdownParser] と同じ規則（行単位のブロック判定、同一行内のインライン強調）で範囲を求める。
 * - 入力文字列は一切変更しない。返すのは装飾範囲のリストだけ。
 * - 処理量に上限を設ける（長すぎる本文・病的な入力では装飾せず素のまま返す）。
 *
 * 純 Kotlin（Android 非依存）なので JVM 上の単体テストで検証する。
 */
object MarkdownHighlighter {

    /** これより長い本文は装飾しない。入力のたびに走るため、重さの上限を決めておく。 */
    const val MAX_CHARS = 50_000

    /** 1 回の [highlight] で許す処理ステップ数の上限。超えたら装飾を諦める。 */
    private const val MAX_STEPS = 400_000

    private const val MAX_DEPTH = 8

    private val headingRegex = Regex("^#{1,3} +")
    private val hrRegex = Regex("^-{3,}\\s*$")
    private val orderedRegex = Regex("^\\s*\\d+\\. ")

    /** 装飾範囲を返す。入力が空・長すぎる・上限超過の場合は空リスト。 */
    fun highlight(source: String): List<MarkdownStyleSpan> {
        if (source.isEmpty() || source.length > MAX_CHARS) return emptyList()
        return try {
            val scan = Scan(source)
            scan.blocks()
            scan.out
        } catch (e: BudgetExceeded) {
            emptyList()
        }
    }

    private class BudgetExceeded : RuntimeException()

    private class Scan(private val src: String) {
        val out = ArrayList<MarkdownStyleSpan>()
        private var steps = 0

        private fun tick() {
            if (++steps > MAX_STEPS) throw BudgetExceeded()
        }

        private fun add(start: Int, end: Int, style: MarkdownStyle) {
            if (end > start) out.add(MarkdownStyleSpan(start, end, style))
        }

        // ---------- blocks（行単位。MarkdownParser.parseBlocks と同じ判定順） ----------

        fun blocks() {
            var inFence = false
            var lineStart = 0
            while (lineStart < src.length) {
                tick()
                var lineEnd = src.indexOf('\n', lineStart)
                if (lineEnd < 0) lineEnd = src.length
                val line = src.substring(lineStart, lineEnd)

                when {
                    inFence -> {
                        if (line.trim() == "```") {
                            add(lineStart, lineEnd, MarkdownStyle.Marker)
                            inFence = false
                        } else {
                            add(lineStart, lineEnd, MarkdownStyle.CodeBlock)
                        }
                    }
                    line.startsWith("```") -> {
                        add(lineStart, lineEnd, MarkdownStyle.Marker)
                        inFence = true
                    }
                    headingRegex.find(line) != null -> {
                        val markerLen = headingRegex.find(line)!!.value.length
                        add(lineStart, lineStart + markerLen, MarkdownStyle.Marker)
                        add(lineStart + markerLen, lineEnd, MarkdownStyle.Heading)
                        inlines(lineStart + markerLen, lineEnd, 0)
                    }
                    hrRegex.matches(line) -> {
                        add(lineStart, lineEnd, MarkdownStyle.Marker)
                    }
                    line == ">" || line.startsWith("> ") -> {
                        val markerLen = minOf(2, line.length)
                        add(lineStart, lineStart + markerLen, MarkdownStyle.Marker)
                        add(lineStart + markerLen, lineEnd, MarkdownStyle.Quote)
                        inlines(lineStart + markerLen, lineEnd, 0)
                    }
                    line.trimStart().startsWith("- ") -> {
                        val markerLen = (line.length - line.trimStart().length) + 2
                        add(lineStart, lineStart + markerLen, MarkdownStyle.Marker)
                        inlines(lineStart + markerLen, lineEnd, 0)
                    }
                    orderedRegex.find(line) != null -> {
                        val markerLen = orderedRegex.find(line)!!.value.length
                        add(lineStart, lineStart + markerLen, MarkdownStyle.Marker)
                        inlines(lineStart + markerLen, lineEnd, 0)
                    }
                    else -> inlines(lineStart, lineEnd, 0)
                }
                lineStart = lineEnd + 1
            }
        }

        // ---------- inlines（MarkdownParser.parseInlines と同じ規則） ----------

        private fun inlines(from: Int, to: Int, depth: Int) {
            if (depth > MAX_DEPTH) return
            var pos = from
            while (pos < to) {
                tick()
                val c = src[pos]
                if (c == '\\' && pos + 1 < to && src[pos + 1] in ESCAPABLE) {
                    pos += 2
                    continue
                }
                if (c == '`') {
                    val close = src.indexOf('`', pos + 1)
                    if (close > pos + 1 && close < to) {
                        add(pos, pos + 1, MarkdownStyle.Marker)
                        add(pos + 1, close, MarkdownStyle.Code)
                        add(close, close + 1, MarkdownStyle.Marker)
                        pos = close + 1
                    } else {
                        pos++
                    }
                    continue
                }
                if (c == '*') {
                    val bold = pos + 1 < to && src[pos + 1] == '*'
                    val delimLen = if (bold) 2 else 1
                    val contentStart = pos + delimLen
                    if (contentStart < to && !src[contentStart].isWhitespace()) {
                        val close = findClosing(contentStart, to, delimLen)
                        if (close > contentStart) {
                            add(pos, contentStart, MarkdownStyle.Marker)
                            add(close, close + delimLen, MarkdownStyle.Marker)
                            add(contentStart, close, if (bold) MarkdownStyle.Bold else MarkdownStyle.Italic)
                            inlines(contentStart, close, depth + 1)
                            pos = close + delimLen
                            continue
                        }
                    }
                    pos = contentStart
                    continue
                }
                pos++
            }
        }

        /** [from] 以降で [to] を超えない範囲にある閉じ記号の位置。無ければ -1。 */
        private fun findClosing(from: Int, to: Int, delimLen: Int): Int {
            var j = from
            while (j < to) {
                tick()
                val ch = src[j]
                if (ch == '\\') {
                    j += 2
                    continue
                }
                if (ch == '`') {
                    val end = src.indexOf('`', j + 1)
                    j = if (end > j + 1 && end < to) end + 1 else j + 1
                    continue
                }
                if (ch == '*') {
                    if (delimLen == 2) {
                        if (isDoubleStarAt(j, to) && j > from && !src[j - 1].isWhitespace()) return j
                        j++
                        continue
                    }
                    // 一重の閉じ: 内側の ** 対（太字）を読み飛ばす
                    if (isDoubleStarAt(j, to)) {
                        val inner = findClosing(j + 2, to, 2)
                        j = if (inner >= 0) inner + 2 else j + 2
                        continue
                    }
                    if (j > from && !src[j - 1].isWhitespace()) return j
                    j++
                    continue
                }
                j++
            }
            return -1
        }

        private fun isDoubleStarAt(j: Int, to: Int) = j + 1 < to && src[j] == '*' && src[j + 1] == '*'

        private companion object {
            val ESCAPABLE = charArrayOf('*', '`', '\\')
        }
    }
}
