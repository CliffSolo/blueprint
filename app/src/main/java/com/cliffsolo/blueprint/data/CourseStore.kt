package com.cliffsolo.blueprint.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

/** Where a course's files live: bundled in the APK, or downloaded from its GitHub repo. */
sealed interface CourseSource {
    /** Folder name, used in lab URLs. */
    val dir: String

    data class Bundled(override val dir: String) : CourseSource
    data class Downloaded(override val dir: String, val root: File) : CourseSource
}

data class InstalledCourse(val manifest: CourseManifest, val source: CourseSource) {
    val id get() = manifest.id
    val isDemo get() = "demo" in manifest.tags
}

/**
 * Finds every course on the phone. Bundled courses ship inside the APK under assets/courses/;
 * synced courses are unzipped into files/courses/. A downloaded copy wins over a bundled one with the same id.
 */
class CourseStore(private val context: Context) {
    val downloadRoot: File get() = File(context.filesDir, "courses").apply { mkdirs() }

    private val _courses = MutableStateFlow<List<InstalledCourse>>(emptyList())
    val courses: StateFlow<List<InstalledCourse>> = _courses

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val found = linkedMapOf<String, InstalledCourse>()
        context.assets.list(ASSET_ROOT).orEmpty().sorted().forEach { dir ->
            runCatching {
                val text = context.assets.open("$ASSET_ROOT/$dir/course.json").bufferedReader().use { it.readText() }
                val m = CourseJson.manifest(text)
                found[m.id] = InstalledCourse(m, CourseSource.Bundled(dir))
            }
        }
        downloadRoot.listFiles().orEmpty().filter { it.isDirectory }.sortedBy { it.name }.forEach { dir ->
            runCatching {
                val m = CourseJson.manifest(File(dir, "course.json").readText())
                found[m.id] = InstalledCourse(m, CourseSource.Downloaded(dir.name, dir))
            }
        }
        _courses.value = found.values.sortedWith(compareByDescending<InstalledCourse> { it.isDemo }.thenBy { it.manifest.title })
    }

    fun course(id: String): InstalledCourse? = _courses.value.firstOrNull { it.id == id }

    suspend fun lesson(course: InstalledCourse, ref: LessonRef): Lesson = withContext(Dispatchers.IO) {
        CourseJson.lesson(readText(course.source, ref.file))
    }

    private fun readText(source: CourseSource, path: String): String = when (source) {
        is CourseSource.Bundled -> context.assets.open("$ASSET_ROOT/${source.dir}/$path").bufferedReader().use { it.readText() }
        is CourseSource.Downloaded -> File(source.root, path).also { require(it.canonicalPath.startsWith(source.root.canonicalPath)) }.readText()
    }

    /** URL the lab WebView loads; served by [com.cliffsolo.blueprint.ui.lesson.labAssetLoader]. */
    fun labUrl(course: InstalledCourse, lab: Block.Lab, dark: Boolean): String {
        val base = when (course.source) {
            is CourseSource.Bundled -> "https://$LAB_HOST/assets/$ASSET_ROOT/${course.source.dir}"
            is CourseSource.Downloaded -> "https://$LAB_HOST/courses/${course.source.dir}"
        }
        val query = listOf(lab.params, "theme=${if (dark) "dark" else "light"}").filter { it.isNotBlank() }.joinToString("&")
        return "$base/${lab.src}?$query"
    }

    companion object {
        const val ASSET_ROOT = "courses"
        const val LAB_HOST = "appassets.androidplatform.net"
    }
}
