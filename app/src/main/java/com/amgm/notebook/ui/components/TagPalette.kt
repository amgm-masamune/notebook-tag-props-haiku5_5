package com.amgm.notebook.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.amgm.notebook.ui.theme.extraColors

/** タグの色一式。[background] は淡い地、[content] は文字、[dot] は小さな丸印。 */
@Immutable
data class TagColor(val background: Color, val content: Color, val dot: Color)

/** タグ名のハッシュで決まる、彩度を抑えた 8 色のパレット（ライト/ダーク）。 */
object TagPalette {
    private val light = listOf(
        TagColor(Color(0xFFF7E1E0), Color(0xFF9B3F3C), Color(0xFFD9807C)),
        TagColor(Color(0xFFFAEBD2), Color(0xFF8A5A10), Color(0xFFE0A23C)),
        TagColor(Color(0xFFEBEFD0), Color(0xFF5E6B1A), Color(0xFFA3B34A)),
        TagColor(Color(0xFFDCF0E0), Color(0xFF2D6B43), Color(0xFF5DB07A)),
        TagColor(Color(0xFFD8EEF0), Color(0xFF1F6470), Color(0xFF55AEBB)),
        TagColor(Color(0xFFDDE6F8), Color(0xFF33508F), Color(0xFF7D9BDB)),
        TagColor(Color(0xFFE7E0F5), Color(0xFF5B4290), Color(0xFF9C84D6)),
        TagColor(Color(0xFFF6DFEC), Color(0xFF8E3A6C), Color(0xFFD783B3))
    )

    private val dark = listOf(
        TagColor(Color(0xFF3B2323), Color(0xFFF0B3AF), Color(0xFFE58E89)),
        TagColor(Color(0xFF3A2F1A), Color(0xFFF0CC8A), Color(0xFFE8B04F)),
        TagColor(Color(0xFF31361C), Color(0xFFD2DC8E), Color(0xFFB5C45A)),
        TagColor(Color(0xFF1F3427), Color(0xFFA8DDB9), Color(0xFF6CC088)),
        TagColor(Color(0xFF1D3438), Color(0xFFA3DCE4), Color(0xFF62BDCB)),
        TagColor(Color(0xFF232C45), Color(0xFFB3C6F2), Color(0xFF8CA9E8)),
        TagColor(Color(0xFF2D2642), Color(0xFFCDBFF0), Color(0xFFA891E0)),
        TagColor(Color(0xFF3B2535), Color(0xFFF0BBDA), Color(0xFFE08DBF))
    )

    /** 8 色のうち [name] に対応する色のインデックス（0..7）。同じ名前は常に同じ色。 */
    fun indexFor(name: String): Int = (name.hashCode() and Int.MAX_VALUE) % light.size

    fun colorFor(name: String, darkTheme: Boolean): TagColor =
        (if (darkTheme) dark else light)[indexFor(name)]
}

/** 現在のテーマ（ライト/ダーク）に合わせた、[name] のタグ色。 */
@Composable
fun rememberTagColor(name: String): TagColor =
    TagPalette.colorFor(name, MaterialTheme.extraColors.isDark)

/** タグに属さない中立のチップ色（「すべて」「+n」「型」など）。 */
@Composable
fun neutralTagColor(): TagColor = TagColor(
    background = MaterialTheme.colorScheme.surfaceVariant,
    content = MaterialTheme.colorScheme.onSurfaceVariant,
    dot = MaterialTheme.colorScheme.outline
)
