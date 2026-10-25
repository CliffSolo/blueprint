package com.cliffsolo.blueprint.ui.course

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.cliffsolo.blueprint.BlueprintApp
import com.cliffsolo.blueprint.data.InstalledCourse
import com.cliffsolo.blueprint.ui.Glyphs
import com.cliffsolo.blueprint.ui.theme.Mono
import com.cliffsolo.blueprint.ui.theme.blueprintGrid
import com.cliffsolo.blueprint.ui.theme.parseHex
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CourseUi(val course: InstalledCourse?, val done: Set<String>, val lastLesson: String?)

class CourseViewModel(app: BlueprintApp, courseId: String) : ViewModel() {
    val state: StateFlow<CourseUi> = combine(
        app.store.courses.map { list -> list.firstOrNull { it.id == courseId } },
        app.db.progress().progressFor(courseId),
        app.db.progress().courseStates(),
    ) { course, progress, states ->
        CourseUi(course, progress.map { it.lessonId }.toSet(), states.firstOrNull { it.courseId == courseId }?.lastLessonId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CourseUi(null, emptySet(), null))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScreen(vm: CourseViewModel, onBack: () -> Unit, onLesson: (String) -> Unit) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val course = ui.course ?: return
    val m = course.manifest
    val colors = MaterialTheme.colorScheme
    val accent = parseHex(m.accent, colors.primary)
    // Continue where you left off: the first unfinished lesson after the last one opened.
    val next = m.lessons.firstOrNull { it.id !in ui.done }?.id ?: m.lessons.first().id
    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onBack) { Icon(Glyphs.Back, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().blueprintGrid(colors.outlineVariant),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text("BLUEPRINT · ${m.minutes} MIN", style = MaterialTheme.typography.labelSmall, color = accent)
                Text(m.title, style = MaterialTheme.typography.headlineLarge, color = colors.onBackground)
                if (m.subtitle.isNotBlank()) Text(m.subtitle, style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Text(m.description, style = MaterialTheme.typography.bodyLarge, color = colors.onBackground)
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { onLesson(next) },
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Glyphs.Play, null)
                    Text(
                        when {
                            ui.done.isEmpty() -> "  Start"
                            ui.done.size == m.lessons.size -> "  Review from the start"
                            else -> "  Continue"
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("LESSONS", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            itemsIndexed(m.lessons, key = { _, l -> l.id }) { i, lesson ->
                val done = lesson.id in ui.done
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.surface)
                        .border(1.dp, if (lesson.id == next && !done) accent else colors.outline, RoundedCornerShape(12.dp))
                        .clickable { onLesson(lesson.id) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(if (done) accent else colors.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) Icon(Glyphs.Check, "Done", tint = Color.White, modifier = Modifier.size(18.dp))
                        else Text("${i + 1}", fontFamily = Mono, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(lesson.title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                        Text("${lesson.minutes} min", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
