package com.example.ui.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shortcut
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProjectEntity
import com.example.util.StringsHelper
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.ProjectFilter
import com.example.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigateToWizard: () -> Unit,
    onNavigateToRunner: (String) -> Unit,
    onNavigateToEditor: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val context = LocalContext.current
    val language by viewModel.prefs.language.collectAsState()
    val viewMode by viewModel.prefs.viewMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val projects by viewModel.projects.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var selectedProjectForAction by remember { mutableStateOf<ProjectEntity?>(null) }
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }
    var projectToExport by remember { mutableStateOf<ProjectEntity?>(null) }

    // SAF Import Launcher (.htapk or zip)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                viewModel.importProject(stream)
            }
        }
    }

    // SAF Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let {
            val proj = projectToExport
            if (proj != null) {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    viewModel.exportProject(proj.id, stream)
                }
            }
            projectToExport = null
        }
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = StringsHelper.get("app_title", language),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                viewModel.prefs.setViewMode(if (viewMode == "grid") "list" else "grid")
                            }
                        ) {
                            Icon(
                                imageVector = if (viewMode == "grid") Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "View Mode"
                            )
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(StringsHelper.get("import_package", language)) },
                                onClick = {
                                    showMenu = false
                                    importLauncher.launch(arrayOf("*/*"))
                                },
                                leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(StringsHelper.get("load_sample_template", language)) },
                                onClick = {
                                    showMenu = false
                                    viewModel.createSampleApp()
                                },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(StringsHelper.get("settings_title", language)) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToSettings()
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(StringsHelper.get("about", language)) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToAbout()
                                },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text(StringsHelper.get("search_placeholder", language)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Filters Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeFilter == ProjectFilter.ALL,
                        onClick = { viewModel.setActiveFilter(ProjectFilter.ALL) },
                        label = { Text(StringsHelper.get("filter_all", language)) }
                    )
                    FilterChip(
                        selected = activeFilter == ProjectFilter.FULLSCREEN,
                        onClick = { viewModel.setActiveFilter(ProjectFilter.FULLSCREEN) },
                        label = { Text(StringsHelper.get("filter_fullscreen", language)) }
                    )
                    FilterChip(
                        selected = activeFilter == ProjectFilter.OFFLINE,
                        onClick = { viewModel.setActiveFilter(ProjectFilter.OFFLINE) },
                        label = { Text(StringsHelper.get("filter_offline", language)) }
                    )
                    FilterChip(
                        selected = activeFilter == ProjectFilter.PIN,
                        onClick = { viewModel.setActiveFilter(ProjectFilter.PIN) },
                        label = { Text(StringsHelper.get("filter_pin", language)) }
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToWizard,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(StringsHelper.get("new_app_btn", language)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (projects.isEmpty()) {
                if (searchQuery.isBlank() && activeFilter == ProjectFilter.ALL) {
                    // Empty state: Welcome Card
                    WelcomeCard(
                        language = language,
                        onCreateApp = onNavigateToWizard,
                        onImportApp = { importLauncher.launch(arrayOf("*/*")) },
                        onLoadSample = { viewModel.createSampleApp() },
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = StringsHelper.get("no_apps_found", language),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                if (viewMode == "grid") {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(projects, key = { it.id }) { project ->
                            ProjectGridItem(
                                project = project,
                                onClick = { onNavigateToRunner(project.id) },
                                onLongClick = { selectedProjectForAction = project },
                                onMenuClick = { selectedProjectForAction = project }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(projects, key = { it.id }) { project ->
                            ProjectListItem(
                                project = project,
                                onClick = { onNavigateToRunner(project.id) },
                                onMenuClick = { selectedProjectForAction = project }
                            )
                        }
                    }
                }
            }

            // Context Action Menu / Bottom Sheet
            selectedProjectForAction?.let { project ->
                AlertDialog(
                    onDismissRequest = { selectedProjectForAction = null },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProjectIconBox(project = project, size = 36.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(project.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    text = {
                        Column {
                            ActionMenuItem(
                                icon = Icons.Default.PlayArrow,
                                label = StringsHelper.get("menu_run", language),
                                onClick = {
                                    selectedProjectForAction = null
                                    onNavigateToRunner(project.id)
                                }
                            )
                            ActionMenuItem(
                                icon = Icons.Default.Edit,
                                label = StringsHelper.get("menu_edit", language),
                                onClick = {
                                    selectedProjectForAction = null
                                    onNavigateToEditor(project.id)
                                }
                            )
                            ActionMenuItem(
                                icon = Icons.Default.Shortcut,
                                label = StringsHelper.get("menu_shortcut", language),
                                onClick = {
                                    selectedProjectForAction = null
                                    viewModel.pinShortcut(project.id)
                                }
                            )
                            ActionMenuItem(
                                icon = Icons.Default.FileDownload,
                                label = StringsHelper.get("menu_export", language),
                                onClick = {
                                    selectedProjectForAction = null
                                    projectToExport = project
                                    exportLauncher.launch("${project.name}.htapk")
                                }
                            )
                            ActionMenuItem(
                                icon = Icons.Default.ContentCopy,
                                label = StringsHelper.get("menu_duplicate", language),
                                onClick = {
                                    selectedProjectForAction = null
                                    viewModel.duplicateProject(project.id, project.name)
                                }
                            )
                            ActionMenuItem(
                                icon = Icons.Default.Delete,
                                label = StringsHelper.get("menu_delete", language),
                                isDestructive = true,
                                onClick = {
                                    selectedProjectForAction = null
                                    projectToDelete = project
                                }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { selectedProjectForAction = null }) {
                            Text(StringsHelper.get("cancel", language))
                        }
                    }
                )
            }

            // Delete Confirmation Dialog
            projectToDelete?.let { project ->
                AlertDialog(
                    onDismissRequest = { projectToDelete = null },
                    title = { Text(StringsHelper.get("delete", language)) },
                    text = { Text("${project.name}: ${StringsHelper.get("confirm_delete", language)}") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteProject(project.id)
                                projectToDelete = null
                            }
                        ) {
                            Text(StringsHelper.get("delete", language), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { projectToDelete = null }) {
                            Text(StringsHelper.get("cancel", language))
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProjectGridItem(
    project: ProjectEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                ProjectIconBox(project = project, size = 52.dp)
                IconButton(onClick = onMenuClick, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = project.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "v${project.version} • ${project.author}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (project.fullscreenMode != "off") {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (project.hasPinLock) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "PIN",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (project.isOfflineOnly) {
                    Icon(
                        imageVector = Icons.Default.PublicOff,
                        contentDescription = "Offline",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectListItem(
    project: ProjectEntity,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProjectIconBox(project = project, size = 48.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "v${project.version} • ${project.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
            }
        }
    }
}

@Composable
fun ProjectIconBox(project: ProjectEntity, size: androidx.compose.ui.unit.Dp) {
    val bgColor = try {
        Color(android.graphics.Color.parseColor(project.iconBackground))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        val label = project.iconEmoji.ifBlank { project.iconInitialLetter.ifBlank { project.name.take(1) } }
        Text(
            text = label,
            fontSize = (size.value * 0.45).sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun WelcomeCard(
    language: String,
    onCreateApp: () -> Unit,
    onImportApp: () -> Unit,
    onLoadSample: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = StringsHelper.get("welcome_title", language),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = StringsHelper.get("welcome_desc", language),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onCreateApp,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(StringsHelper.get("create_first_app", language))
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onImportApp,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(StringsHelper.get("import_package", language))
            }
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(
                onClick = onLoadSample,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(StringsHelper.get("load_sample_template", language))
            }
        }
    }
}

@Composable
fun ActionMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}
