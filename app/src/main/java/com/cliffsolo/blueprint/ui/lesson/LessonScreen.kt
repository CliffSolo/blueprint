package com.cliffsolo.blueprint.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cliffsolo.blueprint.BlueprintApp
import com.cliffsolo.blueprint.data.AppSettings
import com.cliffsolo.blueprint.data.Block
import com.cliffsolo.blueprint.ui.Glyphs
import com.cliffsolo.blueprint.ui.theme.parseHex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(app: BlueprintApp, vm: LessonViewModel, onBack: () -> Unit, onNextLesson: (String) -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val speech by vm.narration.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle(AppSettings())
    val colors = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val course = ui.course
    val lesson = ui.lesson
    val accent = parseHex(course?.manifest?.accent ?: "", colors.primary)
    val list = rememberLazyListState()
    val spokenCount = remember(lesson) { lesson?.let { LessonViewModel.utterancesFor(it).size } ?: 0 }

    // Follow along: keep the block being read on screen.
    val spokenBlock = speech.current?.block
    LaunchedEffect(spokenBlock) {
        if (spokenBlock != null && speech.playing && settings.followAlong) list.animateScrollToItem(spokenBlock + 1)
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        course?.manifest?.title.orEmpty(), style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant, maxLines = 1,
                    )
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Glyphs.Back, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
            )
        },
        bottomBar = {
            if (lesson != null) NarrationBar(
                playing = speech.playing,
                ready = speech.ready,
                progress = if (speech.index < 0) 0f else (speech.index + 1f) / spokenCount.coerceAtLeast(1),
                accent = accent,
                onPlay = vm::togglePlay, onNext = vm::next, onPrevious = vm::previous,
            )
        },
    ) { pad ->
        if (ui.error != null) {
            Text(ui.error!!, modifier = Modifier.padding(pad).padding(16.dp))
            return@Scaffold
        }
        if (lesson == null || course == null) return@Scaffold
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    "LESSON ${ui.number} OF ${course.manifest.lessons.size} · ${lesson.minutes} MIN",
                    style = MaterialTheme.typography.labelSmall, color = accent,
                )
                Text(lesson.title, style = MaterialTheme.typography.headlineLarge, color = colors.onBackground)
                Text(
                    "Long-press any paragraph to read aloud from there.",
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                )
            }
            itemsIndexed(lesson.blocks) { i, block ->
                val current = speech.current
                val spoken = Spoken(active = current?.block == i, line = current?.line ?: -1, range = speech.range)
                val readable = block !is Block.Lab && block !is Block.Quiz
                Box(
                    Modifier.fillMaxWidth()
                        .then(
                            if (readable) Modifier.combinedClickable(
                                interactionSource = remember { MutableInteractionSource() }, indication = null,
                                onClick = {}, onLongClick = { vm.readFrom(i) },
                            ) else Modifier,
                        ),
                ) {
                    if (spoken.active && block !is Block.Lab) {
                        Box(Modifier.align(Alignment.CenterStart).padding(start = 0.dp).width(3.dp).height(24.dp).background(accent))
                    }
                    Box(Modifier.padding(start = if (spoken.active) 10.dp else 0.dp)) {
                        when (block) {
                            is Block.Heading -> HeadingBlock(block)
                            is Block.Text -> TextBlock(block, spoken)
                            is Block.Callout -> CalloutBlock(block, spoken, accent)
                            is Block.Steps -> StepsBlock(block, spoken)
                            is Block.Math -> MathBlock(block)
                            is Block.Lab -> LabBlock(block, app.store.labUrl(course, block, dark))
                            is Block.Quiz -> QuizBlock(block, ui.answers[i]) { vm.answer(i, it) }
                            is Block.Code -> CodeBlock(block, spoken)
                            is Block.Unsupported -> Text(
                                "This part needs a newer version of Blueprint.",
                                color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { vm.finish { ui.nextLessonId?.let(onNextLesson) ?: onBack() } },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                ) {
                    Icon(Glyphs.Check, null)
                    Text(if (ui.nextLessonId != null) "  Done · next lesson" else "  Finish blueprint", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NarrationBar(
    playing: Boolean, ready: Boolean, progress: Float, accent: Color,
    onPlay: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surface, tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Column(Modifier.navigationBarsPadding()) {
            LinearProgressIndicator(
                progress = { progress }, modifier = Modifier.fillMaxWidth().height(3.dp),
                color = accent, trackColor = colors.surfaceVariant, drawStopIndicator = {},
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Glyphs.Speaker, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Text(
                    if (!ready) "Loading voice…" else if (playing) "Reading aloud" else "Listen to this lesson",
                    style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                IconButton(onClick = onPrevious, enabled = ready) { Icon(Glyphs.Previous, "Previous") }
                FilledIconButton(
                    onClick = onPlay, enabled = ready, modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = accent, contentColor = Color.White),
                ) { Icon(if (playing) Glyphs.Pause else Glyphs.Play, if (playing) "Pause" else "Play") }
                IconButton(onClick = onNext, enabled = ready) { Icon(Glyphs.Next, "Next") }
            }
        }
    }
}
