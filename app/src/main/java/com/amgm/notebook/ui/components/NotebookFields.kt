package com.amgm.notebook.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.amgm.notebook.ui.theme.NotebookDimens

/** 入力欄の角丸（14dp）。 */
val NotebookInputShape = RoundedCornerShape(NotebookDimens.InputRadius)

/** 紙色の地に細い罫線の入力欄。未入力のプレースホルダは薄い字。 */
@Composable
fun notebookFieldColors(): TextFieldColors {
    val scheme = MaterialTheme.colorScheme
    return OutlinedTextFieldDefaults.colors(
        focusedContainerColor = scheme.background,
        unfocusedContainerColor = scheme.background,
        disabledContainerColor = scheme.background,
        errorContainerColor = scheme.background,
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
