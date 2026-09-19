package com.clinicalc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF172321)
private val Muted = Color(0xFF61736F)
private val Paper = Color(0xFFF4F8F5)
private val Teal = Color(0xFF23796E)
private val Mint = Color(0xFFD6EEE3)
private val Amber = Color(0xFFFFE3A3)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { ClinicalcApp() } }
}

@Composable fun ClinicalcApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tools = remember { RegistryLoader.load(context) }
    var page by remember { mutableStateOf("home") }
    var selected by remember { mutableStateOf<RegistryTool?>(null) }
    MaterialTheme(colorScheme = lightColorScheme(primary = Teal, background = Paper)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFE9F4EF), Paper, Color(0xFFFFF0E8))))) {
            Scaffold(containerColor = Color.Transparent, bottomBar = { NavBar(page) { page = it; selected = null } }) { pad ->
                if (selected != null) ToolScreen(selected!!, { selected = null })
                else when (page) { "browse" -> Browse(tools) { selected = it }; "converter" -> Converter(); "profile" -> Profile(); else -> Home(tools) { selected = it } }
            }
        }
    }
}

@Composable private fun NavBar(page: String, navigate: (String) -> Unit) { Surface(Modifier.padding(14.dp).navigationBarsPadding(), RoundedCornerShape(30.dp), Color.White.copy(.86f), shadowElevation = 10.dp) { Row(Modifier.fillMaxWidth().padding(8.dp), Arrangement.SpaceAround) { listOf("home" to Icons.Rounded.Home, "browse" to Icons.Rounded.Search, "converter" to Icons.Rounded.Calculate, "profile" to Icons.Rounded.Settings).forEach { (id, icon) -> Column(Modifier.clickable { navigate(id) }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, id, tint = if (id == page) Teal else Muted); Text(id.replaceFirstChar { it.uppercase() }, fontSize = 11.sp, color = if (id == page) Teal else Muted) } } } } }

@Composable private fun Home(tools: List<RegistryTool>, open: (RegistryTool) -> Unit) { LazyColumn(PaddingValues(22.dp, 26.dp, 22.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) { item { Text("CLINICAL DECISION SUPPORT", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp) }; item { Text("Clarity when the clinical moment is moving fast.", color = Ink, fontSize = 35.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold) }; item { Text("Offline calculators, traceable sources, and calm clinical tools.", color = Muted, fontSize = 16.sp) }; item { Panel(Mint) { Text("Quick calculations", color = Teal, fontWeight = FontWeight.Bold); Text("Anion gap · MAP · QTc · MELD · conversions", color = Ink) } }; item { Text("Browse the registry", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold) }; items(tools.take(12)) { ToolRow(it, open) } } }

@Composable private fun Browse(tools: List<RegistryTool>, open: (RegistryTool) -> Unit) { var q by remember { mutableStateOf("") }; val filtered = tools.filter { it.name.contains(q, true) || it.category.contains(q, true) }; LazyColumn(PaddingValues(22.dp, 26.dp, 22.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Text("THE REGISTRY", color = Teal, fontWeight = FontWeight.Bold); Text("Find your score.", color = Ink, fontSize = 38.sp, fontWeight = FontWeight.Bold); OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), placeholder = { Text("Search ${tools.size} tools") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, singleLine = true); Text("${filtered.size} tools indexed", color = Muted, fontSize = 12.sp) }; items(filtered) { ToolRow(it, open) } } }

@Composable private fun ToolRow(tool: RegistryTool, open: (RegistryTool) -> Unit) { Surface(Modifier.fillMaxWidth().clickable { open(tool) }, RoundedCornerShape(20.dp), Color.White.copy(.9f), shadowElevation = 2.dp) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(42.dp), RoundedCornerShape(14.dp), if (tool.status == "ready") Mint else Amber) { Box(contentAlignment = Alignment.Center) { Text(if (tool.status == "ready") "✓" else "!", color = Teal, fontWeight = FontWeight.Bold) } }; Spacer(Modifier.width(13.dp)); Column { Text(tool.name, color = Ink, fontWeight = FontWeight.Bold); Text(tool.category, color = Muted, fontSize = 12.sp); Text(if (tool.status == "ready") "Calculator or reference card" else "Verify against source", color = Muted, fontSize = 12.sp) } } } }

