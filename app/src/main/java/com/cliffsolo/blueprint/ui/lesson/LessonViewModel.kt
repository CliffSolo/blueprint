package com.cliffsolo.blueprint.ui.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cliffsolo.blueprint.BlueprintApp
import com.cliffsolo.blueprint.data.Block
import com.cliffsolo.blueprint.data.CourseState
import com.cliffsolo.blueprint.data.InstalledCourse
import com.cliffsolo.blueprint.data.Lesson
import com.cliffsolo.blueprint.data.LessonProgress
import com.cliffsolo.blueprint.data.VoiceRole
import com.cliffsolo.blueprint.tts.Utterance
import com.cliffsolo.blueprint.ui.Markdown
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

data class LessonUi(
    val course: InstalledCourse? = null,
    val lesson: Lesson? = null,
    val number: Int = 0,
    val nextLessonId: String? = null,
    /** Quiz block index → option the learner picked. */
    val answers: Map<Int, Int> = emptyMap(),
    val error: String? = null,
)

class LessonViewModel(private val app: BlueprintApp, private val courseId: String, private val lessonId: String) : ViewModel() {
    private val _ui = MutableStateFlow(LessonUi())
    val ui: StateFlow<LessonUi> = _ui.asStateFlow()
    val narration = app.narrator.state
    val settings = app.settings.settings

    init {
        viewModelScope.launch {
            val course = app.store.courses.filter { list -> list.any { it.id == courseId } }.first().first { it.id == courseId }
            val refs = course.manifest.lessons
            val index = refs.indexOfFirst { it.id == lessonId }.coerceAtLeast(0)
            runCatching { app.store.lesson(course, refs[index]) }
                .onSuccess { lesson ->
                    _ui.value = LessonUi(course, lesson, index + 1, refs.getOrNull(index + 1)?.id)
                    app.narrator.load(utterancesFor(lesson))
                    app.db.progress().saveCourseState(CourseState(courseId, lessonId, System.currentTimeMillis()))
                }
                .onFailure { _ui.value = LessonUi(course, error = "Couldn't open this lesson: ${it.message}") }
        }
    }

    fun answer(block: Int, option: Int) {
        if (block in _ui.value.answers) return
        _ui.value = _ui.value.copy(answers = _ui.value.answers + (block to option))
    }

    fun togglePlay() {
        val s = narration.value
        if (s.playing) app.narrator.pause() else app.narrator.play()
    }
    fun next() = app.narrator.next()
    fun previous() = app.narrator.previous()
    fun readFrom(block: Int) = app.narrator.playBlock(block)

    fun finish(onDone: () -> Unit) {
        val ui = _ui.value
        val lesson = ui.lesson ?: return
        val quizzes = lesson.blocks.withIndex().filter { it.value is Block.Quiz }
        val correct = quizzes.count { (i, b) -> ui.answers[i] == (b as Block.Quiz).answer }
        viewModelScope.launch {
            app.db.progress().saveProgress(LessonProgress(courseId, lesson.id, System.currentTimeMillis(), correct, quizzes.size))
            onDone()
        }
    }

    override fun onCleared() {
        app.narrator.stop()
    }

    companion object {
        /** Turns lesson blocks into the list of things the narrator will say, in order. */
        fun utterancesFor(lesson: Lesson): List<Utterance> = lesson.blocks.flatMapIndexed { i, block ->
            when (block) {
                is Block.Heading -> listOf(Utterance(i, block.text, VoiceRole.Narrator))
                is Block.Text -> listOf(Utterance(i, Markdown.plain(block.text), VoiceRole.Narrator))
                is Block.Callout -> listOf(Utterance(i, Markdown.plain(block.text), VoiceRole.Notes))
                is Block.Bullets ->
                    listOfNotNull(block.intro.takeIf { it.isNotBlank() }?.let { Utterance(i, Markdown.plain(it), VoiceRole.Narrator, -1) }) +
                        block.items.mapIndexed { n, item -> Utterance(i, Markdown.plain(item), VoiceRole.Narrator, n) }
                is Block.Steps -> block.items.mapIndexed { n, item -> Utterance(i, Markdown.plain(item), VoiceRole.Narrator, n) }
                is Block.Math -> listOf(Utterance(i, block.say.ifBlank { block.lines.joinToString(". ") }, VoiceRole.Notes))
                is Block.Lab -> listOfNotNull(block.narration.ifBlank { block.caption }.takeIf { it.isNotBlank() }?.let { Utterance(i, it, VoiceRole.Narrator) })
                is Block.Quiz -> listOf(
                    Utterance(
                        i,
                        "Quick check. ${block.question} " + block.options.mapIndexed { n, o -> "${'A' + n}: $o." }.joinToString(" "),
                        VoiceRole.Narrator,
                    ),
                )
                is Block.Code -> block.notes.map { Utterance(i, it.say, VoiceRole.Code, it.line) }
                is Block.Unsupported -> emptyList()
            }
        }
    }
}
