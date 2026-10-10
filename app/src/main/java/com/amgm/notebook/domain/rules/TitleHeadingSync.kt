package com.amgm.notebook.domain.rules

/**
 * タイトルと本文の最初の h1 行を同期するための純粋関数群。
 * 保存時の不変条件には関与しない（同期の実行は UI が行う）。
 */
object TitleHeadingSync {

    /** 新規ノートの初期本文。title が空白のみなら ""、そうでなければ "# {title}\n\n"。 */
    fun initialBody(title: String): String =
        if (title.isBlank()) "" else "# ${title.trim()}\n\n"

    /**
     * 本文の最初の h1 行（行頭が "# "。"##" やコードブロック内は対象外）を "# {newTitle}" に置換する。
     * h1 が無ければ先頭に "# {newTitle}\n\n" を挿入（本文が CRLF なら CRLF）。
     * newTitle が空白のみなら body をそのまま返す。行末の改行は保持する。
     */
    fun syncFirstH1(body: String, newTitle: String): String {
        if (newTitle.isBlank()) return body
        val heading = "# ${newTitle.trim()}"

        var inFence = false
        var lineStart = 0
        while (lineStart <= body.length) {
            val lf = body.indexOf('\n', lineStart)
            val lineEnd = if (lf < 0) body.length else lf
            // CR を行内容から除く（CRLF 対応）。残した CR は終端として保持される
            val contentEnd = if (lineEnd > lineStart && body[lineEnd - 1] == '\r') lineEnd - 1 else lineEnd
            val content = body.substring(lineStart, contentEnd)

            if (content.trimStart().startsWith("```")) {
                inFence = !inFence
            } else if (!inFence && content.startsWith("# ")) {
                return body.substring(0, lineStart) + heading + body.substring(contentEnd)
            }
            if (lf < 0) break
            lineStart = lf + 1
        }

        val newline = if (body.contains("\r\n")) "\r\n" else "\n"
        return heading + newline + newline + body
    }
}
