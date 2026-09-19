package com.clinicalc.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.isSystemInDarkTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.E
import kotlin.math.pow
import kotlin.math.sqrt

private val Ink = Color(0xFF172321)
private val Muted = Color(0xFF647772)
private val Paper = Color(0xFFF3F7F4)
private val PaperDark = Color(0xFF08110F)
private val Teal = Color(0xFF23796E)
private val Mint = Color(0xFFDCEFE7)
private val Peach = Color(0xFFF3D8C8)
private val Amber = Color(0xFFF1C66B)
private val Rose = Color(0xFFE6B4AF)

private enum class AppTab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Rounded.Home),
    Browse("Browse", Icons.Rounded.Search),
    Favorites("Favorites", Icons.Rounded.Favorite),
    History("History", Icons.Rounded.History),
    Profile("Profile", Icons.Rounded.Person)
}

private enum class AppearanceMode { Clear, Tinted, Frosted, Solid }
private enum class ToolEngine { Equation, PointsSum, DisplayOnly, ExternalOnly, Categorical }
private enum class ToolStatus { Ready, NeedsVerification, ExternalOnly, Blocked }

private data class RegistryTool(
    val id: String,
    val name: String,
    val acronym: String?,
    val aliases: List<String>,
    val category: String,
    val tier: Int,
    val purpose: String,
    val formula: String,
    val engine: ToolEngine,
    val status: ToolStatus,
    val restricted: Boolean = false
)

private data class PointsItem(val label: String, val points: Int, val group: String? = null)
private data class EquationStatement(val label: String, val expression: String)
private data class EquationDefinition(val statements: List<EquationStatement>, val inputs: List<String>)
private data class EquationResult(val label: String, val value: Double)
private data class SourceEvidence(val citation: String?, val pmid: String?, val sourceNote: String)

private class UserLibraryStore(context: Context) {
    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("clinicalc_user_prefs") }
    )
    private val favoritesKey = stringPreferencesKey("favorite_ids")
    private val recentsKey = stringPreferencesKey("recent_ids")

    val favorites: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[favoritesKey].orEmpty().split(',').filter { it.isNotBlank() }.toSet()
    }

    val recents: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[recentsKey].orEmpty().split(',').filter { it.isNotBlank() }
    }

    suspend fun toggleFavorite(id: String) {
        dataStore.edit { prefs ->
            val current = prefs[favoritesKey].orEmpty().split(',').filter { it.isNotBlank() }.toMutableSet()
            if (!current.add(id)) current.remove(id)
            prefs[favoritesKey] = current.sorted().joinToString(",")
        }
    }

    suspend fun recordRecent(id: String) {
        dataStore.edit { prefs ->
            val current = prefs[recentsKey].orEmpty().split(',').filter { it.isNotBlank() }
            prefs[recentsKey] = (listOf(id) + current.filterNot { it == id }).take(12).joinToString(",")
        }
    }

    suspend fun clearRecents() {
        dataStore.edit { it[recentsKey] = "" }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ClinicalcApp() }
    }
}

