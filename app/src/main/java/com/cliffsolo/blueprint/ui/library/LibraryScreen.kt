package com.cliffsolo.blueprint.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.cliffsolo.blueprint.ui.theme.blueprintGrid
import com.cliffsolo.blueprint.ui.theme.parseHex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CourseCard(val course: InstalledCourse, val done: Int, val total: Int)

data class LibraryState(val cards: List<CourseCard> = emptyList(), val syncing: Boolean = false, val message: String? = null)

class LibraryViewModel(private val app: BlueprintApp) : ViewModel() {
    private val syncing = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<LibraryState> = combine(app.store.courses, app.db.progress().allProgress(), syncing, message) { courses, progress, s, m ->
        val doneByCourse = progress.groupBy { it.courseId }
        LibraryState(
            cards = courses.map { c ->
                val ids = c.manifest.lessons.map { it.id }.toSet()
                CourseCard(c, doneByCourse[c.id].orEmpty().count { it.lessonId in ids }, ids.size)
            },
            syncing = s,
            message = m,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryState())

    fun sync() {
        if (syncing.value) return
        viewModelScope.launch {
            syncing.value = true
            message.value = runCatching { app.sync.sync(app.settings.current().courseRepo).message }
                .getOrElse { "Couldn't reach GitHub. Your courses still work offline." }
            syncing.value = false
        }
    }

    fun messageShown() { message.value = null }
}

@Composable
fun LibraryScreen(vm: LibraryViewModel, onOpen: (String) -> Unit, onSettings: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); vm.messageShown() }
    }
    val colors = MaterialTheme.colorScheme
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = colors.background,
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().blueprintGrid(colors.outlineVariant),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding() + 8.dp, bottom = pad.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("BLUEPRINT", style = MaterialTheme.typography.labelSmall, color = colors.primary)
                        Text("See how things work", style = MaterialTheme.typography.headlineSmall, color = colors.onBackground)
                    }
                    IconButton(onClick = vm::sync, enabled = !state.syncing) {
                        if (state.syncing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Glyphs.Refresh, contentDescription = "Check for new courses")
                    }
                    IconButton(onClick = onSettings) { Icon(Glyphs.Tune, contentDescription = "Settings") }
                }
            }
            item {
                Text(
                    "${state.cards.size} ${if (state.cards.size == 1) "blueprint" else "blueprints"} on this phone · works offline",
                    style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant,
                )
            }
            if (state.cards.isEmpty()) {
                item { EmptyLibrary() }
            }
            items(state.cards, key = { it.course.id }) { card -> CourseCardView(card) { onOpen(card.course.id) } }
        }
    }
}

@Composable
private fun CourseCardView(card: CourseCard, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val m = card.course.manifest
    val accent = parseHex(m.accent, colors.primary)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
    ) {
        Box(Modifier.width(6.dp).fillMaxHeight().background(accent))
        Column(Modifier.padding(14.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(m.title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, modifier = Modifier.weight(1f))
                if (card.course.isDemo) Tag("DEMO", accent)
            }
            if (m.subtitle.isNotBlank()) Text(m.subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${m.minutes} min · ${card.total} lessons",
                    style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f),
                )
                Text(
                    if (card.done == card.total && card.total > 0) "Complete ✓" else "${card.done}/${card.total}",
                    style = MaterialTheme.typography.labelSmall, color = if (card.done == card.total) accent else colors.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { if (card.total == 0) 0f else card.done / card.total.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = accent, trackColor = colors.surfaceVariant, drawStopIndicator = {},
            )
        }
    }
}

@Composable
fun Tag(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color,
        modifier = Modifier.border(1.dp, color, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun EmptyLibrary() {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No blueprints yet", style = MaterialTheme.typography.titleMedium)
        Text("Tap ↻ to check GitHub for courses.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
