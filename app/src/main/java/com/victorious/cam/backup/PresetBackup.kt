package com.victorious.cam.backup

import com.victorious.cam.library.FeedPreset
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Export/import presets + overlay configs as JSON or zip (with optional logo assets).
 * Uses a minimal hand-rolled JSON codec so unit tests do not depend on Android org.json stubs.
 */
object PresetBackup {
    const val FORMAT_VERSION = 1
    const val JSON_NAME = "victorcam_presets.json"
    const val LOGOS_DIR = "logos/"

    fun toJson(presets: List<FeedPreset>): String {
        val items = presets.joinToString(",\n") { presetToJson(it).prependIndent("    ") }
        return """
            |{
            |  "formatVersion": $FORMAT_VERSION,
            |  "exportedAt": ${System.currentTimeMillis()},
            |  "presets": [
            |$items
            |  ]
            |}
        """.trimMargin()
    }

    fun fromJson(json: String): List<FeedPreset> {
        val ver = Regex(""""formatVersion"\s*:\s*(\d+)""").find(json)?.groupValues?.get(1)?.toIntOrNull()
            ?: error("missing formatVersion")
        require(ver == FORMAT_VERSION) { "Unsupported backup version: $ver" }
        val presetsBlock = Regex(""""presets"\s*:\s*\[(.*?)]""", RegexOption.DOT_MATCHES_ALL)
            .find(json)?.groupValues?.get(1) ?: return emptyList()
        if (presetsBlock.isBlank()) return emptyList()
        // Split top-level objects inside the array
        val objects = splitJsonObjects(presetsBlock)
        return objects.map { presetFromJsonObject(it) }
    }

    fun exportZip(presets: List<FeedPreset>, logoFiles: Map<String, ByteArray> = emptyMap()): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            zip.putNextEntry(ZipEntry(JSON_NAME))
            zip.write(toJson(presets).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            logoFiles.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(LOGOS_DIR + name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    data class ZipImport(
        val presets: List<FeedPreset>,
        val logos: Map<String, ByteArray>
    )

    fun importZip(bytes: ByteArray): ZipImport {
        var json: String? = null
        val logos = mutableMapOf<String, ByteArray>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                val data = zip.readBytes()
                when {
                    name == JSON_NAME || name.endsWith("/$JSON_NAME") ->
                        json = data.toString(Charsets.UTF_8)
                    name.startsWith(LOGOS_DIR) && !entry.isDirectory ->
                        logos[name.removePrefix(LOGOS_DIR)] = data
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val presets = fromJson(json ?: error("zip missing $JSON_NAME"))
        return ZipImport(presets, logos)
    }

    fun writeToFile(file: File, presets: List<FeedPreset>) {
        file.parentFile?.mkdirs()
        file.writeText(toJson(presets))
    }

    fun readFromFile(file: File): List<FeedPreset> = fromJson(file.readText())

    private fun presetToJson(p: FeedPreset): String = buildString {
        append("{\n")
        fun field(k: String, v: String?, raw: Boolean = false) {
            append("  \"")
            append(k)
            append("\": ")
            if (v == null) append("null")
            else if (raw) append(v)
            else append("\"").append(escape(v)).append("\"")
            append(",\n")
        }
        field("name", p.name)
        field("sourceKind", p.sourceKind)
        field("sourceUri", p.sourceUri)
        field("cropCsv", p.cropCsv)
        field("textEnabled", p.textEnabled.toString(), raw = true)
        field("text", p.text)
        field("textPosition", p.textPosition)
        field("textOpacity", p.textOpacity.toString(), raw = true)
        field("logoEnabled", p.logoEnabled.toString(), raw = true)
        field("logoUri", p.logoUri)
        field("logoPosition", p.logoPosition)
        field("logoOpacity", p.logoOpacity.toString(), raw = true)
        field("timestampEnabled", p.timestampEnabled.toString(), raw = true)
        field("timestampPosition", p.timestampPosition)
        // last field without trailing concerns — keep comma then strip
        append("  \"timestampOpacity\": ").append(p.timestampOpacity).append("\n}")
    }

    private fun presetFromJsonObject(obj: String): FeedPreset = FeedPreset(
        id = 0,
        name = stringField(obj, "name") ?: "",
        sourceKind = stringField(obj, "sourceKind") ?: "VIDEO_FILE",
        sourceUri = stringField(obj, "sourceUri"),
        cropCsv = stringField(obj, "cropCsv"),
        textEnabled = boolField(obj, "textEnabled"),
        text = stringField(obj, "text") ?: "",
        textPosition = stringField(obj, "textPosition") ?: "TOP_LEFT",
        textOpacity = floatField(obj, "textOpacity", 1f),
        logoEnabled = boolField(obj, "logoEnabled"),
        logoUri = stringField(obj, "logoUri"),
        logoPosition = stringField(obj, "logoPosition") ?: "BOTTOM_RIGHT",
        logoOpacity = floatField(obj, "logoOpacity", 0.85f),
        timestampEnabled = boolField(obj, "timestampEnabled"),
        timestampPosition = stringField(obj, "timestampPosition") ?: "BOTTOM_LEFT",
        timestampOpacity = floatField(obj, "timestampOpacity", 0.9f)
    )

    private fun stringField(obj: String, key: String): String? {
        val nullHit = Regex(""""$key"\s*:\s*null""").containsMatchIn(obj)
        if (nullHit) return null
        val m = Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""").find(obj) ?: return null
        return unescape(m.groupValues[1])
    }

    private fun boolField(obj: String, key: String): Boolean =
        Regex(""""$key"\s*:\s*(true|false)""").find(obj)?.groupValues?.get(1)?.toBoolean() ?: false

    private fun floatField(obj: String, key: String, default: Float): Float =
        Regex(""""$key"\s*:\s*(-?[0-9.]+)""").find(obj)?.groupValues?.get(1)?.toFloatOrNull() ?: default

    private fun escape(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

    private fun unescape(s: String): String =
        s.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\")

    private fun splitJsonObjects(arrayBody: String): List<String> {
        val out = mutableListOf<String>()
        var depth = 0
        var start = -1
        for (i in arrayBody.indices) {
            when (arrayBody[i]) {
                '{' -> {
                    if (depth == 0) start = i
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0 && start >= 0) {
                        out.add(arrayBody.substring(start, i + 1))
                        start = -1
                    }
                }
            }
        }
        return out
    }
}
