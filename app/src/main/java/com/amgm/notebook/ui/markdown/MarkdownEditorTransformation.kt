package com.amgm.notebook.ui.markdown

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.amgm.notebook.markdown.MarkdownHighlighter
import com.amgm.notebook.markdown.MarkdownStyle

/**
 * Markdown の記法を、編集欄の見た目に反映させる。
 *
 * 文字列は一切変えない（[OffsetMapping.Identity]）ため、カーソル位置・IME・保存される本文は
 * 通常のテキスト入力と同じ挙動になる。記号は薄く残し、本文に太字・斜体・見出しなどの装飾を重ねる。
 */
class MarkdownEditorVisualTransformation(
    private val styles: Map<MarkdownStyle, SpanStyle>,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val plain = text.text
        val spans = MarkdownHighlighter.highlight(plain)
        if (spans.isEmpty()) return TransformedText(AnnotatedString(plain), OffsetMapping.Identity)
        val annotated = buildAnnotatedString {
            append(plain)
            spans.forEach { span ->
                val style = styles[span.style] ?: return@forEach
                addStyle(style, span.start, span.end)
            }
        }
        return TransformedText(annotated, OffsetMapping.Identity)
    }
}

/** テーマの色から作った装飾を保持し、色が変わらない限り同じ変換を使い回す。 */
@Composable
fun rememberMarkdownEditorTransformation(): VisualTransformation {
    val scheme = MaterialTheme.colorScheme
    val marker = scheme.onSurfaceVariant.copy(alpha = 0.5f)
    return remember(scheme.primary, scheme.secondary, scheme.onSurfaceVariant) {
        MarkdownEditorVisualTransformation(markdownEditorStyles(scheme.primary, scheme.secondary, scheme.onSurfaceVariant, marker))
    }
}

internal fun markdownEditorStyles(
    primary: Color,
    secondary: Color,
    muted: Color,
    marker: Color,
): Map<MarkdownStyle, SpanStyle> = mapOf(
    MarkdownStyle.Marker to SpanStyle(color = marker),
    MarkdownStyle.Heading to SpanStyle(fontWeight = FontWeight.Bold, color = primary),
    MarkdownStyle.Bold to SpanStyle(fontWeight = FontWeight.Bold),
    MarkdownStyle.Italic to SpanStyle(fontStyle = FontStyle.Italic),
    MarkdownStyle.Code to SpanStyle(fontFamily = FontFamily.Monospace, color = secondary),
    MarkdownStyle.CodeBlock to SpanStyle(fontFamily = FontFamily.Monospace, color = secondary),
    MarkdownStyle.Quote to SpanStyle(fontStyle = FontStyle.Italic, color = muted),
)
