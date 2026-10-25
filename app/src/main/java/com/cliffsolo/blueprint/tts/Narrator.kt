package com.cliffsolo.blueprint.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.cliffsolo.blueprint.data.AppSettings
import com.cliffsolo.blueprint.data.VoiceRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** One thing to say: [block] is the lesson block it belongs to, [line] a code line to highlight (or -1). */
data class Utterance(val block: Int, val text: String, val role: VoiceRole, val line: Int = -1)

data class NarrationState(
    val ready: Boolean = false,
    val playing: Boolean = false,
    val index: Int = -1,
    val current: Utterance? = null,
    /** Character range being spoken right now, for word-by-word highlighting. */
    val range: IntRange? = null,
)

data class VoiceOption(val name: String, val label: String, val offline: Boolean)

/**
 * Reads a lesson aloud with Android's built-in text-to-speech.
 * Speaks one utterance at a time so it can switch voices per role, skip, and resume.
 */
class Narrator(context: Context) {
    private val _state = MutableStateFlow(NarrationState())
    val state: StateFlow<NarrationState> = _state.asStateFlow()

    private val _voices = MutableStateFlow<List<VoiceOption>>(emptyList())
    val voices: StateFlow<List<VoiceOption>> = _voices.asStateFlow()

    private var queue: List<Utterance> = emptyList()
    private var settings = AppSettings()
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setOnUtteranceProgressListener(listener)
                _voices.value = loadVoices()
                _state.value = _state.value.copy(ready = true)
            }
        }
    }

    fun applySettings(s: AppSettings) {
        settings = s
    }

    fun load(items: List<Utterance>) {
        stop()
        queue = items
        _state.value = _state.value.copy(index = -1, current = null, range = null)
    }

    fun play(from: Int = _state.value.index.coerceAtLeast(0)) {
        if (queue.isEmpty()) return
        speak(from.coerceIn(0, queue.lastIndex))
    }

    /** Starts reading at the first utterance belonging to [block]. */
    fun playBlock(block: Int) {
        val i = queue.indexOfFirst { it.block >= block }
        if (i >= 0) speak(i)
    }

    fun pause() {
        tts?.stop()
        _state.value = _state.value.copy(playing = false, range = null)
    }

    fun stop() {
        tts?.stop()
        _state.value = _state.value.copy(playing = false, index = -1, current = null, range = null)
    }

    fun next() = if (_state.value.index < queue.lastIndex) speak(_state.value.index + 1) else stop()
    fun previous() = speak((_state.value.index - 1).coerceAtLeast(0))

    /** Says a short sample in the chosen voice; used by the voice picker. */
    fun preview(voiceName: String?, text: String) {
        val engine = tts ?: return
        engine.stop()
        applyVoice(voiceName)
        engine.setSpeechRate(settings.rate)
        engine.setPitch(settings.pitch)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "preview")
    }

    private fun speak(i: Int) {
        val engine = tts ?: return
        val u = queue.getOrNull(i) ?: return
        engine.stop()
        applyVoice(settings.voices[u.role])
        engine.setSpeechRate(settings.rate)
        engine.setPitch(settings.pitch)
        _state.value = _state.value.copy(playing = true, index = i, current = u, range = null)
        engine.speak(u.text, TextToSpeech.QUEUE_FLUSH, null, "u$i")
    }

    private fun applyVoice(name: String?) {
        val engine = tts ?: return
        val voice = name?.let { n -> engine.voices?.firstOrNull { it.name == n } }
        if (voice != null) engine.voice = voice else engine.language = Locale.getDefault()
    }

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {}

        override fun onRangeStart(utteranceId: String, start: Int, end: Int, frame: Int) {
            if (utteranceId == "u${_state.value.index}") _state.value = _state.value.copy(range = start until end)
        }

        override fun onDone(utteranceId: String) {
            val i = utteranceId.removePrefix("u").toIntOrNull() ?: return
            if (i != _state.value.index || !_state.value.playing) return
            if (i < queue.lastIndex) speak(i + 1) else _state.value = _state.value.copy(playing = false, range = null)
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String) {
            _state.value = _state.value.copy(playing = false, range = null)
        }
    }

    private fun loadVoices(): List<VoiceOption> {
        val lang = Locale.getDefault().language
        return tts?.voices.orEmpty()
            .filter { it.locale.language == lang && !it.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
            .sortedWith(compareBy<Voice>({ it.isNetworkConnectionRequired }, { it.locale.displayCountry }, { it.name }))
            .map { VoiceOption(it.name, describe(it), !it.isNetworkConnectionRequired) }
    }

    private fun describe(v: Voice): String {
        // Google voice names look like "en-us-x-iob-local"; the 3-letter code is the speaker.
        val speaker = Regex("-x-([a-z]+)").find(v.name)?.groupValues?.get(1)?.uppercase() ?: v.name
        val country = v.locale.displayCountry.ifBlank { v.locale.displayLanguage }
        val quality = if (v.quality >= Voice.QUALITY_HIGH) " · HD" else ""
        return "$country · $speaker$quality"
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}
