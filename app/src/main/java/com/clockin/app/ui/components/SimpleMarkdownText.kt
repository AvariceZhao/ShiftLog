package com.clockin.app.ui.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.clockin.app.ui.theme.AmberPrimary
import com.clockin.app.ui.theme.TextSecondary

@Composable
fun SimpleMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val annotated = remember(markdown) { parseSimpleMarkdown(markdown) }

    ClickableText(
        text = annotated,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
        onClick = { offset ->
            annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()
                ?.let { uriHandler.openUri(it.item) }
        },
    )
}

private fun parseSimpleMarkdown(source: String): AnnotatedString = buildAnnotatedString {
    source.lines().forEachIndexed { index, rawLine ->
        if (index > 0) append('\n')
        val line = rawLine.trimEnd()
        when {
            line.startsWith("### ") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                    appendInline(line.removePrefix("### "))
                }
            }
            line.startsWith("## ") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    appendInline(line.removePrefix("## "))
                }
            }
            line.startsWith("# ") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    appendInline(line.removePrefix("# "))
                }
            }
            line.matches(Regex("^[-*+]\\s+.*")) -> {
                append("• ")
                appendInline(line.replaceFirst(Regex("^[-*+]\\s+"), ""))
            }
            else -> appendInline(line)
        }
    }
}

private fun AnnotatedString.Builder.appendInline(text: String) {
    var index = 0
    while (index < text.length) {
        val linkStart = text.indexOf('[', index)
        if (linkStart == -1) {
            appendStyledSegment(text.substring(index))
            break
        }
        appendStyledSegment(text.substring(index, linkStart))
        val labelEnd = text.indexOf(']', linkStart)
        val urlStart = text.indexOf('(', labelEnd)
        val urlEnd = text.indexOf(')', urlStart)
        if (labelEnd != -1 && urlStart == labelEnd + 1 && urlEnd != -1) {
            val label = text.substring(linkStart + 1, labelEnd)
            val url = text.substring(urlStart + 1, urlEnd)
            pushStringAnnotation(tag = "URL", annotation = url)
            withStyle(
                SpanStyle(
                    color = AmberPrimary,
                    textDecoration = TextDecoration.Underline,
                ),
            ) {
                append(label)
            }
            pop()
            index = urlEnd + 1
        } else {
            appendStyledSegment(text.substring(linkStart))
            break
        }
    }
}

private fun AnnotatedString.Builder.appendStyledSegment(segment: String) {
    var index = 0
    while (index < segment.length) {
        val boldStart = segment.indexOf("**", index)
        if (boldStart == -1) {
            append(segment.substring(index))
            break
        }
        append(segment.substring(index, boldStart))
        val boldEnd = segment.indexOf("**", boldStart + 2)
        if (boldEnd == -1) {
            append(segment.substring(boldStart))
            break
        }
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            append(segment.substring(boldStart + 2, boldEnd))
        }
        index = boldEnd + 2
    }
}
