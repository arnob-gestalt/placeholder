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
        val categoryRegex = Regex("^###+\\s+(.+)$", RegexOption.MULTILINE)
        val category = categoryRegex.findAll(markdown).map { it.groupValues[1].trim() }.toList().firstOrNull() ?: "Medical scoring"
        val tools = Regex("^\\*\\*([^*]+)\\*\\*\\s*[—-]?\\s*(.*)$", RegexOption.MULTILINE)
            .findAll(markdown).mapIndexed { index, match ->
                val full = match.groupValues[1].trim()
                val parts = full.split(" — ", limit = 2)
                val name = parts[0].trim()
                RegistryTool("MSC-%04d".format(index + 1), name, category, parts.getOrNull(1) ?: "Clinical scoring system", null)
            }.distinctBy { it.name.lowercase() }.toList()
        return if (tools.isEmpty()) listOf(RegistryTool("MSC-0000", "Catalog unavailable", "System", "The bundled catalog could not be read", null, "blocked")) else tools
    }
}
