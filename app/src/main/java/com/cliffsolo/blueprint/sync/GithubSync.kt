package com.cliffsolo.blueprint.sync

import com.cliffsolo.blueprint.data.CourseJson
import com.cliffsolo.blueprint.data.CourseStore
import com.cliffsolo.blueprint.data.SyncDao
import com.cliffsolo.blueprint.data.SyncedCourse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class RepoEntry(val name: String, val type: String, val sha: String)

@Serializable
private data class Tree(val tree: List<TreeEntry> = emptyList(), val truncated: Boolean = false)

@Serializable
private data class TreeEntry(val path: String, val type: String)

data class SyncResult(val added: Int, val updated: Int, val failed: List<String>, val checked: Int) {
    val message: String
        get() = when {
            checked == 0 && failed.isEmpty() -> "No courses found online yet."
            added + updated == 0 && failed.isEmpty() -> "Everything is up to date."
            else -> buildList {
                if (added > 0) add("$added new")
                if (updated > 0) add("$updated updated")
                if (failed.isNotEmpty()) add("${failed.size} failed")
            }.joinToString(" · ")
        }
}

/**
 * Courses live in one public repo, one folder each under `courses/`. Each folder's git tree SHA changes
 * whenever anything inside it changes, so sync compares SHAs and only downloads folders that changed.
 * Public repo: no token needed.
 */
class GithubSync(private val store: CourseStore, private val dao: SyncDao) {

    suspend fun sync(repo: String): SyncResult = withContext(Dispatchers.IO) {
        val folders = listCourses(repo)
        val known = dao.all().associateBy { it.dir }
        var added = 0
        var updated = 0
        val failed = mutableListOf<String>()
        for (folder in folders) {
            val previous = known[folder.name]
            val target = File(store.downloadRoot, folder.name)
            if (previous != null && previous.sha == folder.sha && target.exists()) continue
            runCatching { install(repo, folder, target) }
                .onSuccess {
                    dao.save(SyncedCourse(folder.name, folder.sha, System.currentTimeMillis()))
                    if (previous == null) added++ else updated++
                }
                .onFailure { failed += folder.name }
        }
        store.refresh()
        SyncResult(added, updated, failed, folders.size)
    }

    private fun listCourses(repo: String): List<RepoEntry> {
        val body = get("https://api.github.com/repos/$repo/contents/$COURSES?ref=$BRANCH").use { it.bufferedReader().readText() }
        return CourseJson.json.decodeFromString<List<RepoEntry>>(body).filter { it.type == "dir" }
    }

    /** Downloads every file in one course folder, checks course.json, then swaps the folder in. */
    private fun install(repo: String, folder: RepoEntry, target: File) {
        val treeJson = get("https://api.github.com/repos/$repo/git/trees/${folder.sha}?recursive=1").use { it.bufferedReader().readText() }
        val tree = CourseJson.json.decodeFromString<Tree>(treeJson)
        if (tree.truncated) throw IOException("Course ${folder.name} is too big to list")
        val staging = File(store.downloadRoot.parentFile, "staging/${folder.name}").apply { deleteRecursively(); mkdirs() }
        try {
            for (entry in tree.tree.filter { it.type == "blob" }) {
                val out = safeChild(staging, entry.path)
                out.parentFile?.mkdirs()
                get("https://raw.githubusercontent.com/$repo/$BRANCH/$COURSES/${folder.name}/${entry.path}").use { input ->
                    out.outputStream().use { input.copyTo(it) }
                }
            }
            CourseJson.manifest(File(staging, "course.json").readText())
            target.deleteRecursively()
            if (!staging.renameTo(target)) staging.copyRecursively(target, overwrite = true)
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun get(url: String): InputStream {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "Blueprint-Android")
        if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode} for $url")
        return conn.inputStream
    }

    companion object {
        const val COURSES = "courses"
        const val BRANCH = "main"

        /** Resolves [path] inside [dir], refusing paths that would escape it (e.g. "../x"). */
        fun safeChild(dir: File, path: String): File {
            val out = File(dir, path)
            if (!out.canonicalPath.startsWith(dir.canonicalPath + File.separator)) throw IOException("Blocked unsafe path: $path")
            return out
        }
    }
}
