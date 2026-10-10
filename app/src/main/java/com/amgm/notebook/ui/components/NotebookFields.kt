package com.amgm.notebook.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.amgm.notebook.ui.lighting.LocalLightState
import com.amgm.notebook.ui.theme.NotebookDimens

/** 入力欄の角丸（14dp）。 */
val NotebookInputShape = RoundedCornerShape(NotebookDimens.InputRadius)

/**
 * 細い罫線の入力欄。塗りは面色の半透明にして、背景の光がうっすら透けるようにする
 * （ライト alpha 0.7、ダーク 0.5）。未入力のプレースホルダは薄い字。
 */
@Composable
fun notebookFieldColors(): TextFieldColors {
    val scheme = MaterialTheme.colorScheme
    val fill = scheme.surface.copy(alpha = if (LocalLightState.current.isDark) 0.5f else 0.7f)
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = fill,
        unfocusedContainerColor = fill,
        disabledContainerColor = fill,
        errorContainerColor = fill,
        focusedBorderColor = scheme.primary,
        unfocusedBorderColor = scheme.outlineVariant,
        errorBorderColor = scheme.error,
        cursorColor = scheme.primary,
        focusedPlaceholderColor = scheme.onSurfaceVariant.copy(alpha = 0.55f),
        unfocusedPlaceholderColor = scheme.onSurfaceVariant.copy(alpha = 0.55f),
        focusedLabelColor = scheme.primary,
        unfocusedLabelColor = scheme.onSurfaceVariant
    )
}

/** カード内に溶け込む、枠なしの入力欄（本文など）。 */
@Composable
fun notebookBorderlessFieldColors(): TextFieldColors {
    val scheme = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        errorContainerColor = Color.Transparent,
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        errorBorderColor = scheme.error,
        cursorColor = scheme.primary,
        focusedPlaceholderColor = scheme.onSurfaceVariant.copy(alpha = 0.55f),
        unfocusedPlaceholderColor = scheme.onSurfaceVariant.copy(alpha = 0.55f)
    )
}
