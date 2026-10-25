package com.cliffsolo.blueprint

import com.cliffsolo.blueprint.data.Block
import com.cliffsolo.blueprint.data.CourseJson
import com.cliffsolo.blueprint.sync.GithubSync
import com.cliffsolo.blueprint.ui.Markdown
import com.cliffsolo.blueprint.ui.lesson.LessonViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CourseFormatTest {

    @Test
    fun `every bundled course parses and its labs exist`() {
        val root = File("src/main/assets/courses")
        val courses = root.listFiles().orEmpty().filter { it.isDirectory }
        assertTrue("expected at least one bundled course", courses.isNotEmpty())
        for (dir in courses) {
            val manifest = CourseJson.manifest(File(dir, "course.json").readText())
            assertTrue(manifest.lessons.isNotEmpty())
            for (ref in manifest.lessons) {
                val lesson = CourseJson.lesson(File(dir, ref.file).readText())
                assertEquals(ref.id, lesson.id)
                assertTrue("${ref.id} has unknown blocks", lesson.blocks.none { it is Block.Unsupported })
                lesson.blocks.filterIsInstance<Block.Lab>().forEach { lab ->
                    assertTrue("missing lab ${lab.src}", File(dir, lab.src).isFile)
                }
                lesson.blocks.filterIsInstance<Block.Quiz>().forEach { q ->
                    assertTrue("bad answer index in ${ref.id}", q.answer in q.options.indices)
                }
            }
        }
    }

    @Test
    fun `unknown block types do not break a lesson`() {
        val lesson = CourseJson.lesson(
            """{"id":"x","title":"X","blocks":[{"type":"text","text":"hi"},{"type":"hologram","size":3}]}""",
        )
        assertEquals(2, lesson.blocks.size)
        assertTrue(lesson.blocks[1] is Block.Unsupported)
    }

    @Test
    fun `spoken text matches displayed text so highlights line up`() {
        val src = "The **crankshaft** turns *push* into `spin`."
        val plain = Markdown.plain(src)
        assertEquals("The crankshaft turns push into spin.", plain)
        assertEquals(plain, Markdown.annotate(src, androidx.compose.ui.graphics.Color.Gray).text)
    }

    @Test
    fun `narration reads steps one item at a time`() {
        val lesson = CourseJson.lesson(
            """{"id":"x","title":"X","blocks":[{"type":"steps","items":["**One**","Two"]},{"type":"lab","src":"a.html"}]}""",
        )
        val u = LessonViewModel.utterancesFor(lesson)
        assertEquals(listOf("One", "Two"), u.map { it.text })
        assertEquals(listOf(0, 1), u.map { it.line })
    }

    @Test
    fun `unzip refuses entries that escape the folder`() {
        val bytes = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { z -> z.putNextEntry(ZipEntry("../evil.txt")); z.write(1); z.closeEntry() }
        }.toByteArray()
        val dest = Files.createTempDirectory("unzip").toFile()
        try {
            GithubSync.unzip(bytes.inputStream(), dest)
            fail("expected unsafe entry to be blocked")
        } catch (_: IOException) {
        } finally {
            dest.deleteRecursively()
        }
    }
}
