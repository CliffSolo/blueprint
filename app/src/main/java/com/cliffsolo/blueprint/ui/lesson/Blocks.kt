package com.cliffsolo.blueprint.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cliffsolo.blueprint.data.Block
import com.cliffsolo.blueprint.ui.Markdown
import com.cliffsolo.blueprint.ui.theme.Mono

/** What the narrator is doing to this block right now. */
data class Spoken(val active: Boolean, val line: Int = -1, val range: IntRange? = null)

private val textSize = 17.sp
private val textLine = 26.sp

@Composable
private fun rich(src: String, range: IntRange?) = Markdown.annotate(
    src,
    codeBg = MaterialTheme.colorScheme.surfaceVariant,
    highlight = range,
    highlightBg = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
)

@Composable
fun HeadingBlock(b: Block.Heading) {
    Text(
        b.text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
fun TextBlock(b: Block.Text, spoken: Spoken) {
    Text(
        rich(b.text, spoken.range.takeIf { spoken.active }), fontSize = textSize, lineHeight = textLine,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
fun CalloutBlock(b: Block.Callout, spoken: Spoken, accent: Color) {
    val colors = MaterialTheme.colorScheme
    val (icon, tint) = when (b.style) {
        "warn" -> "⚠" to Color(0xFFE0A100)
        "math" -> "∑" to colors.primary
        "example" -> "◎" to accent
        else -> "✦" to colors.primary
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(12.dp)).height(IntrinsicSize.Min),
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(tint))
        Column(Modifier.padding(14.dp)) {
            Text("$icon  ${b.title.ifBlank { "Note" }}", fontWeight = FontWeight.Bold, color = tint)
            Spacer(Modifier.height(4.dp))
            Text(rich(b.text, spoken.range.takeIf { spoken.active }), fontSize = 16.sp, lineHeight = 24.sp, color = colors.onSurface)
        }
    }
}

@Composable
fun BulletsBlock(b: Block.Bullets, spoken: Spoken) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (b.intro.isNotBlank()) {
            Text(
                rich(b.intro, spoken.range.takeIf { spoken.active && spoken.line == -1 }),
                fontSize = textSize, lineHeight = textLine, fontWeight = FontWeight.SemiBold, color = colors.onBackground,
            )
        }
        b.items.forEachIndexed { i, item ->
            val on = spoken.active && spoken.line == i
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(if (on) colors.primaryContainer else Color.Transparent).padding(horizontal = 6.dp, vertical = 3.dp),
            ) {
                Box(Modifier.padding(top = 10.dp).size(7.dp).clip(CircleShape).background(colors.secondary))
                Text(
                    rich(item, spoken.range.takeIf { on }), fontSize = 16.sp, lineHeight = 24.sp, color = colors.onBackground,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
fun StepsBlock(b: Block.Steps, spoken: Spoken) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        b.items.forEachIndexed { i, item ->
            val on = spoken.active && spoken.line == i
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(if (on) colors.primaryContainer else Color.Transparent).padding(6.dp),
            ) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(colors.primary), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", color = colors.onPrimary, fontFamily = Mono, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    rich(item, spoken.range.takeIf { on }), fontSize = 16.sp, lineHeight = 23.sp, color = colors.onBackground,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
    }
}

@Composable
fun MathBlock(b: Block.Math) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.primaryContainer).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        b.lines.forEach { Text(it, fontFamily = Mono, fontSize = 15.sp, lineHeight = 22.sp, color = colors.onPrimaryContainer) }
    }
}

@Composable
fun LabBlock(b: Block.Lab, url: String) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, colors.outline, RoundedCornerShape(14.dp)),
    ) {
        Text(
            "▶ LIVE LAB", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colors.primary,
            modifier = Modifier.fillMaxWidth().background(colors.surface).padding(horizontal = 12.dp, vertical = 6.dp),
        )
        LabView(url, b.height)
        if (b.caption.isNotBlank()) {
            Text(
                b.caption, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().background(colors.surface).padding(12.dp),
            )
        }
    }
}

@Composable
fun QuizBlock(b: Block.Quiz, picked: Int?, onPick: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val good = Color(0xFF2E9D5B)
    val bad = Color(0xFFD0453A)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("QUICK CHECK", style = MaterialTheme.typography.labelSmall, color = colors.secondary, fontWeight = FontWeight.Bold)
        Text(b.question, fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
        b.options.forEachIndexed { i, option ->
            val border = when {
                picked == null -> colors.outline
                i == b.answer -> good
                i == picked -> bad
                else -> colors.outline
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(if (picked != null && i == b.answer) good.copy(alpha = 0.12f) else Color.Transparent)
                    .border(if (border == colors.outline) 1.dp else 2.dp, border, RoundedCornerShape(10.dp))
                    .clickable(enabled = picked == null) { onPick(i) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${'A' + i}", fontFamily = Mono, fontWeight = FontWeight.Bold, color = colors.primary)
                Text(option, fontSize = 16.sp, color = colors.onSurface, modifier = Modifier.padding(start = 12.dp).weight(1f))
                if (picked != null && i == b.answer) Text("✓", color = good, fontWeight = FontWeight.Bold)
                else if (picked == i) Text("✗", color = bad, fontWeight = FontWeight.Bold)
            }
        }
        if (picked != null) {
            Text(
                (if (picked == b.answer) "Right! " else "Not quite. ") + b.explain,
                fontSize = 15.sp, lineHeight = 22.sp, color = if (picked == b.answer) good else colors.onSurface,
            )
        }
    }
}

@Composable
fun CodeBlock(b: Block.Code, spoken: Spoken) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.surfaceVariant).padding(vertical = 10.dp)) {
        b.lines.forEachIndexed { i, line ->
            val on = spoken.active && spoken.line == i + 1
            Row(Modifier.fillMaxWidth().background(if (on) colors.secondary.copy(alpha = 0.25f) else Color.Transparent).padding(horizontal = 10.dp)) {
                Text("${i + 1}".padStart(3), fontFamily = Mono, fontSize = 13.sp, color = colors.onSurfaceVariant)
                Text(line, fontFamily = Mono, fontSize = 13.sp, color = colors.onSurface, modifier = Modifier.padding(start = 10.dp))
            }
        }
    }
}