@Composable
private fun ClinicalcApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(context) { UserLibraryStore(context) }
    val registry = remember(context) { loadRegistry(context) }
    val masterPrompt = remember(context) { loadTextAsset(context, "master_prompt.md") }
    val favorites by store.favorites.collectAsState(initial = emptySet())
    val recents by store.recents.collectAsState(initial = emptyList())
    var tab by rememberSaveable { mutableStateOf(AppTab.Home) }
    var selectedToolId by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var tierFilter by rememberSaveable { mutableStateOf(0) }
    var appearance by rememberSaveable { mutableStateOf(AppearanceMode.Tinted) }
    val selectedTool = registry.firstOrNull { it.id == selectedToolId }
    val darkTheme = isSystemInDarkTheme()
    val colors = if (darkTheme) {
        darkColorScheme(primary = Color(0xFF66C7B5), background = PaperDark, surface = Color(0xFF0F1A17))
    } else {
        lightColorScheme(primary = Teal, background = Paper, surface = Color.White)
    }

    MaterialTheme(colorScheme = colors) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(meshBrush(darkTheme))
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    if (selectedTool == null) {
                        BottomBar(current = tab, appearance = appearance) {
                            tab = it
                        }
                    }
                }
            ) { padding ->
                AnimatedContent(
                    targetState = selectedTool,
                    modifier = Modifier.padding(padding),
                    label = "clinicalc-screen"
                ) { tool ->
                    if (tool != null) {
                        LaunchedEffect(tool.id) { store.recordRecent(tool.id) }
                        ToolDetailScreen(
                            tool = tool,
                            isFavorite = favorites.contains(tool.id),
                            appearance = appearance,
                            evidence = remember(tool.id, masterPrompt) { resolveEvidence(tool, masterPrompt) },
                            onBack = { selectedToolId = null },
                            onToggleFavorite = { scope.launch { store.toggleFavorite(tool.id) } }
                        )
                    } else {
                        when (tab) {
                            AppTab.Home -> HomeScreen(
                                registry = registry,
                                favoritesCount = favorites.size,
                                recentsCount = recents.size,
                                appearance = appearance,
                                onBrowse = { tab = AppTab.Browse },
                                onOpen = { selectedToolId = it.id }
                            )
                            AppTab.Browse -> BrowseScreen(
                                registry = registry,
                                query = searchQuery,
                                tierFilter = tierFilter,
                                appearance = appearance,
                                onQueryChange = { searchQuery = it },
                                onTierChange = { tierFilter = it },
                                onOpen = { selectedToolId = it.id }
                            )
                            AppTab.Favorites -> ToolCollectionScreen(
                                title = "Favorites",
                                body = "Pinned tools stay local on device and never store patient-entered values.",
                                tools = registry.filter { favorites.contains(it.id) },
                                emptyTitle = "No favorites yet",
                                emptyBody = "Star a calculator to keep it close.",
                                appearance = appearance,
                                onOpen = { selectedToolId = it.id }
                            )
                            AppTab.History -> ToolCollectionScreen(
                                title = "History",
                                body = "Recent tools only. Clinicalc does not persist entered values.",
                                tools = recents.mapNotNull { recentId -> registry.firstOrNull { it.id == recentId } },
                                emptyTitle = "No recent tools",
                                emptyBody = "Open a tool to add it here.",
                                appearance = appearance,
                                trailingAction = {
                                    Text(
                                        text = "Clear",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable { scope.launch { store.clearRecents() } }
                                    )
                                },
                                onOpen = { selectedToolId = it.id }
                            )
                            AppTab.Profile -> ProfileScreen(
                                registry = registry,
                                appearance = appearance,
                                onAppearanceChange = { appearance = it }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBar(current: AppTab, appearance: AppearanceMode, onNavigate: (AppTab) -> Unit) {
    GlassSurface(
        appearance = appearance,
        modifier = Modifier
            .padding(16.dp)
            .navigationBarsPadding()
            .shadow(18.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            AppTab.entries.forEach { tab ->
                val active = current == tab
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onNavigate(tab) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (active) MaterialTheme.colorScheme.primary else Muted,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = tab.label,
                        color = if (active) MaterialTheme.colorScheme.primary else Muted,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    registry: List<RegistryTool>,
    favoritesCount: Int,
    recentsCount: Int,
    appearance: AppearanceMode,
    onBrowse: () -> Unit,
    onOpen: (RegistryTool) -> Unit
) {
    val readyCount = registry.count { it.status == ToolStatus.Ready }
    val categories = registry.map { it.category }.distinct()
    val bedside = registry.filter { it.category.equals("Bedside calculations", true) || it.tier == 1 }.take(8)
    LazyColumn(
        contentPadding = PaddingValues(22.dp, 24.dp, 22.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SectionEyebrow("Clinicalc") }
        item {
            Text(
                text = "Fast, traceable bedside scoring with calm chrome and solid clinical content.",
                color = if (isSystemInDarkTheme()) Color.White else Ink,
                fontSize = 34.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Text(
                text = "Offline registry, conservative status handling, and live calculators where the bundled source is safely computable.",
                color = Muted,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
        item {
            GlassSurface(appearance = appearance, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onBrowse)
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Search by name, acronym, or alias", fontWeight = FontWeight.SemiBold, color = if (isSystemInDarkTheme()) Color.White else Ink)
                        Text("${registry.size} tools indexed from bundled assets", color = Muted, fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Ready", readyCount.toString(), Mint, Modifier.weight(1f))
                MetricCard("Favorites", favoritesCount.toString(), Color(0xFFE9DDF1), Modifier.weight(1f))
                MetricCard("Recents", recentsCount.toString(), Peach, Modifier.weight(1f))
            }
        }
        item { SectionTitle("Category Browse") }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { category ->
                    Surface(
                        color = categoryColor(category).copy(alpha = 0.18f),
                        contentColor = if (isSystemInDarkTheme()) Color.White else Ink,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = category,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        item { SectionTitle("Bedside Essentials") }
        items(bedside) { tool ->
            ToolRow(tool = tool, appearance = appearance) { onOpen(tool) }
        }
    }
}

@Composable
private fun BrowseScreen(
    registry: List<RegistryTool>,
    query: String,
    tierFilter: Int,
    appearance: AppearanceMode,
    onQueryChange: (String) -> Unit,
    onTierChange: (Int) -> Unit,
    onOpen: (RegistryTool) -> Unit
) {
    val filtered = registry.filter { tool ->
        (tierFilter == 0 || tool.tier == tierFilter) && tool.matchesQuery(query)
    }
    LazyColumn(
        contentPadding = PaddingValues(22.dp, 24.dp, 22.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SectionEyebrow("Browse") }
        item {
            Text(
                text = "Find the right score.",
                color = if (isSystemInDarkTheme()) Color.White else Ink,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            SolidSurface {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Try chads, qsofa, map, or glasgow") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )
            }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "All", 1 to "Tier 1", 2 to "Tier 2", 3 to "Tier 3").forEach { (tier, label) ->
                    FilterChip(
                        selected = tierFilter == tier,
                        onClick = { onTierChange(tier) },
                        label = { Text(label) }
                    )
                }
            }
        }
        item {
            Text(
                text = "${filtered.size} results. External and verification-needed entries stay clearly labeled.",
                color = Muted,
                fontSize = 12.sp
            )
        }
        items(filtered) { tool ->
            ToolRow(tool = tool, appearance = appearance) { onOpen(tool) }
        }
    }
}

@Composable
private fun ToolCollectionScreen(
    title: String,
    body: String,
    tools: List<RegistryTool>,
    emptyTitle: String,
    emptyBody: String,
    appearance: AppearanceMode,
    trailingAction: @Composable (() -> Unit)? = null,
    onOpen: (RegistryTool) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(22.dp, 24.dp, 22.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionEyebrow(title)
                    Text(
                        text = title,
                        color = if (isSystemInDarkTheme()) Color.White else Ink,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                trailingAction?.invoke()
            }
        }
        item { Text(text = body, color = Muted, fontSize = 14.sp, lineHeight = 21.sp) }
        if (tools.isEmpty()) {
            item { EmptyState(title = emptyTitle, body = emptyBody, appearance = appearance) }
        } else {
            items(tools) { tool ->
                ToolRow(tool = tool, appearance = appearance) { onOpen(tool) }
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    registry: List<RegistryTool>,
    appearance: AppearanceMode,
    onAppearanceChange: (AppearanceMode) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(22.dp, 24.dp, 22.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SectionEyebrow("Profile") }
        item {
            Text(
                text = "Appearance & data posture",
                color = if (isSystemInDarkTheme()) Color.White else Ink,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            SolidSurface {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Glass style", fontWeight = FontWeight.SemiBold, color = if (isSystemInDarkTheme()) Color.White else Ink)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppearanceMode.entries.forEach { option ->
                            FilterChip(
                                selected = appearance == option,
                                onClick = { onAppearanceChange(option) },
                                label = { Text(option.name) }
                            )
                        }
                    }
                }
            }
        }
        item {
            SolidSurface {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Registry status", fontWeight = FontWeight.SemiBold, color = if (isSystemInDarkTheme()) Color.White else Ink)
                    Text("Ready: ${registry.count { it.status == ToolStatus.Ready }}", color = Muted)
                    Text("Needs verification: ${registry.count { it.status == ToolStatus.NeedsVerification }}", color = Muted)
                    Text("External-only: ${registry.count { it.status == ToolStatus.ExternalOnly }}", color = Muted)
                }
            }
        }
        item {
            SolidSurface {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Clinical disclaimer", fontWeight = FontWeight.SemiBold, color = if (isSystemInDarkTheme()) Color.White else Ink)
                    Text(
                        text = "Educational and decision-support use only. Verify every result against the cited source, current guidance, and local policy. Clinicalc does not persist patient-entered values.",
                        color = Muted,
                        lineHeight = 21.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolDetailScreen(
    tool: RegistryTool,
    isFavorite: Boolean,
    appearance: AppearanceMode,
    evidence: SourceEvidence,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    var shareSummary by remember(tool.id) { mutableStateOf(tool.purpose) }
    LazyColumn(
        contentPadding = PaddingValues(22.dp, 22.dp, 22.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFE14E78) else Muted
                    )
                }
                IconButton(onClick = { shareTool(context, tool, shareSummary, evidence) }) {
                    Icon(Icons.Rounded.Share, contentDescription = "Share")
                }
            }
        }
        item {
            SectionEyebrow(tool.category)
            Text(
                text = tool.name,
                color = if (isSystemInDarkTheme()) Color.White else Ink,
                fontSize = 32.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(text = tool.purpose, color = Muted, fontSize = 15.sp, lineHeight = 22.sp)
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(tool.status)
                MetaChip("Tier ${tool.tier}")
                MetaChip(tool.engine.name.replace('-', ' '))
            }
        }
        item {
            when (tool.engine) {
                ToolEngine.Equation -> EquationPanel(tool = tool, appearance = appearance) { shareSummary = it }
                ToolEngine.PointsSum -> PointsPanel(tool = tool, appearance = appearance) { shareSummary = it }
                ToolEngine.DisplayOnly -> DisplayOnlyPanel(tool = tool) { shareSummary = it }
                ToolEngine.ExternalOnly -> ExternalOnlyPanel(tool = tool) { shareSummary = it }
                ToolEngine.Categorical -> ReferencePanel(tool = tool) { shareSummary = it }
            }
        }
        item { SectionTitle("Formula") }
        item {
            SolidSurface {
                Text(text = tool.formula, color = Muted, lineHeight = 21.sp)
            }
        }
        item { SectionTitle("Sources & Evidence") }
        item {
            SolidSurface {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Offline source note", fontWeight = FontWeight.SemiBold, color = if (isSystemInDarkTheme()) Color.White else Ink)
                    Text(evidence.sourceNote, color = Muted, lineHeight = 21.sp)
                    evidence.citation?.let { citation ->
                        Text("Citation", fontWeight = FontWeight.SemiBold, color = if (isSystemInDarkTheme()) Color.White else Ink)
                        Text(citation, color = Muted, lineHeight = 21.sp)
                    }
                    evidence.pmid?.let { pmid ->
                        Text("PMID: $pmid", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    if (tool.restricted) {
                        Text(
                            "Restricted instrument: show scoring structure only and verify licensing before redistributing any stimulus or administration content.",
                            color = Color(0xFF9B693C),
                            lineHeight = 21.sp
                        )
                    }
                }
            }
        }
        item {
            Text(
                text = "Clinicalc is educational and decision support only. Confirm every output against the source literature and current local guidance.",
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun EquationPanel(tool: RegistryTool, appearance: AppearanceMode, onSummaryChange: (String) -> Unit) {
    val definition = remember(tool.id) { parseEquationDefinition(tool.formula) }
    if (definition == null || definition.inputs.isEmpty()) {
        ReferencePanel(tool = tool, onSummaryChange = onSummaryChange)
        return
    }
    val values = remember(tool.id) { mutableStateMapOf<String, String>().apply { definition.inputs.forEach { put(it, "") } } }
    val outputs = remember(tool.id, values.toMap()) { evaluateEquation(definition, values) }
    LaunchedEffect(outputs) {
        onSummaryChange(
            outputs.joinToString(separator = " · ") { "${it.label}: ${formatNumber(it.value)}" }.ifBlank { tool.purpose }
        )
    }
    GlassSurface(appearance = appearance, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(20.dp)) {
            Text("Live calculator", fontWeight = FontWeight.SemiBold, color = if (isSystemInDarkTheme()) Color.White else Ink)
            definition.inputs.forEach { input ->
                SolidSurface {
                    OutlinedTextField(
                        value = values[input].orEmpty(),
                        onValueChange = { values[input] = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(prettyToken(input)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp)
                    )
                }
            }
            SolidSurface {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Result", fontWeight = FontWeight.SemiBold, color = if (isSystemInDarkTheme()) Color.White else Ink)
                    if (outputs.isEmpty()) {
                        Text("Enter all numeric inputs to calculate.", color = Muted)
                    } else {
                        outputs.forEach { result ->
                            Text("${result.label}: ${formatNumber(result.value)}", color = if (isSystemInDarkTheme()) Color.White else Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PointsPanel(tool: RegistryTool, appearance: AppearanceMode, onSummaryChange: (String) -> Unit) {
    val points = remember(tool.id) { parsePointsItems(tool.formula) }
    if (points.isEmpty()) {
        ReferencePanel(tool = tool, onSummaryChange = onSummaryChange)
        return
    }
    var selectedGrouped by remember(tool.id) { mutableStateOf(mapOf<String, String>()) }
    var selectedSingles by remember(tool.id) { mutableStateOf(setOf<String>()) }
    val total = points.sumOf { item ->
        when {
            item.group != null -> if (selectedGrouped[item.group] == item.label) item.points else 0
            item.label in selectedSingles -> item.points
            else -> 0
        }
    }
    LaunchedEffect(total) { onSummaryChange("Score: $total") }
    GlassSurface(appearance = appearance, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(20.dp)) {
            Text("Boolean point score", fontWeight = FontWeight.SemiBold, color = if (isSystemInDarkTheme()) Color.White else Ink)
            SolidSurface {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    points.groupBy { it.group ?: it.label }.forEach { (group, items) ->
                        Text(if (items.first().group != null) prettyToken(group) else items.first().label, fontWeight = FontWeight.SemiBold, color = if (isSystemInDarkTheme()) Color.White else Ink)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (items.first().group != null) {
                                items.forEach { item ->
                                    FilterChip(
                                        selected = selectedGrouped[group] == item.label,
                                        onClick = { selectedGrouped = selectedGrouped.toMutableMap().apply { put(group, item.label) } },
                                        label = { Text("${item.label} (+${item.points})") }
                                    )
                                }
                            } else {
                                items.forEach { item ->
                                    FilterChip(
                                        selected = selectedSingles.contains(item.label),
                                        onClick = {
                                            selectedSingles = selectedSingles.toMutableSet().apply {
                                                if (!add(item.label)) remove(item.label)
                                            }
                                        },
                                        label = { Text("${item.label} (+${item.points})") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            SolidSurface {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Result", fontWeight = FontWeight.SemiBold, color = if (isSystemInDarkTheme()) Color.White else Ink)
                    Text("$total points", color = if (isSystemInDarkTheme()) Color.White else Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Verify mutually exclusive choices against the source before acting on the result.", color = Muted, lineHeight = 21.sp)
                }
            }
        }
    }
}

@Composable
private fun DisplayOnlyPanel(tool: RegistryTool, onSummaryChange: (String) -> Unit) {
    var value by remember(tool.id) { mutableStateOf("") }
    LaunchedEffect(value) { onSummaryChange(if (value.isBlank()) tool.purpose else "Reported value: $value") }
    SolidSurface {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Machine-reported value", fontWeight = FontWeight.SemiBold, color = if (isSystemInDarkTheme()) Color.White else Ink)
            Text("This item is intentionally display-only. Enter the instrument or analyzer result rather than computing it locally.", color = Muted, lineHeight = 21.sp)
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Reported value") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}

@Composable
private fun ExternalOnlyPanel(tool: RegistryTool, onSummaryChange: (String) -> Unit) {
    LaunchedEffect(tool.id) { onSummaryChange("External-only reference card") }
    SolidSurface {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("External-only card", fontWeight = FontWeight.SemiBold, color = if (isSystemInDarkTheme()) Color.White else Ink)
            Text(
                "The bundled source identifies this model, but the coefficients, licensing, or proprietary scoring tables are not safely present for local computation.",
                color = Muted,
                lineHeight = 21.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Keep as citation-backed reference until a verified local implementation exists.", color = Muted)
            }
        }
    }
}

@Composable
private fun ReferencePanel(tool: RegistryTool, onSummaryChange: (String) -> Unit) {
    LaunchedEffect(tool.id) { onSummaryChange(tool.purpose) }
    SolidSurface {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Reference card", fontWeight = FontWeight.SemiBold, color = if (isSystemInDarkTheme()) Color.White else Ink)
            Text(
                "This entry is currently presented as a structured reference because the merged source text does not expose enough machine-safe input detail for a generic calculator.",
                color = Muted,
                lineHeight = 21.sp
            )
            Text("Use the formula and evidence panel below to verify thresholds, classes, or upgrade paths.", color = Muted, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun ToolRow(tool: RegistryTool, appearance: AppearanceMode, onOpen: () -> Unit) {
    GlassSurface(appearance = appearance, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(categoryColor(tool.category).copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (tool.engine) {
                        ToolEngine.Equation -> Icons.Rounded.Calculate
                        ToolEngine.PointsSum -> Icons.Rounded.Calculate
                        ToolEngine.DisplayOnly -> Icons.Rounded.Calculate
                        ToolEngine.ExternalOnly -> Icons.Rounded.OpenInNew
                        ToolEngine.Categorical -> Icons.Rounded.Search
                    },
                    contentDescription = null,
                    tint = categoryColor(tool.category)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = tool.name,
                    color = if (isSystemInDarkTheme()) Color.White else Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = tool.purpose,
                    color = Muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(tool.status, compact = true)
                    MetaChip("Tier ${tool.tier}", compact = true)
                }
            }
        }
    }
}

@Composable
private fun SectionEyebrow(text: String) {
    Text(
        text = text.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        fontSize = 12.sp,
        letterSpacing = 1.6.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = if (isSystemInDarkTheme()) Color.White else Ink,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun MetricCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.78f))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = Ink.copy(alpha = 0.75f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(value, color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String, appearance: AppearanceMode) {
    GlassSurface(appearance = appearance, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = if (isSystemInDarkTheme()) Color.White else Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(body, color = Muted, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun StatusChip(status: ToolStatus, compact: Boolean = false) {
    val (bg, fg, label) = when (status) {
        ToolStatus.Ready -> Triple(Mint, Teal, "Ready")
        ToolStatus.NeedsVerification -> Triple(Amber.copy(alpha = 0.35f), Color(0xFF8A5B00), "Verify")
        ToolStatus.ExternalOnly -> Triple(Peach.copy(alpha = 0.45f), Color(0xFF9B693C), "External")
        ToolStatus.Blocked -> Triple(Rose.copy(alpha = 0.45f), Color(0xFF8C3B35), "Blocked")
    }
    Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(999.dp)) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = if (compact) 10.dp else 12.dp, vertical = if (compact) 5.dp else 7.dp),
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MetaChip(text: String, compact: Boolean = false) {
    Surface(
        color = Color.White.copy(alpha = if (isSystemInDarkTheme()) 0.08f else 0.58f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (isSystemInDarkTheme()) 0.08f else 0.35f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = if (compact) 10.dp else 12.dp, vertical = if (compact) 5.dp else 7.dp),
            color = Muted,
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GlassSurface(
    appearance: AppearanceMode,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    content: @Composable () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val container = when (appearance) {
        AppearanceMode.Clear -> if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.68f)
        AppearanceMode.Tinted -> if (dark) Color(0xFF12342F).copy(alpha = 0.42f) else Color(0xFFE7F4EF).copy(alpha = 0.78f)
        AppearanceMode.Frosted -> if (dark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.88f)
        AppearanceMode.Solid -> MaterialTheme.colorScheme.surface
    }
    val border = if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.42f)
    Surface(
        modifier = modifier,
        shape = shape,
        color = container,
        border = BorderStroke(1.dp, border),
        shadowElevation = if (appearance == AppearanceMode.Solid) 0.dp else 10.dp
    ) {
        Box(Modifier.padding(1.dp)) { content() }
    }
}

@Composable
private fun SolidSurface(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = if (isSystemInDarkTheme()) Color(0xFF0F1A17) else Color.White.copy(alpha = 0.92f)
    ) {
        Box(Modifier.padding(18.dp)) { content() }
    }
}

private fun loadRegistry(context: Context): List<RegistryTool> {
    val source = loadTextAsset(context, "catalog.md")
    if (source.isBlank()) {
        return listOf(
            RegistryTool(
                id = "catalog-unavailable",
                name = "Catalog unavailable",
                acronym = null,
                aliases = emptyList(),
                category = "System",
                tier = 1,
                purpose = "Bundled source asset could not be loaded.",
                formula = "Source asset missing",
                engine = ToolEngine.ExternalOnly,
                status = ToolStatus.Blocked
            )
        )
    }
    val tools = mutableListOf<RegistryTool>()
    var category = "Clinical tools"
    var tier = 2
    val lines = source.lines()
    var index = 0
    while (index < lines.size) {
        val line = lines[index].trim()
        when {
            line.startsWith("## ") -> category = line.removePrefix("## ").trim()
            line.startsWith("### Tier ") -> tier = line.removePrefix("### Tier ").trim().toIntOrNull() ?: tier
            line.startsWith("**") && line.contains("** — ") -> {
                val match = Regex("^\\*\\*(.+?)\\*\\*\\s+—\\s+(.*)$").find(line)
                if (match != null) {
                    val name = match.groupValues[1].trim()
                    val purpose = match.groupValues[2].trim()
                    val formulaLines = mutableListOf<String>()
                    var scan = index + 1
                    while (scan < lines.size && lines[scan].trim().startsWith(">")) {
                        formulaLines += lines[scan].trim().removePrefix(">").trim()
                        scan += 1
                    }
                    val formula = formulaLines.joinToString(" ").ifBlank { "No formula text found in source." }
                    tools += buildRegistryTool(name, purpose, formula, category, tier)
                    index = scan - 1
                }
            }
        }
        index += 1
    }
    return tools.distinctBy { it.id }
}

private fun buildRegistryTool(
    name: String,
    purpose: String,
    formula: String,
    category: String,
    tier: Int
): RegistryTool {
    val normalizedName = normalizeSearch(name)
    val acronym = inferAcronym(name)
    val aliases = buildList {
        acronym?.let { add(it) }
        add(name.replace("/", " "))
        add(name.replace("-", " "))
        if (normalizedName == "map") add("mean arterial pressure")
        if (normalizedName.contains("cha2ds2")) add("chads vasc")
        if (normalizedName.contains("qsofa")) add("quick sofa")
    }.distinct()
    val points = parsePointsItems(formula)
    val equationDefinition = parseEquationDefinition(formula)
    val externalOnlyNames = setOf(
        "euroscore ii", "qrisk3", "sts risk score", "garfield af", "garfield-af",
        "reynolds risk score", "apache iv", "oasis", "phoenix criteria", "clif c aclf",
        "clip score", "okuda score"
    )
    val displayOnlyNames = setOf("base excess")
    val restrictedNames = setOf("mmse", "moca", "slums")
    val engine = when {
        normalizedName in displayOnlyNames || formula.contains("not calculable", true) -> ToolEngine.DisplayOnly
        normalizedName in externalOnlyNames || formula.contains("online calculator required", true) -> ToolEngine.ExternalOnly
        points.size >= 2 -> ToolEngine.PointsSum
        equationDefinition != null && equationDefinition.inputs.isNotEmpty() -> ToolEngine.Equation
        else -> ToolEngine.Categorical
    }
    val status = when {
        engine == ToolEngine.ExternalOnly -> ToolStatus.ExternalOnly
        normalizedName in restrictedNames || formula.contains("?", ignoreCase = false) || formula.contains("see ", true) -> ToolStatus.NeedsVerification
        else -> ToolStatus.Ready
    }
    return RegistryTool(
        id = slug(name),
        name = name,
        acronym = acronym,
        aliases = aliases,
        category = category,
        tier = tier,
        purpose = purpose,
        formula = formula,
        engine = engine,
        status = status,
        restricted = normalizedName in restrictedNames
    )
}

private fun parsePointsItems(formula: String): List<PointsItem> {
    val segments = formula.split(",", ";")
    return segments.mapNotNull { segment ->
        val match = Regex("(.+?)=\\s*([0-9]+)").find(segment.trim()) ?: return@mapNotNull null
        val label = match.groupValues[1].trim().trim('+')
        if (label.isBlank() || label.startsWith(">") || label.startsWith("<") || label.contains("total", true)) return@mapNotNull null
        PointsItem(label = label, points = match.groupValues[2].toInt(), group = if (label.contains("Age", true)) "age" else null)
    }.distinctBy { it.label }
}

private fun parseEquationDefinition(formula: String): EquationDefinition? {
    val statements = formula.split(";", ". ").mapNotNull { rawStatement ->
        val parts = rawStatement.split("=", limit = 2)
        if (parts.size != 2) return@mapNotNull null
        val label = parts[0].trim()
        val expression = sanitizeExpression(parts[1]) ?: return@mapNotNull null
        EquationStatement(label = label, expression = expression)
    }
    if (statements.isEmpty()) return null
    val outputIds = statements.map { slug(it.label) }.toSet()
    val inputs = statements.flatMap { extractIdentifiers(it.expression) }
        .map(::slug)
        .distinct()
        .filterNot { it in outputIds || it in setOf("sqrt", "ln", "exp") }
    return if (inputs.isEmpty()) null else EquationDefinition(statements = statements, inputs = inputs)
}

private fun sanitizeExpression(raw: String): String? {
    var expression = raw.substringBefore(" abnormal", raw, true)
        .substringBefore(" normal", raw, true)
        .substringBefore(" prolonged", raw, true)
        .substringBefore(" risk", raw, true)
        .substringBefore(" guideline", raw, true)
        .trim()
        .replace("×", "*")
        .replace("−", "-")
        .replace("–", "-")
        .replace("÷", "/")
        .replace("√", "sqrt")
        .replace("[", "")
        .replace("]", "")
        .replace("{", "")
        .replace("}", "")
        .replace("+/-", "")
        .replace("±", "")
        .replace(" ", "")
    expression = expression.replace(Regex("sqrt([A-Za-z][A-Za-z0-9]*)"), "sqrt($1)")
    expression = expression.replace(Regex("(\\d)(\\()"), "$1*$2")
    expression = expression.replace(Regex("([A-Za-z0-9_])\\("), "$1*(")
    expression = expression.replace("ln*", "ln").replace("sqrt*", "sqrt").replace("exp*", "exp")
    if (!expression.contains(Regex("[+\\-*/^]"))) return null
    if (!expression.matches(Regex("[A-Za-z0-9_().,+\\-*/^]+"))) return null
    return expression
}

private fun evaluateEquation(definition: EquationDefinition, values: Map<String, String>): List<EquationResult> {
    val env = mutableMapOf<String, Double>()
    definition.inputs.forEach { input ->
        val numeric = values[input]?.replace(',', '.')?.toDoubleOrNull() ?: return emptyList()
        env[input] = numeric
    }
    return buildList {
        definition.statements.forEach { statement ->
            val value = ExpressionParser(statement.expression, env).parse()
            env[slug(statement.label)] = value
            add(EquationResult(label = statement.label, value = value))
        }
    }
}

private class ExpressionParser(
    private val source: String,
    private val values: Map<String, Double>
) {
    private var index = 0

    fun parse(): Double {
        val result = parseExpression()
        if (index < source.length) error("Unexpected token at $index")
        return result
    }

    private fun parseExpression(): Double {
        var value = parseTerm()
        while (index < source.length) {
            value = when (source[index]) {
                '+' -> {
                    index += 1
                    value + parseTerm()
                }
                '-' -> {
                    index += 1
                    value - parseTerm()
                }
                else -> return value
            }
        }
        return value
    }

    private fun parseTerm(): Double {
        var value = parseFactor()
        while (index < source.length) {
            value = when (source[index]) {
                '*' -> {
                    index += 1
                    value * parseFactor()
                }
                '/' -> {
                    index += 1
                    value / parseFactor()
                }
                else -> return value
            }
        }
        return value
    }

    private fun parseFactor(): Double {
        var value = parseUnary()
        while (index < source.length && source[index] == '^') {
            index += 1
            value = value.pow(parseUnary())
        }
        return value
    }

    private fun parseUnary(): Double {
        return when {
            index < source.length && source[index] == '-' -> {
                index += 1
                -parseUnary()
            }
            else -> parsePrimary()
        }
    }

    private fun parsePrimary(): Double {
        if (index >= source.length) error("Unexpected end of expression")
        return when {
            source[index].isDigit() || source[index] == '.' -> parseNumber()
            source[index] == '(' -> {
                index += 1
                val value = parseExpression()
                require(source.getOrNull(index) == ')')
                index += 1
                value
            }
            source[index].isLetter() -> parseIdentifier()
            else -> error("Unexpected token ${source[index]}")
        }
    }

    private fun parseNumber(): Double {
        val start = index
        while (index < source.length && (source[index].isDigit() || source[index] == '.')) index += 1
        return source.substring(start, index).toDouble()
    }

    private fun parseIdentifier(): Double {
        val start = index
        while (index < source.length && (source[index].isLetterOrDigit() || source[index] == '_')) index += 1
        val identifier = source.substring(start, index).lowercase()
        return if (index < source.length && source[index] == '(') {
            index += 1
            val value = parseExpression()
            require(source.getOrNull(index) == ')')
            index += 1
            when (identifier) {
                "sqrt" -> sqrt(value)
                "ln" -> kotlin.math.ln(value)
                "exp" -> E.pow(value)
                else -> error("Unknown function $identifier")
            }
        } else {
            values[identifier] ?: error("Missing variable $identifier")
        }
    }
}

private fun resolveEvidence(tool: RegistryTool, masterPrompt: String): SourceEvidence {
    if (masterPrompt.isBlank()) {
        return SourceEvidence(
            citation = null,
            pmid = null,
            sourceNote = "Loaded from the bundled catalog asset only; the merged master prompt asset was unavailable."
        )
    }
    val patterns = listOfNotNull(
        tool.acronym?.let { """"acronym": "${Regex.escape(it)}"""" },
        Regex.escape(tool.name),
        Regex.escape(tool.name.substringBefore(" ("))
    )
    val index = patterns.firstNotNullOfOrNull { pattern ->
        Regex(pattern).find(masterPrompt)?.range?.first
    }
    val window = if (index == null) "" else masterPrompt.substring(maxOf(0, index - 700), minOf(masterPrompt.length, index + 1800))
    val citation = Regex("original_citation\":\\s*\"([^\"]+)\"").find(window)?.groupValues?.get(1)
        ?: Regex("PMID:\\s*(\\d{6,9})").find(window)?.let { "PMID-bearing source present near this entry in the merged prompt." }
    val pmid = Regex("PMID:\\s*(\\d{6,9})").find(window)?.groupValues?.get(1)
    val sourceNote = buildString {
        append("Bundled offline from the merged catalog asset.")
        if (tool.status == ToolStatus.NeedsVerification) append(" Conservative verification status kept because the source text contains ambiguity, a cross-reference, or a restricted instrument marker.")
        if (window.isNotBlank()) append(" Nearby entry text was also found in the merged master prompt for on-device evidence lookup.")
    }
    return SourceEvidence(citation = citation, pmid = pmid, sourceNote = sourceNote)
}

private fun loadTextAsset(context: Context, name: String): String {
    return runCatching { context.assets.open(name).bufferedReader().use { it.readText() } }.getOrDefault("")
}

private fun RegistryTool.matchesQuery(query: String): Boolean {
    if (query.isBlank()) return true
    val needle = normalizeSearch(query)
    return listOf(name, acronym.orEmpty(), category, purpose, *aliases.toTypedArray())
        .joinToString(" ")
        .let(::normalizeSearch)
        .contains(needle)
}

private fun normalizeSearch(value: String): String {
    return value.lowercase().filter { it.isLetterOrDigit() }
}

private fun inferAcronym(name: String): String? {
    val inParens = Regex("\\(([^)]+)\\)").find(name)?.groupValues?.get(1)?.trim()
    if (!inParens.isNullOrBlank() && inParens.any(Char::isUpperCase)) return inParens
    return if (name.length <= 8 && name.any(Char::isDigit)) name else null
}

private fun prettyToken(token: String): String {
    val shortForms = mapOf(
        "na" to "Na",
        "cl" to "Cl",
        "hco3" to "HCO3",
        "sbp" to "SBP",
        "dbp" to "DBP",
        "hr" to "HR",
        "rr" to "RR",
        "bun" to "BUN",
        "cr" to "Cr",
        "etoh" to "EtOH",
        "qt" to "QT",
        "qtc" to "QTc",
        "paco2" to "PaCO2"
    )
    return shortForms[token] ?: token.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun extractIdentifiers(expression: String): List<String> {
    return Regex("\\b[A-Za-z][A-Za-z0-9_]*\\b").findAll(expression).map { it.value }.toList()
}

private fun slug(value: String): String {
    return value.lowercase().map { char ->
        when {
            char.isLetterOrDigit() -> char
            else -> '_'
        }
    }.joinToString("").replace(Regex("_+"), "_").trim('_')
}

private fun formatNumber(value: Double): String {
    val rounded = String.format("%.2f", value)
    return rounded.trimEnd('0').trimEnd('.')
}

private fun meshBrush(dark: Boolean): Brush {
    return Brush.verticalGradient(
        colors = if (dark) {
            listOf(Color(0xFF07110F), Color(0xFF12221E), Color(0xFF09110E))
        } else {
            listOf(Color(0xFFE9F5EF), Paper, Color(0xFFF8EDE6))
        }
    )
}

private fun categoryColor(category: String): Color {
    val palette = listOf(
        Color(0xFF23796E),
        Color(0xFF3E6FB1),
        Color(0xFF8B5FBF),
        Color(0xFFA4634D),
        Color(0xFF5B8B63),
        Color(0xFF9E6C8B)
    )
    return palette[(normalizeSearch(category).hashCode().absoluteValue % palette.size)]
}

private fun shareTool(context: Context, tool: RegistryTool, summary: String, evidence: SourceEvidence) {
    val body = buildString {
        appendLine(tool.name)
        appendLine(tool.purpose)
        appendLine()
        appendLine("Summary: $summary")
        appendLine("Formula: ${tool.formula}")
        evidence.citation?.let { appendLine("Citation: $it") }
        evidence.pmid?.let { appendLine("PMID: $it") }
        appendLine()
        appendLine("Clinicalc is educational and decision support only.")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, tool.name)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(intent, "Share score summary"))
}
