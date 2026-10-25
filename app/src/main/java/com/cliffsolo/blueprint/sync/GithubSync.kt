package com.cliffsolo.blueprint.sync

import com.cliffsolo.blueprint.data.AppSettings
import com.cliffsolo.blueprint.data.CourseJson
import com.cliffsolo.blueprint.data.CourseStore
import com.cliffsolo.blueprint.data.SyncDao
import com.cliffsolo.blueprint.data.SyncedRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.zip.ZipInputStream

@Serializable
private data class SearchResult(val items: List<RepoInfo> = emptyList())

@Serializable
data class RepoInfo(
    val name: String,
    val full_name: String,
    val default_branch: String = "main",
    val pushed_at: String = "",
)

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
 * Finds public repos owned by [AppSettings.githubOwner] tagged with the `blueprint-course` topic,
 * and downloads any that changed since the last sync. Public repos need no token.
 */
class GithubSync(private val store: CourseStore, private val dao: SyncDao) {

    suspend fun sync(owner: String): SyncResult = withContext(Dispatchers.IO) {
        val repos = findRepos(owner)
        val known = dao.all().associateBy { it.repo }
        var added = 0
        var updated = 0
        val failed = mutableListOf<String>()
        for (repo in repos) {
            val previous = known[repo.full_name]
            val folder = File(store.downloadRoot, repo.name)
            if (previous != null && previous.pushedAt == repo.pushed_at && folder.exists()) continue
            runCatching { install(repo, folder) }
                .onSuccess {
                    dao.save(SyncedRepo(repo.full_name, repo.pushed_at, System.currentTimeMillis()))
                    if (previous == null) added++ else updated++
                }
                .onFailure { failed += repo.name }
        }
        store.refresh()
        SyncResult(added, updated, failed, repos.size)
    }

    private fun findRepos(owner: String): List<RepoInfo> {
        val q = URLEncoder.encode("topic:${AppSettings.COURSE_TOPIC} user:$owner", "UTF-8")
        val body = get("https://api.github.com/search/repositories?q=$q&per_page=100").use { it.bufferedReader().readText() }
        return CourseJson.json.decodeFromString<SearchResult>(body).items
    }

    /** Downloads the repo as a zip, checks it has a valid course.json, then swaps it in. */
    private fun install(repo: RepoInfo, target: File) {
        val staging = File(store.downloadRoot.parentFile, "staging/${repo.name}").apply { deleteRecursively(); mkdirs() }
        try {
            get("https://codeload.github.com/${repo.full_name}/zip/refs/heads/${repo.default_branch}").use { unzip(it, staging) }
            val root = staging.walkTopDown().maxDepth(2).firstOrNull { File(it, "course.json").isFile }
                ?: throw IOException("No course.json in ${repo.name}")
            CourseJson.manifest(File(root, "course.json").readText())
            target.deleteRecursively()
            if (!root.renameTo(target)) root.copyRecursively(target, overwrite = true)
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
        /** Unzips [input] into [dest], refusing entries that would escape it ("zip slip"). */
        fun unzip(input: InputStream, dest: File) {
            val base = dest.canonicalPath + File.separator
            ZipInputStream(input.buffered()).use { zip ->
                generateSequence { zip.nextEntry }.forEach { entry ->
                    val out = File(dest, entry.name)
                    if (!out.canonicalPath.startsWith(base)) throw IOException("Blocked unsafe zip entry: ${entry.name}")
                    if (entry.isDirectory) out.mkdirs()
                    else {
                        out.parentFile?.mkdirs()
                        out.outputStream().use { zip.copyTo(it) }
                    }
                }
            }
        }
    }
}
