package com.clinicalc.app

import android.content.Context
import org.json.JSONObject

/** Runtime registry adapter. The bundled master catalog remains the source of truth. */
data class RegistryTool(
    val id: String,
    val name: String,
    val category: String,
    val purpose: String,
    val formula: String?,
    val status: String = "ready",
    val tier: Int = 2,
    val engine: String = "reference"
)

object RegistryLoader {
    fun load(context: Context): List<RegistryTool> {
        val markdown = runCatching { context.assets.open("catalog.md").bufferedReader().use { it.readText() } }.getOrDefault("")
        val headingRegex = Regex("^#{2,4}\\s+(.+)$", RegexOption.MULTILINE)
        val entryRegex = Regex("^\\*\\*([^*]+)\\*\\*\\s*[—-]?\\s*(.*)$", RegexOption.MULTILINE)
        val headings = headingRegex.findAll(markdown).toList()
        val tools = entryRegex.findAll(markdown).mapIndexed { index, match ->
            val heading = headings.lastOrNull { it.range.first < match.range.first }?.groupValues?.get(1)?.trim()
                ?: "Medical scoring"
            val name = match.groupValues[1].trim()
            val purpose = match.groupValues[2].trim().ifBlank { "Clinical scoring system" }
            RegistryTool("MSC-%04d".format(index + 1), name, heading, purpose, null)
        }.distinctBy { it.name.lowercase() }.toList()
        return if (tools.isEmpty()) listOf(RegistryTool("MSC-0000", "Catalog unavailable", "System", "The bundled catalog could not be read", null, "blocked")) else tools
    }
}
