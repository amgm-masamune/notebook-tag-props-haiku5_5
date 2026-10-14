package com.amgm.notebook.ui.theme

import androidx.compose.ui.unit.dp

/** 余白・角丸の共通値。 */
object NotebookDimens {
    /** 画面の左右余白。 */
    val ScreenPadding = 20.dp
    /** セクション間の余白。 */
    val SectionSpacing = 24.dp
    /** カード内の余白。 */
    val CardPadding = 16.dp
    val CardRadius = 20.dp
    val InputRadius = 14.dp

    /** 主要カード（プロパティ・本文・名前などのセクションカード）の影の強さ。 */
    val MainElevation = 6.dp
    /** 行・小カード（一覧の行、タグのカード）の影の強さ。 */
    val RowElevation = 3.dp
}
