package com.cliffsolo.blueprint.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Who is speaking. Each role can have its own voice. */
enum class VoiceRole(val label: String, val hint: String) {
    Narrator("Narrator", "Lesson text"),
    Notes("Tips and notes", "Callouts, examples and math"),
    Code("Code walk-through", "Line-by-line code explanations"),
}

data class AppSettings(
    val voices: Map<VoiceRole, String> = emptyMap(),
    val rate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val githubOwner: String = DEFAULT_OWNER,
    val autoSync: Boolean = true,
    val followAlong: Boolean = true,
) {
    companion object {
        const val DEFAULT_OWNER = "CliffSolo"
        const val COURSE_TOPIC = "blueprint-course"
    }
}

private val Context.dataStore by preferencesDataStore("settings")

class SettingsRepo(private val context: Context) {
    private fun voiceKey(role: VoiceRole) = stringPreferencesKey("voice_${role.name}")
    private val rateKey = floatPreferencesKey("rate")
    private val pitchKey = floatPreferencesKey("pitch")
    private val ownerKey = stringPreferencesKey("github_owner")
    private val autoSyncKey = booleanPreferencesKey("auto_sync")
    private val followKey = booleanPreferencesKey("follow_along")

    val settings: Flow<AppSettings> = context.dataStore.data.map { p -> p.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    private fun Preferences.toSettings() = AppSettings(
        voices = VoiceRole.entries.mapNotNull { role -> this[voiceKey(role)]?.let { role to it } }.toMap(),
        rate = this[rateKey] ?: 1.0f,
        pitch = this[pitchKey] ?: 1.0f,
        githubOwner = this[ownerKey]?.takeIf { it.isNotBlank() } ?: AppSettings.DEFAULT_OWNER,
        autoSync = this[autoSyncKey] ?: true,
        followAlong = this[followKey] ?: true,
    )

    suspend fun setVoice(role: VoiceRole, name: String?) = context.dataStore.edit {
        if (name == null) it.remove(voiceKey(role)) else it[voiceKey(role)] = name
    }
    suspend fun setRate(v: Float) = context.dataStore.edit { it[rateKey] = v }
    suspend fun setPitch(v: Float) = context.dataStore.edit { it[pitchKey] = v }
    suspend fun setOwner(v: String) = context.dataStore.edit { it[ownerKey] = v.trim() }
    suspend fun setAutoSync(v: Boolean) = context.dataStore.edit { it[autoSyncKey] = v }
    suspend fun setFollowAlong(v: Boolean) = context.dataStore.edit { it[followKey] = v }
}
