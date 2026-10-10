package com.amgm.notebook.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * 「静かな紙面と、インクの色」の色トークン。色はすべてここに定義し、Theme.kt で ColorScheme に割り当てる。
 * ダイナミックカラーは使わず、ブランドの色を固定する。
 */

// ---- ライト ----
internal val PaperLight = Color(0xFFF6F5F1)          // 背景（温かい紙色）
internal val SurfaceLight = Color(0xFFFFFDF8)        // カード・入力欄（暖白）
internal val SurfaceDimLight = Color(0xFFEEEDE8)     // 一段沈んだ面（ヘッダ行・トラック）
internal val InkBlueLight = Color(0xFF2B3A8F)        // 主色
internal val OnInkBlueLight = Color(0xFFFFFFFF)
internal val InkBlueSoftLight = Color(0xFFE4E7F7)    // 選択インジケータ・淡い塗り
internal val OnInkBlueSoftLight = Color(0xFF1B2670)
internal val TealLight = Color(0xFF0F9D8A)           // アクセント
internal val TealSoftLight = Color(0xFFDDF3EF)
internal val OnTealSoftLight = Color(0xFF0B6F61)
internal val TextLight = Color(0xFF1B1C22)           // 本文
internal val TextSubLight = Color(0xFF6B6E7A)        // 補助
internal val ErrorLight = Color(0xFFC8432F)
internal val ErrorSoftLight = Color(0xFFF9E1DC)
internal val OnErrorSoftLight = Color(0xFF7A2315)
internal val AmberLight = Color(0xFF9A5B13)          // 注意（必須未入力・型衝突）
internal val AmberSoftLight = Color(0xFFF6E9D2)
internal val OnAmberSoftLight = Color(0xFF5E3607)
internal val OutlineLight = Color(0xFFB9B9C0)
internal val HairlineLight = Color(0xFFE4E2DB)       // 罫線
internal val RowAltLight = Color(0xFFF9F8F5)         // 表の交互行

// ---- ダーク ----
internal val PaperDark = Color(0xFF121317)
internal val SurfaceDark = Color(0xFF23252E)        // カード（インク寄りのスレート）
internal val SurfaceDimDark = Color(0xFF262932)
internal val InkBlueDark = Color(0xFFA9B6FF)
internal val OnInkBlueDark = Color(0xFF151C5C)
internal val InkBlueSoftDark = Color(0xFF2A3270)
internal val OnInkBlueSoftDark = Color(0xFFDDE1FF)
internal val TealDark = Color(0xFF5FD6C2)
internal val TealSoftDark = Color(0xFF173B36)
internal val OnTealSoftDark = Color(0xFF5FD6C2)
internal val TextDark = Color(0xFFE8E8EE)
internal val TextSubDark = Color(0xFF9FA2AF)
internal val ErrorDark = Color(0xFFFF8A78)
internal val ErrorSoftDark = Color(0xFF5A1D13)
internal val OnErrorSoftDark = Color(0xFFFFDAD4)
internal val AmberDark = Color(0xFFE8B96E)
internal val AmberSoftDark = Color(0xFF3D2F14)
internal val OnAmberSoftDark = Color(0xFFF6DDB0)
internal val OutlineDark = Color(0xFF5A5D6A)
internal val HairlineDark = Color(0xFF2E313B)
internal val RowAltDark = Color(0xFF1F2128)

// ---- 光（ui/lighting の光源モデル用）----
internal val LightKeyLight = Color(0xFFFFF8EA)       // 窓辺の朝の光（主光源。紙色より明るい暖白）
internal val ShadowInkLight = Color(0xFF3A3226)     // 紙の上の影（暖かい茶インク）
internal val GlassTintLight = Color(0xFFFFFFFF)     // すりガラスの板（ライト）
internal val GlassTintDark = Color(0xFF1C1E25)      // すりガラスの板（ダーク）
internal val LightKeyDark = Color(0xFFFFC98A)        // 卓上灯（暖色）
internal val LightHighlightDark = Color(0xFFFFF1DC)  // 卓上灯の反射（温白）
internal val LightRimDark = Color(0xFFFFF1DC)        // カードの縁に当たる暖白
