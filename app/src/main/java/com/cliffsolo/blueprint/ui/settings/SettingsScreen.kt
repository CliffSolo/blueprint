package com.cliffsolo.blueprint.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cliffsolo.blueprint.BlueprintApp
import com.cliffsolo.blueprint.data.AppSettings
import com.cliffsolo.blueprint.data.VoiceRole
import com.cliffsolo.blueprint.tts.VoiceOption
import com.cliffsolo.blueprint.ui.Glyphs
import kotlinx.coroutines.launch

private val SAMPLES = mapOf(
    VoiceRole.Narrator to "The piston slides down, and the intake valve opens.",
    VoiceRole.Notes to "Tip: squeezed mix burns hotter and pushes harder.",
    VoiceRole.Code to "Line three adds the price to the running total.",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(app: BlueprintApp, onBack: () -> Unit) {
    val s by app.settings.settings.collectAsStateWithLifecycle(AppSettings())
    val voices by app.narrator.voices.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var picking by remember { mutableStateOf<VoiceRole?>(null) }
    var owner by remember(s.githubOwner) { mutableStateOf(s.githubOwner) }
    val colors = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Glyphs.Back, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Section("VOICES") }
            item {
                Text(
                    if (voices.isEmpty()) "No voices found yet. Install voices in Android Settings → Text-to-speech."
                    else "${voices.size} voices on this phone. Pick a different one for each role.",
                    style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
                )
            }
            items(VoiceRole.entries) { role ->
                val chosen = voices.firstOrNull { it.name == s.voices[role] }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.surface)
                        .border(1.dp, colors.outline, RoundedCornerShape(12.dp)).clickable { picking = role }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(role.label, style = MaterialTheme.typography.titleMedium)
                        Text(role.hint, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    Text(chosen?.label ?: "Phone default", style = MaterialTheme.typography.labelSmall, color = colors.primary)
                }
            }
            item {
                LabeledSlider("Speed", s.rate, 0.6f..1.6f, "%.1f×") { v -> scope.launch { app.settings.setRate(v) } }
                LabeledSlider("Pitch", s.pitch, 0.7f..1.3f, "%.1f") { v -> scope.launch { app.settings.setPitch(v) } }
            }
            item {
                Toggle("Follow along", "Scroll to whatever is being read", s.followAlong) { v -> scope.launch { app.settings.setFollowAlong(v) } }
            }

            item { Section("COURSES") }
            item {
                Toggle("Check for new courses", "A few times a day, on any network", s.autoSync) { v -> scope.launch { app.settings.setAutoSync(v) } }
            }
            item {
                OutlinedTextField(
                    value = owner, onValueChange = { owner = it },
                    label = { Text("GitHub account to load courses from") },
                    supportingText = { Text("Public repos tagged \"${AppSettings.COURSE_TOPIC}\". No sign-in needed.") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { scope.launch { app.settings.setOwner(owner) } }, enabled = owner.isNotBlank() && owner != s.githubOwner) {
                    Text("Save")
                }
            }
            item { Section("ABOUT") }
            item {
                Text(
                    "Blueprint 0.1.0 · Kotlin + Jetpack Compose. Courses are open source on GitHub; labs run offline once downloaded.",
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                )
            }
        }
    }

    picking?.let { role ->
        VoicePicker(
            role = role, voices = voices, selected = s.voices[role],
            onPreview = { name -> app.narrator.preview(name, SAMPLES.getValue(role)) },
            onPick = { name -> scope.launch { app.settings.setVoice(role, name) }; picking = null },
            onDismiss = { picking = null },
        )
    }
}

@Composable
private fun VoicePicker(
    role: VoiceRole, voices: List<VoiceOption>, selected: String?,
    onPreview: (String?) -> Unit, onPick: (String?) -> Unit, onDismiss: () -> Unit,
) {
    var current by remember { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${role.label} voice") },
        text = {
            LazyColumn {
                item { VoiceRow("Phone default", "", current == null) { current = null; onPreview(null) } }
                items(voices, key = { it.name }) { v ->
                    VoiceRow(v.label, if (v.offline) "offline" else "needs internet", current == v.name) { current = v.name; onPreview(v.name) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(current) }) { Text("Use this voice") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun VoiceRow(label: String, note: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Column {
            Text(label)
            if (note.isNotBlank()) Text(note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun Toggle(title: String, hint: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, format: String, onChange: (Float) -> Unit) {
    var v by remember(value) { mutableStateOf(value) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.padding(end = 12.dp), style = MaterialTheme.typography.titleMedium)
        Slider(value = v, onValueChange = { v = it }, onValueChangeFinished = { onChange(v) }, valueRange = range, modifier = Modifier.weight(1f))
        Text(format.format(v), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 8.dp))
    }
}
