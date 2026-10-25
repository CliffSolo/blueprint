package com.cliffsolo.blueprint.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** course.json at the root of every course repo. */
@Serializable
data class CourseManifest(
    val schema: Int = 1,
    val id: String,
    val title: String,
    val subtitle: String = "",
    val description: String = "",
    val minutes: Int = 0,
    val version: String = "1.0.0",
    val updated: String = "",
    val accent: String = "#1F5FAE",
    val tags: List<String> = emptyList(),
    val lessons: List<LessonRef>,
)

@Serializable
data class LessonRef(val id: String, val title: String, val minutes: Int = 0, val file: String)

data class Lesson(val id: String, val title: String, val minutes: Int, val blocks: List<Block>)

/** One piece of a lesson. Unknown block types become [Block.Unsupported] so older apps can open newer courses. */
@Serializable
sealed interface Block {
    @Serializable @SerialName("heading")
    data class Heading(val text: String) : Block

    @Serializable @SerialName("text")
    data class Text(val text: String) : Block

    @Serializable @SerialName("callout")
    data class Callout(val style: String = "tip", val title: String = "", val text: String) : Block

    @Serializable @SerialName("steps")
    data class Steps(val items: List<String>) : Block

    @Serializable @SerialName("math")
    data class Math(val lines: List<String>, val say: String = "") : Block

    @Serializable @SerialName("lab")
    data class Lab(
        val src: String,
        val params: String = "",
        val height: Int = 420,
        val caption: String = "",
        val narration: String = "",
    ) : Block

    @Serializable @SerialName("quiz")
    data class Quiz(val question: String, val options: List<String>, val answer: Int, val explain: String = "") : Block

    /** Code shown line by line; each note explains one line and is read in the "code" voice. */
    @Serializable @SerialName("code")
    data class Code(val language: String = "", val lines: List<String>, val notes: List<CodeNote> = emptyList()) : Block

    @Serializable @SerialName("unsupported")
    data class Unsupported(val kind: String) : Block
}

@Serializable
data class CodeNote(val line: Int, val say: String)

object CourseJson {
    val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    fun manifest(text: String): CourseManifest = json.decodeFromString(text)

    fun lesson(text: String): Lesson {
        val root = json.parseToJsonElement(text).jsonObject
        val blocks = (root["blocks"] as? JsonArray).orEmpty().map { element ->
            val type = element.jsonObject["type"]?.jsonPrimitive?.content ?: "?"
            runCatching { json.decodeFromJsonElement<Block>(element) }.getOrElse { Block.Unsupported(type) }
        }
        return Lesson(
            id = root.string("id"),
            title = root.string("title"),
            minutes = root["minutes"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
            blocks = blocks,
        )
    }

    private fun JsonObject.string(key: String) = this[key]?.jsonPrimitive?.content.orEmpty()
}
