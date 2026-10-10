package com.amgm.notebook.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.amgm.notebook.markdown.MarkdownBlock
import com.amgm.notebook.markdown.MarkdownDocument
import com.amgm.notebook.markdown.MarkdownInline

/** [MarkdownDocument] を Compose で描画する。 */
@Composable
fun MarkdownView(document: MarkdownDocument, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        document.blocks.forEach { block -> MarkdownBlockView(block) }
    }
}

@Composable
private fun MarkdownBlockView(block: MarkdownBlock) {
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    when (block) {
        is MarkdownBlock.Heading -> {
            val style = when (block.level) {
                1 -> MaterialTheme.typography.headlineSmall
                2 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            }
            val text = remember(block, codeBackground) { inlinesToAnnotated(block.inlines, codeBackground) }
            Text(text = text, style = style.copy(fontWeight = FontWeight.Bold))
        }
        is MarkdownBlock.Paragraph -> {
            val text = remember(block, codeBackground) { inlinesToAnnotated(block.inlines, codeBackground) }
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
        is MarkdownBlock.CodeBlock -> {
            Surface(
                color = codeBackground,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = block.code,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp)
                )
            }
        }
        is MarkdownBlock.BulletList -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                block.items.forEach { item ->
                    Row {
                        Text("•  ", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = inlinesToAnnotated(item, codeBackground),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
        is MarkdownBlock.OrderedList -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                block.items.forEachIndexed { index, item ->
                    Row {
                        Text("${block.start + index}.  ", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = inlinesToAnnotated(item, codeBackground),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
        is MarkdownBlock.Quote -> {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Spacer(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    block.blocks.forEach { inner -> MarkdownBlockView(inner) }
                }
            }
        }
        is MarkdownBlock.ThematicBreak -> HorizontalDivider()
    }
}

internal fun inlinesToAnnotated(inlines: List<MarkdownInline>, codeBackground: Color): AnnotatedString =
    buildAnnotatedString { appendInlines(inlines, codeBackground) }

private fun AnnotatedString.Builder.appendInlines(inlines: List<MarkdownInline>, codeBackground: Color) {
    for (inline in inlines) {
        when (inline) {
            is MarkdownInline.Text -> append(inline.text)
            is MarkdownInline.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                appendInlines(inline.children, codeBackground)
            }
            is MarkdownInline.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                appendInlines(inline.children, codeBackground)
            }
            is MarkdownInline.Code -> withStyle(
                SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)
            ) {
                append(inline.text)
            }
            is MarkdownInline.LineBreak -> append("\n")
        }
    }
}
