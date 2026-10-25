package com.cliffsolo.blueprint.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Tiny inline markdown: **bold**, *italic* and `code`. Course text only needs these.
 * The plain text (markers removed) is what gets spoken, so highlight ranges from the
 * speech engine line up with the displayed text exactly.
 */
object Markdown {
    private val token = Regex("""\*\*(.+?)\*\*|`(.+?)`|\*(.+?)\*""")

    fun plain(src: String): String = token.replace(src) { m -> m.groupValues.drop(1).first { it.isNotEmpty() } }

    fun annotate(src: String, codeBg: Color, highlight: IntRange? = null, highlightBg: Color = Color.Unspecified): AnnotatedString {
        val text = buildAnnotatedString {
            var last = 0
            token.findAll(src).forEach { m ->
                append(src.substring(last, m.range.first))
                val (bold, code, italic) = m.destructured
                when {
                    bold.isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
                    code.isNotEmpty() -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)) { append(code) }
                    else -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(italic) }
                }
                last = m.range.last + 1
            }
            append(src.substring(last))
        }
        if (highlight == null || highlight.isEmpty() || highlight.last >= text.length) return text
        return buildAnnotatedString {
            append(text)
            addStyle(SpanStyle(background = highlightBg), highlight.first, highlight.last + 1)
        }
    }
}
