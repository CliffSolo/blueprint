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
import java.io.File
import java.io.IOException
import java.nio.file.Files

class CourseFormatTest {

    @Test
    fun `every course in the repo parses and its labs exist`() {
        val root = File("../courses")
        val courses = root.listFiles().orEmpty().filter { it.isDirectory }
        assertTrue("expected at least one course", courses.isNotEmpty())
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
    fun `bullet lists read the intro then each bullet`() {
        val lesson = CourseJson.lesson(
            """{"id":"x","title":"X","blocks":[{"type":"list","intro":"The strokes are:","items":["**Intake**","Power"]}]}""",
        )
        val u = LessonViewModel.utterancesFor(lesson)
        assertEquals(listOf("The strokes are:", "Intake", "Power"), u.map { it.text })
        assertEquals(listOf(-1, 0, 1), u.map { it.line })
    }

    @Test
    fun `downloads refuse paths that escape the course folder`() {
        val dest = Files.createTempDirectory("course").toFile()
        try {
            assertEquals(File(dest, "labs/a.html").canonicalPath, GithubSync.safeChild(dest, "labs/a.html").canonicalPath)
            GithubSync.safeChild(dest, "../evil.txt")
            fail("expected unsafe path to be blocked")
        } catch (_: IOException) {
        } finally {
            dest.deleteRecursively()
        }
    }
}
