package com.example.ui.wizard

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shortcut
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PinLockConfig
import com.example.data.model.ProjectManifest
import com.example.util.HtmlDetector
import com.example.util.StringsHelper
import com.example.viewmodel.AppViewModel
import java.security.MessageDigest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val language by viewModel.prefs.language.collectAsState()
    val manifest by viewModel.wizardManifest.collectAsState()
    val currentHtml by viewModel.wizardHtml.collectAsState()

    var currentStep by remember { mutableIntStateOf(1) }
    var pinInput by remember { mutableStateOf("") }

    val openHtmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val content = stream.bufferedReader().readText()
                viewModel.updateWizardHtml(content)

                // Auto-detect metadata
                val detected = HtmlDetector.parseHtml(content)
                val updatedMeta = manifest.meta.copy(
                    name = detected.title ?: manifest.meta.name,
                    description = detected.description ?: manifest.meta.description,
                    author = detected.author ?: manifest.meta.author
                )
                val updatedIcon = if (detected.themeColor != null) {
                    manifest.icon.copy(background = detected.themeColor)
                } else manifest.icon

                viewModel.updateWizardManifest(
                    manifest.copy(meta = updatedMeta, icon = updatedIcon)
                )
            }
        }
    }

    BackHandler {
        if (currentStep > 1) {
            currentStep--
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Krok $currentStep z 5: ${getStepTitle(currentStep, language)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentStep > 1) currentStep-- else onNavigateBack()
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(onClick = { currentStep-- }) {
                            Text(StringsHelper.get("back", language))
                        }
                    } else {
                        TextButton(onClick = onNavigateBack) {
                            Text(StringsHelper.get("cancel", language))
                        }
                    }

                    if (currentStep < 5) {
                        Button(onClick = { currentStep++ }) {
                            Text(StringsHelper.get("next", language))
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.saveWizardProject(thenRun = false)
                            }
                        ) {
                            Text(StringsHelper.get("save", language))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            when (currentStep) {
                1 -> StepSource(
                    htmlContent = currentHtml,
                    onOpenHtml = { openHtmlLauncher.launch(arrayOf("text/html", "*/*")) },
                    onHtmlChange = { viewModel.updateWizardHtml(it) },
                    language = language
                )
                2 -> StepIdentity(
                    manifest = manifest,
                    onManifestChange = { viewModel.updateWizardManifest(it) },
                    language = language
                )
                3 -> StepWindow(
                    manifest = manifest,
                    onManifestChange = { viewModel.updateWizardManifest(it) },
                    language = language
                )
                4 -> StepBehavior(
                    manifest = manifest,
                    pinInput = pinInput,
                    onPinChange = {
                        pinInput = it
                        val hash = if (it.isNotBlank()) hashPin(it) else ""
                        viewModel.updateWizardManifest(
                            manifest.copy(
                                security = manifest.security.copy(
                                    pinLock = PinLockConfig(enabled = it.isNotBlank(), pinHash = hash)
                                )
                            )
                        )
                    },
                    onManifestChange = { viewModel.updateWizardManifest(it) },
                    language = language
                )
                5 -> StepSummary(
                    manifest = manifest,
                    onSaveAndRun = { viewModel.saveWizardProject(thenRun = true) },
                    onSaveAndShortcut = { viewModel.saveWizardProject(thenPin = true) },
                    language = language
                )
            }
        }
    }
}