@Composable private fun ToolScreen(tool: RegistryTool, back: () -> Unit) { var a by remember { mutableStateOf("") }; var b by remember { mutableStateOf("") }; var c by remember { mutableStateOf("") }; val result = when (tool.name.lowercase()) { "anion gap" -> if (a.isNotBlank() && b.isNotBlank() && c.isNotBlank()) runCatching { ClinicalEngine.anionGap(a.toDouble(), b.toDouble(), c.toDouble()).value }.getOrNull() else null; "map" -> if (a.isNotBlank() && b.isNotBlank()) runCatching { ClinicalEngine.map(a.toDouble(), b.toDouble()).value }.getOrNull() else null; "corrected calcium" -> if (a.isNotBlank() && b.isNotBlank()) runCatching { ClinicalEngine.correctedCalcium(a.toDouble(), b.toDouble()).value }.getOrNull() else null; "serum osmolality" -> if (a.isNotBlank() && b.isNotBlank() && c.isNotBlank()) runCatching { ClinicalEngine.osmolality(a.toDouble(), b.toDouble(), c.toDouble()).value }.getOrNull() else null; else -> null }; LazyColumn(PaddingValues(22.dp, 20.dp, 22.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { IconButton(back) { Icon(Icons.Rounded.ArrowBack, "Back") } }; item { Text(tool.category.uppercase(), color = Teal, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(tool.name, color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Bold); Text(tool.purpose, color = Muted) }; if (tool.status != "ready") item { Panel(Amber) { Text("Verify against source", color = Color(0xFF7A5200), fontWeight = FontWeight.Bold); Text("This content remains flagged and must not be treated as independently verified.", color = Ink) } }; item { Panel(if (result != null) Mint else Color.White.copy(.92f)) { Text(result ?: "Enter values to calculate", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold) } }; if (tool.name.lowercase() in listOf("anion gap", "serum osmolality")) { item { NumericField("Sodium", a) { a = it } }; item { NumericField("Chloride or glucose", b) { b = it } }; item { NumericField("Bicarbonate or BUN", c) { c = it } } } else if (tool.name.lowercase() == "map" || tool.name.lowercase() == "corrected calcium") { item { NumericField("Primary value", a) { a = it } }; item { NumericField("Secondary value", b) { b = it } } }; item { Text("Sources & evidence", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold) }; item { Text("Formula and clinical interpretation are sourced from the bundled Master File. Verify against current guidelines and local protocols.", color = Muted, lineHeight = 21.sp) }; item { Text("Educational / decision-support tool — not a substitute for clinical judgment.", color = Muted, fontSize = 12.sp) } } }

@Composable private fun Converter() { var value by remember { mutableStateOf("") }; var output by remember { mutableStateOf("") }; Column(Modifier.fillMaxSize().padding(22.dp), Arrangement.spacedBy(14.dp)) { Text("CONVERTER", color = Teal, fontWeight = FontWeight.Bold); Text("Clinical units", color = Ink, fontSize = 36.sp, fontWeight = FontWeight.Bold); NumericField("Creatinine mg/dL", value) { value = it; output = it.toDoubleOrNull()?.let { n -> ClinicalEngine.convert(n, "mg/dL", "µmol/L").toString() } ?: "" }; Panel(Mint) { Text(if (output.isBlank()) "Enter a value" else "$output µmol/L", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold) }; Text("Offline conversion using the WO-6 factors. Values are not stored.", color = Muted) } }
@Composable private fun Profile() { Column(Modifier.fillMaxSize().padding(22.dp), Arrangement.spacedBy(15.dp)) { Text("PROFILE", color = Teal, fontWeight = FontWeight.Bold); Text("Clinicalc", color = Ink, fontSize = 38.sp, fontWeight = FontWeight.Bold); Panel(Color.White.copy(.9f)) { Text("Privacy first", color = Teal, fontWeight = FontWeight.Bold); Text("Calculator inputs and results are not persisted or transmitted.", color = Ink) }; Text("Appearance, accessibility, provenance, and verification controls are part of the production roadmap.", color = Muted) } }
@Composable private fun NumericField(label: String, value: String, onValue: (String) -> Unit) { OutlinedTextField(value, onValue, Modifier.fillMaxWidth(), label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true) }
@Composable private fun Panel(color: Color, content: @Composable ColumnScope.() -> Unit) { Surface(Modifier.fillMaxWidth(), RoundedCornerShape(24.dp), color, shadowElevation = 1.dp) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content) } }
