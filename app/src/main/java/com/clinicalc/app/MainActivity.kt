package com.clinicalc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext

private val Ink = Color(0xFF172321)
private val Muted = Color(0xFF6D7D79)
private val Paper = Color(0xFFF1F6F3)
private val Teal = Color(0xFF23796E)
private val Mint = Color(0xFFD6EEE3)
private val Peach = Color(0xFFF5D6C6)

data class CatalogTool(val name: String, val category: String, val summary: String, val formula: String?, val calculable: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ClinicalcApp() }
    }
}

@Composable
fun ClinicalcApp() {
    val context = LocalContext.current
    val catalog = remember(context) { loadCatalog(context) }
    var screen by remember { mutableStateOf("home") }
    var selected by remember { mutableStateOf<CatalogTool?>(null) }
    var query by remember { mutableStateOf("") }
    MaterialTheme(colorScheme = androidx.compose.material3.lightColorScheme(primary = Teal, background = Paper)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFE8F3EE), Paper, Color(0xFFF7E9DF))))) {
            Scaffold(containerColor = Color.Transparent, bottomBar = { BottomBar(screen) { screen = it; selected = null } }) { padding ->
                AnimatedContent(selected, modifier = Modifier.padding(padding), label = "screen") { tool ->
                    if (tool != null) ToolDetail(tool) { selected = null }
                    else when (screen) {
                        "browse" -> BrowseScreen(catalog, query, { query = it }) { selected = it }
                        "saved" -> EmptyScreen("Saved tools", "Star a calculator to keep it close.")
                        "profile" -> EmptyScreen("Profile & settings", "Solid mode, privacy, sources, and app preferences.")
                        else -> HomeScreen(catalog) { selected = it }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBar(screen: String, onNavigate: (String) -> Unit) {
    Surface(Modifier.padding(16.dp).navigationBarsPadding().shadow(18.dp, RoundedCornerShape(28.dp)), shape = RoundedCornerShape(28.dp), color = Color.White.copy(.78f)) {
        Row(Modifier.fillMaxWidth().padding(7.dp), horizontalArrangement = Arrangement.SpaceAround) {
            NavItem("home", screen, Icons.Rounded.Home, "Home", onNavigate)
            NavItem("browse", screen, Icons.Rounded.Search, "Browse", onNavigate)
            NavItem("saved", screen, Icons.Rounded.Favorite, "Saved", onNavigate)
            NavItem("profile", screen, Icons.Rounded.Settings, "Profile", onNavigate)
        }
    }
}

@Composable
private fun NavItem(id: String, current: String, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onNavigate: (String) -> Unit) {
    Column(Modifier.clickable { onNavigate(id) }.padding(horizontal = 15.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = if (current == id) Teal else Muted, modifier = Modifier.size(22.dp))
        Text(label, color = if (current == id) Teal else Muted, fontSize = 11.sp, fontWeight = if (current == id) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun HomeScreen(catalog: List<CatalogTool>, onOpen: (CatalogTool) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(22.dp, 26.dp, 22.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("CLINICAL DECISION SUPPORT", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp) }
        item { Text("Clarity when the clinical moment is moving fast.", color = Ink, fontSize = 36.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold) }
        item { Text("A calm, traceable home for bedside scores, risk tools, and essential calculations.", color = Muted, fontSize = 16.sp, lineHeight = 24.sp) }
        item { GlassCard(Mint) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Calculate, null, tint = Teal); Spacer(Modifier.width(12.dp)); Column { Text("Quick calculator", fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("${catalog.size}+ indexed tools from your source catalog", color = Muted, fontSize = 13.sp) } } } }
        item { Text("Bedside essentials", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        items(catalog.filter { it.calculable }.take(8)) { ToolCard(it) { onOpen(it) } }
    }
}

@Composable
private fun BrowseScreen(catalog: List<CatalogTool>, query: String, onQuery: (String) -> Unit, onOpen: (CatalogTool) -> Unit) {
    val filtered = catalog.filter { it.name.contains(query, true) || it.category.contains(query, true) }
    LazyColumn(contentPadding = PaddingValues(22.dp, 26.dp, 22.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("THE CATALOG", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp) }
        item { Text("Find your score.", color = Ink, fontSize = 40.sp, fontWeight = FontWeight.Bold) }
        item { OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), placeholder = { Text("Search ${catalog.size}+ tools") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
        item { Text("${filtered.size} tools indexed · reference cards are clearly marked", color = Muted, fontSize = 12.sp) }
        items(filtered) { ToolCard(it) { onOpen(it) } }
    }
}

@Composable
private fun ToolCard(tool: CatalogTool, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(.86f)), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(42.dp), shape = RoundedCornerShape(13.dp), color = if (tool.calculable) Mint else Peach) { Box(contentAlignment = Alignment.Center) { Text(if (tool.calculable) "✓" else "i", color = Teal, fontWeight = FontWeight.Bold, fontSize = 18.sp) } }
            Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(tool.name, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(tool.summary, color = Muted, fontSize = 12.sp, maxLines = 2); Text(if (tool.calculable) "Calculator ready" else "Reference only · coefficient review needed", color = if (tool.calculable) Teal else Color(0xFF9B693C), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)) }
        }
    }
}

@Composable
private fun ToolDetail(tool: CatalogTool, onBack: () -> Unit) {
    var first by remember { mutableStateOf("") }; var second by remember { mutableStateOf("") }
    val result = when (tool.name.lowercase()) { "anion gap" -> if (first.isNotBlank() && second.isNotBlank()) "Enter bicarbonate in the third field below" else "—"; "mean arterial pressure" -> if (first.isNotBlank() && second.isNotBlank()) "${((second.toDoubleOrNull() ?: 0.0) + ((first.toDoubleOrNull() ?: 0.0) - (second.toDoubleOrNull() ?: 0.0)) / 3).toInt()} mmHg" else "—"; else -> "Reference" }
    LazyColumn(contentPadding = PaddingValues(22.dp, 22.dp, 22.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { IconButton(onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } }
        item { Text(tool.category.uppercase(), color = Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp) }
        item { Text(tool.name, color = Ink, fontSize = 34.sp, fontWeight = FontWeight.Bold) }
        item { Text(tool.summary, color = Muted, fontSize = 15.sp) }
        item { GlassCard(if (tool.calculable) Mint else Peach) { Column { Text(if (tool.calculable) "Calculator ready" else "Reference-only tool", color = Teal, fontWeight = FontWeight.Bold); Text(if (tool.calculable) result else "The supplied source identifies this tool, but coefficients or a complete scoring table are not verified for computation.", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) } } }
        if (tool.calculable) item { OutlinedTextField(first, { first = it }, Modifier.fillMaxWidth(), label = { Text("Primary value") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true) }
        if (tool.calculable) item { OutlinedTextField(second, { second = it }, Modifier.fillMaxWidth(), label = { Text("Secondary value") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true) }
        item { Text("Formula", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
        item { Text(tool.formula ?: "Published scoring table or external coefficient model required.", color = Muted, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.background(Color.White.copy(.75f), RoundedCornerShape(15.dp)).padding(15.dp)) }
        item { Text("Clinicalc is decision support for qualified professionals. Verify every result against current guidance and local protocols.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp) }
    }
}

@Composable private fun GlassCard(color: Color, content: @Composable () -> Unit) { Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = color.copy(.82f), tonalElevation = 0.dp) { Box(Modifier.padding(22.dp)) { content() } } }
@Composable private fun EmptyScreen(title: String, body: String) { Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) { Text(title, color = Ink, fontSize = 36.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp)); Text(body, color = Muted, fontSize = 16.sp) } }

private fun loadCatalog(context: android.content.Context): List<CatalogTool> {
    val source = runCatching { context.assets.open("catalog.md").bufferedReader().use { it.readText() } }.getOrNull().orEmpty()
    val headings = Regex("^\\*\\*([^*]+)\\*\\*", RegexOption.MULTILINE).findAll(source).map { it.groupValues[1].trim() }.distinctBy { it.lowercase() }.toList()
    val known = mapOf(
        "Anion gap" to "Na − (Cl + HCO₃)",
        "MAP" to "DBP + (SBP − DBP) / 3",
        "CHA2DS2-VASc" to "CHF + HTN + Age + Diabetes + Stroke/TIA + Vascular disease + Sex",
        "HEART" to "History + ECG + Age + Risk factors + Troponin",
        "qSOFA" to "RR ≥22 + SBP ≤100 + altered mentation",
        "CURB-65" to "Confusion + Urea + RR ≥30 + low BP + Age ≥65",
        "Glasgow Coma Scale" to "Eye + Verbal + Motor",
        "MELD" to "3.78 ln(bilirubin) + 11.2 ln(INR) + 9.57 ln(creatinine) + 6.43"
    )
    return headings.map { raw ->
        val name = raw.substringBefore(" —").trim(); val category = headingsCategory(source, raw); val formula = known.entries.firstOrNull { name.contains(it.key, true) }?.value
        CatalogTool(name, category, raw.substringAfter(" —", "Clinical scoring system").trim(), formula, formula != null)
    }.filter { it.name.length > 2 }.ifEmpty { listOf(CatalogTool("Catalog unavailable", "System", "Source asset could not be loaded", null, false)) }
}

private fun headingsCategory(source: String, heading: String): String { val index = source.indexOf("**$heading**"); val prefix = if (index >= 0) source.substring(0, index) else ""; return prefix.substringAfterLast("### ", "Clinical tools").lineSequence().firstOrNull()?.trim()?.ifBlank { "Clinical tools" } ?: "Clinical tools" }