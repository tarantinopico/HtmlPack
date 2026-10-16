package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.about.AboutScreen
import com.example.ui.editor.EditorScreen
import com.example.ui.home.HomeScreen
import com.example.ui.runner.HtmlRunnerScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.HTMLPackTheme
import com.example.ui.wizard.WizardScreen
import com.example.util.ShortcutHelper
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.Screen
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleLaunchIntent(intent)

        setContent {
            val themeMode by viewModel.prefs.themeMode.collectAsState()
            val language by viewModel.prefs.language.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()

            val isDarkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            LaunchedEffect(Unit) {
                viewModel.userMessage.collectLatest { msg ->
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                }
            }

            HTMLPackTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (val screen = currentScreen) {
                        is Screen.Home -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToWizard = { viewModel.startNewAppWizard() },
                                onNavigateToRunner = { id -> viewModel.navigateTo(Screen.Runner(id)) },
                                onNavigateToEditor = { id -> viewModel.loadProjectForEditor(id) },
                                onNavigateToSettings = { viewModel.navigateTo(Screen.Settings) },
                                onNavigateToAbout = { viewModel.navigateTo(Screen.About) }
                            )
                        }
                        is Screen.Wizard -> {
                            WizardScreen(
                                viewModel = viewModel,
                                onNavigateBack = { viewModel.navigateBack() }
                            )
                        }
                        is Screen.Editor -> {
                            EditorScreen(
                                projectId = screen.projectId,
                                viewModel = viewModel,
                                initialTab = screen.tab,
                                onNavigateBack = { viewModel.navigateBack() },
                                onNavigateToRunner = { id -> viewModel.navigateTo(Screen.Runner(id)) }
                            )
                        }
                        is Screen.Runner -> {
                            HtmlRunnerScreen(
                                projectId = screen.projectId,
                                repository = viewModel.repository,
                                language = language,
                                onNavigateHome = {
                                    if (!viewModel.navigateBack()) {
                                        viewModel.navigateTo(Screen.Home)
                                    }
                                }
                            )
                        }
                        is Screen.Settings -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { viewModel.navigateBack() }
                            )
                        }
                        is Screen.About -> {
                            AboutScreen(
                                viewModel = viewModel,
                                onNavigateBack = { viewModel.navigateBack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent == null) return

        // 1. Check for shortcut extra
        val shortcutProjectId = intent.getStringExtra(ShortcutHelper.EXTRA_PROJECT_ID)
        if (!shortcutProjectId.isNullOrBlank()) {
            viewModel.navigateTo(Screen.Runner(shortcutProjectId))
            return
        }

        // 2. Check for deep link URI htmlpack://run?id=...
        val data: Uri? = intent.data
        if (data != null && data.scheme == "htmlpack" && data.host == "run") {
            val id = data.getQueryParameter("id")
            if (!id.isNullOrBlank()) {
                viewModel.navigateTo(Screen.Runner(id))
                return
            }
        }

        // 3. Check for .htapk file open intent
        if (data != null && (intent.action == Intent.ACTION_VIEW)) {
            try {
                contentResolver.openInputStream(data)?.use { stream ->
                    viewModel.importProject(stream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
