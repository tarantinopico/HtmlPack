package com.example.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shortcut
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PinLockConfig
import com.example.data.model.ProjectFileItem
import com.example.data.model.ProjectManifest
import com.example.util.StringsHelper
import com.example.viewmodel.AppViewModel
import java.security.MessageDigest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectId: String,
    viewModel: AppViewModel,
    initialTab: Int = 0,
    onNavigateBack: () -> Unit,
    onNavigateToRunner: (String) -> Unit
) {
    val language by viewModel.prefs.language.collectAsState()
    val manifest by viewModel.activeEditorManifest.collectAsState()
    val files by viewModel.activeProjectFiles.collectAsState()
    val viewingFile by viewModel.viewingFileContent.collectAsState()

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var showAddFileDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var newFileContent by remember { mutableStateOf("") }

    BackHandler {
        onNavigateBack()
    }

    if (manifest == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val current = manifest!!
    val tabs = listOf(
        StringsHelper.get("tab_basic", language),
        StringsHelper.get("tab_window", language),
        StringsHelper.get("tab_webview", language),
        StringsHelper.get("tab_behavior", language),
        StringsHelper.get("tab_security", language),
        StringsHelper.get("tab_files", language)
    )

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = current.meta.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.pinShortcut(projectId) }) {
                            Icon(Icons.Default.Shortcut, contentDescription = "Shortcut")
                        }
                        IconButton(onClick = { onNavigateToRunner(projectId) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run")
                        }
                        IconButton(onClick = { viewModel.saveEditorManifest() }) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 16.dp,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> BasicSettingsTab(
                    manifest = current,
                    onManifestChange = { viewModel.updateEditorManifest(it) }
                )
                1 -> WindowSettingsTab(
                    manifest = current,
                    onManifestChange = { viewModel.updateEditorManifest(it) }
                )
                2 -> WebviewSettingsTab(
                    manifest = current,
                    onManifestChange = { viewModel.updateEditorManifest(it) }
                )
                3 -> BehaviorSettingsTab(
                    manifest = current,
                    onManifestChange = { viewModel.updateEditorManifest(it) }
                )
                4 -> SecuritySettingsTab(
                    manifest = current,
                    onManifestChange = { viewModel.updateEditorManifest(it) }
                )
                5 -> FilesManagerTab(
                    projectId = projectId,
                    files = files,
                    onViewFile = { relPath -> viewModel.viewFileContent(projectId, relPath) },
                    onDeleteFile = { relPath -> viewModel.deleteProjectFile(projectId, relPath) },
                    onAddFile = { showAddFileDialog = true },
                    language = language
                )
            }

            // File Content Viewer Dialog
            viewingFile?.let { (relPath, content) ->
                AlertDialog(
                    onDismissRequest = { viewModel.closeFileViewer() },
                    title = { Text(relPath, maxLines = 1) },
                    text = {
                        Column {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                            ) {
                                LazyColumn(modifier = Modifier.padding(12.dp)) {
                                    items(content.lines()) { line ->
                                        Text(
                                            text = line,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.closeFileViewer() }) {
                            Text("Zavřít")
                        }
                    }
                )
            }

            // Add File Dialog
            if (showAddFileDialog) {
                AlertDialog(
                    onDismissRequest = { showAddFileDialog = false },
                    title = { Text(StringsHelper.get("add_file", language)) },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = newFileName,
                                onValueChange = { newFileName = it },
                                label = { Text("Název souboru (např. custom.css)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = newFileContent,
                                onValueChange = { newFileContent = it },
                                label = { Text("Obsah") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newFileName.isNotBlank()) {
                                    viewModel.addProjectFile(projectId, newFileName.trim(), newFileContent)
                                    showAddFileDialog = false
                                    newFileName = ""
                                    newFileContent = ""
                                }
                            }
                        ) {
                            Text(StringsHelper.get("save", language))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddFileDialog = false }) {
                            Text(StringsHelper.get("cancel", language))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BasicSettingsTab(
    manifest: ProjectManifest,
    onManifestChange: (ProjectManifest) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
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
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = manifest.source.entry,
            onValueChange = { onManifestChange(manifest.copy(source = manifest.source.copy(entry = it))) },
            label = { Text("Výchozí soubor (Entry Point)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = manifest.icon.emoji,
            onValueChange = { onManifestChange(manifest.copy(icon = manifest.icon.copy(emoji = it))) },
            label = { Text("Ikona / Emoji") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun WindowSettingsTab(
    manifest: ProjectManifest,
    onManifestChange: (ProjectManifest) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Režim celé obrazovky", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("off" to "Vypnuto", "edge_to_edge" to "Edge-to-Edge", "immersive" to "Immersive", "hide_status" to "Skrýt status").forEach { (valKey, label) ->
                FilterChip(
                    selected = manifest.window.fullscreen == valKey,
                    onClick = { onManifestChange(manifest.copy(window = manifest.window.copy(fullscreen = valKey))) },
                    label = { Text(label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Orientace displeje", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("auto" to "Auto", "portrait" to "Portrét", "landscape" to "Na šířku").forEach { (valKey, label) ->
                FilterChip(
                    selected = manifest.window.orientation == valKey,
                    onClick = { onManifestChange(manifest.copy(window = manifest.window.copy(orientation = valKey))) },
                    label = { Text(label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Udržovat rozsvícený displej", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = manifest.window.keepScreenOn,
                onCheckedChange = { onManifestChange(manifest.copy(window = manifest.window.copy(keepScreenOn = it))) }
            )
        }
    }
}

@Composable
fun WebviewSettingsTab(
    manifest: ProjectManifest,
    onManifestChange: (ProjectManifest) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Povolit JavaScript", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = manifest.webview.javaScript,
                onCheckedChange = { onManifestChange(manifest.copy(webview = manifest.webview.copy(javaScript = it))) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Povolit DOM Storage (localStorage)", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = manifest.webview.domStorage,
                onCheckedChange = { onManifestChange(manifest.copy(webview = manifest.webview.copy(domStorage = it))) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Hardwarová akcelerace", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = manifest.webview.hardwareAcceleration,
                onCheckedChange = { onManifestChange(manifest.copy(webview = manifest.webview.copy(hardwareAcceleration = it))) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Podpora gest přiblížení (Zoom)", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = manifest.webview.zoom,
                onCheckedChange = { onManifestChange(manifest.copy(webview = manifest.webview.copy(zoom = it))) }
            )
        }
    }
}

@Composable
fun BehaviorSettingsTab(
    manifest: ProjectManifest,
    onManifestChange: (ProjectManifest) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Splash screen", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = manifest.behavior.splash.enabled,
                onCheckedChange = {
                    onManifestChange(manifest.copy(behavior = manifest.behavior.copy(splash = manifest.behavior.splash.copy(enabled = it))))
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Tlačítko Zpět", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("history" to "Historie stránek", "close" to "Zavřít").forEach { (valKey, label) ->
                FilterChip(
                    selected = manifest.behavior.backButton == valKey,
                    onClick = { onManifestChange(manifest.copy(behavior = manifest.behavior.copy(backButton = valKey))) },
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
fun SecuritySettingsTab(
    manifest: ProjectManifest,
    onManifestChange: (ProjectManifest) -> Unit
) {
    var pinText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Offline-only režim", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = manifest.urlPolicy.offlineOnly,
                onCheckedChange = { onManifestChange(manifest.copy(urlPolicy = manifest.urlPolicy.copy(offlineOnly = it))) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Zámek PIN kódem", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = manifest.security.pinLock.enabled,
                onCheckedChange = { enabled ->
                    onManifestChange(
                        manifest.copy(security = manifest.security.copy(pinLock = manifest.security.pinLock.copy(enabled = enabled)))
                    )
                }
            )
        }

        if (manifest.security.pinLock.enabled) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = pinText,
                onValueChange = {
                    pinText = it
                    if (it.length >= 4) {
                        val md = MessageDigest.getInstance("SHA-256")
                        val digest = md.digest(it.toByteArray())
                        val hash = digest.fold("") { str, b -> str + "%02x".format(b) }
                        onManifestChange(
                            manifest.copy(security = manifest.security.copy(pinLock = PinLockConfig(enabled = true, pinHash = hash)))
                        )
                    }
                },
                label = { Text("Změnit 4-místný PIN") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun FilesManagerTab(
    projectId: String,
    files: List<ProjectFileItem>,
    onViewFile: (String) -> Unit,
    onDeleteFile: (String) -> Unit,
    onAddFile: () -> Unit,
    language: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${StringsHelper.get("files_title", language)} (${files.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = onAddFile) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(StringsHelper.get("add_file", language))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (files.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(StringsHelper.get("files_empty", language), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(files, key = { it.relativePath }) { fileItem ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fileItem.relativePath,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${fileItem.size / 1024 + 1} KB • ${fileItem.mimeType}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onViewFile(fileItem.relativePath) }) {
                                Icon(Icons.Default.Visibility, contentDescription = "View")
                            }
                            if (fileItem.relativePath != "index.html") {
                                IconButton(onClick = { onDeleteFile(fileItem.relativePath) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