@Composable
fun StepSource(
    htmlContent: String,
    onOpenHtml: () -> Unit,
    onHtmlChange: (String) -> Unit,
    language: String
) {
    Column {
        Text(
            text = "Vyberte výchozí HTML soubor",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Můžete načíst soubor z paměti telefonu nebo začít s výchozí šablonou.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedButton(
            onClick = onOpenHtml,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.FileOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Načíst soubor HTML (SAF)")
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Náhled zdrojového kódu (index.html):",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = htmlContent,
            onValueChange = onHtmlChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun StepIdentity(
    manifest: ProjectManifest,
    onManifestChange: (ProjectManifest) -> Unit,
    language: String
) {
    val colorPresets = listOf(
        "#1E3A8A", "#0F766E", "#7C3AED", "#DC2626", "#D97706", "#059669", "#0F172A"
    )
    val emojiPresets = listOf("⚡", "🚀", "📱", "🧮", "🎮", "📝", "🌐", "📊", "🛠️", "⭐")

    Column {
        Text(
            text = "Identita aplikace",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = manifest.meta.name,
            onValueChange = { onManifestChange(manifest.copy(meta = manifest.meta.copy(name = it))) },
            label = { Text("Název aplikace") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = manifest.meta.version,
                onValueChange = { onManifestChange(manifest.copy(meta = manifest.meta.copy(version = it))) },
                label = { Text("Verze") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = manifest.meta.author,
                onValueChange = { onManifestChange(manifest.copy(meta = manifest.meta.copy(author = it))) },
                label = { Text("Autor") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = manifest.meta.description,
            onValueChange = { onManifestChange(manifest.copy(meta = manifest.meta.copy(description = it))) },
            label = { Text("Popis") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Ikona aplikace",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Live icon preview
        Row(verticalAlignment = Alignment.CenterVertically) {
            val bg = try {
                Color(android.graphics.Color.parseColor(manifest.icon.background))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = manifest.icon.emoji.ifBlank { manifest.icon.initialLetter.ifBlank { manifest.meta.name.take(1) } },
                    fontSize = 28.sp,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Náhled ikony na ploše", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Klepnutím níže vyberte barvu a symbol", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Barva pozadí:", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colorPresets.forEach { hex ->
                val c = Color(android.graphics.Color.parseColor(hex))
                val isSelected = manifest.icon.background.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(c)
                        .clickable {
                            onManifestChange(manifest.copy(icon = manifest.icon.copy(background = hex)))
                        }
                        .then(
                            if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Symbol / Emoji:", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            emojiPresets.take(7).forEach { emoji ->
                val isSelected = manifest.icon.emoji == emoji
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable {
                            onManifestChange(manifest.copy(icon = manifest.icon.copy(emoji = emoji)))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 20.sp)
                }
            }
        }
    }
}

@Composable
fun StepWindow(
    manifest: ProjectManifest,
    onManifestChange: (ProjectManifest) -> Unit,
    language: String
) {
    Column {
        Text(
            text = "Nastavení okna a zobrazení",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Režim celé obrazovky (Fullscreen):", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = manifest.window.fullscreen == "off",
                onClick = { onManifestChange(manifest.copy(window = manifest.window.copy(fullscreen = "off"))) },
                label = { Text("Vypnuto") }
            )
            FilterChip(
                selected = manifest.window.fullscreen == "edge_to_edge",
                onClick = { onManifestChange(manifest.copy(window = manifest.window.copy(fullscreen = "edge_to_edge"))) },
                label = { Text("Edge-to-edge") }
            )
            FilterChip(
                selected = manifest.window.fullscreen == "immersive",
                onClick = { onManifestChange(manifest.copy(window = manifest.window.copy(fullscreen = "immersive"))) },
                label = { Text("Immersive") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Orientace obrazovky:", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = manifest.window.orientation == "auto",
                onClick = { onManifestChange(manifest.copy(window = manifest.window.copy(orientation = "auto"))) },
                label = { Text("Auto") }
            )
            FilterChip(
                selected = manifest.window.orientation == "portrait",
                onClick = { onManifestChange(manifest.copy(window = manifest.window.copy(orientation = "portrait"))) },
                label = { Text("Portrét") }
            )
            FilterChip(
                selected = manifest.window.orientation == "landscape",
                onClick = { onManifestChange(manifest.copy(window = manifest.window.copy(orientation = "landscape"))) },
                label = { Text("Na šířku") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Udržovat rozsvícený displej", style = MaterialTheme.typography.bodyLarge)
                Text("Zabrání zhasnutí obrazovky při spuštění", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = manifest.window.keepScreenOn,
                onCheckedChange = { onManifestChange(manifest.copy(window = manifest.window.copy(keepScreenOn = it))) }
            )
        }
    }
}

@Composable
fun StepBehavior(
    manifest: ProjectManifest,
    pinInput: String,
    onPinChange: (String) -> Unit,
    onManifestChange: (ProjectManifest) -> Unit,
    language: String
) {
    Column {
        Text(
            text = "Chování a zabezpečení",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Úvodní obrazovka (Splash Screen)", style = MaterialTheme.typography.bodyLarge)
                Text("Zobrazit logo a název při načítání", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = manifest.behavior.splash.enabled,
                onCheckedChange = {
                    onManifestChange(
                        manifest.copy(behavior = manifest.behavior.copy(splash = manifest.behavior.splash.copy(enabled = it)))
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Offline-only režim", style = MaterialTheme.typography.bodyLarge)
                Text("Zablokovat všechny síťové požadavky ven", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = manifest.urlPolicy.offlineOnly,
                onCheckedChange = {
                    onManifestChange(
                        manifest.copy(urlPolicy = manifest.urlPolicy.copy(offlineOnly = it))
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Zámek aplikace PIN kódem (volitelné):", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = pinInput,
            onValueChange = onPinChange,
            label = { Text("Zadejte 4-místný PIN kód") },
            placeholder = { Text("Ponechte prázdné pro bez zámku") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun StepSummary(
    manifest: ProjectManifest,
    onSaveAndRun: () -> Unit,
    onSaveAndShortcut: () -> Unit,
    language: String
) {
    Column {
        Text(
            text = "Souhrn nové aplikace",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val bg = try {
                        Color(android.graphics.Color.parseColor(manifest.icon.background))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(bg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = manifest.icon.emoji.ifBlank { manifest.meta.name.take(1) },
                            fontSize = 24.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(manifest.meta.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Verze ${manifest.meta.version} • ${manifest.meta.author}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Režim okna: ${manifest.window.fullscreen}", style = MaterialTheme.typography.bodyMedium)
                Text("Orientace: ${manifest.window.orientation}", style = MaterialTheme.typography.bodyMedium)
                Text("Zámek PIN: ${if (manifest.security.pinLock.enabled) "Aktivní" else "Vypnuto"}", style = MaterialTheme.typography.bodyMedium)
                Text("Režim: ${if (manifest.urlPolicy.offlineOnly) "Offline" else "Online"}", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onSaveAndRun,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(StringsHelper.get("run_now", language))
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onSaveAndShortcut,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Shortcut, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(StringsHelper.get("save_and_shortcut", language))
        }
    }
}

private fun getStepTitle(step: Int, language: String): String {
    return when (step) {
        1 -> StringsHelper.get("step_source", language)
        2 -> StringsHelper.get("step_identity", language)
        3 -> StringsHelper.get("step_window", language)
        4 -> StringsHelper.get("step_behavior", language)
        5 -> StringsHelper.get("step_summary", language)
        else -> ""
    }
}

private fun hashPin(pin: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(pin.toByteArray())
    return digest.fold("") { str, it -> str + "%02x".format(it) }
}
