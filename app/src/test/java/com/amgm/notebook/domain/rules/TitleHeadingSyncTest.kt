package com.amgm.notebook.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class TitleHeadingSyncTest {

    @Test
    fun `新規本文はh1と空行`() {
        assertEquals("# 題名\n\n", TitleHeadingSync.initialBody("題名"))
    }

    @Test
    fun `空白のみのタイトルでは新規本文は空`() {
        assertEquals("", TitleHeadingSync.initialBody(""))
        assertEquals("", TitleHeadingSync.initialBody("  \t"))
    }

    @Test
    fun `既存の最初のh1だけが置換される`() {
        val body = "# 旧\n\n本文\n\n# 二つ目\n"
        assertEquals("# 新\n\n本文\n\n# 二つ目\n", TitleHeadingSync.syncFirstH1(body, "新"))
    }

    @Test
    fun `h1より前に本文があっても最初のh1を置換する`() {
        assertEquals("前置き\n# 新\n後", TitleHeadingSync.syncFirstH1("前置き\n# 旧\n後", "新"))
    }

    @Test
    fun `h1が無ければ先頭に挿入する`() {
        assertEquals("# 新\n\n本文", TitleHeadingSync.syncFirstH1("本文", "新"))
        assertEquals("# 新\n\n", TitleHeadingSync.syncFirstH1("", "新"))
    }

    @Test
    fun `見出しレベル2以上とシャープ直後が空白でない行は対象外`() {
        assertEquals("# 新\n\n## 小見出し\n#タグ", TitleHeadingSync.syncFirstH1("## 小見出し\n#タグ", "新"))
    }

    @Test
    fun `コードブロック内のシャープは無視される`() {
        val body = "```\n# コメント\n```\n# 旧\n"
        assertEquals("```\n# コメント\n```\n# 新\n", TitleHeadingSync.syncFirstH1(body, "新"))
    }

    @Test
    fun `コードブロック内にしかh1が無ければ先頭に挿入する`() {
        val body = "```\n# コメント\n```\n"
        assertEquals("# 新\n\n$body", TitleHeadingSync.syncFirstH1(body, "新"))
    }

    @Test
    fun `空白のみの新タイトルでは本文を変更しない`() {
        assertEquals("# 旧\n", TitleHeadingSync.syncFirstH1("# 旧\n", "  "))
        assertEquals("", TitleHeadingSync.syncFirstH1("", ""))
    }

    @Test
    fun `CRLFの改行を保って置換する`() {
        assertEquals("# 新\r\n\r\n本文\r\n", TitleHeadingSync.syncFirstH1("# 旧\r\n\r\n本文\r\n", "新"))
    }

    @Test
    fun `CRLF本文への挿入はCRLFを使う`() {
        assertEquals("# 新\r\n\r\n本文\r\n", TitleHeadingSync.syncFirstH1("本文\r\n", "新"))
    }

    @Test
    fun `末尾の改行の有無を保つ`() {
        assertEquals("# 新", TitleHeadingSync.syncFirstH1("# 旧", "新"))
        assertEquals("# 新\n", TitleHeadingSync.syncFirstH1("# 旧\n", "新"))
    }

    @Test
    fun `同じタイトルなら冪等`() {
        val once = TitleHeadingSync.syncFirstH1("本文", "新")
        assertEquals(once, TitleHeadingSync.syncFirstH1(once, "新"))
    }
}
