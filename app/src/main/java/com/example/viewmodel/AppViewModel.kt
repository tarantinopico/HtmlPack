package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ProjectEntity
import com.example.data.model.IconConfig
import com.example.data.model.MetaConfig
import com.example.data.model.ProjectFileItem
import com.example.data.model.ProjectManifest
import com.example.data.model.SourceConfig
import com.example.data.model.WindowConfig
import com.example.data.repository.ProjectRepository
import com.example.data.storage.FileStorageManager
import com.example.data.storage.HtapkArchiveManager
import com.example.util.PreferencesManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

sealed interface Screen {
    data object Home : Screen
    data class Wizard(val step: Int = 1) : Screen
    data class Editor(val projectId: String, val tab: Int = 0) : Screen
    data class Runner(val projectId: String) : Screen
    data object Settings : Screen
    data object About : Screen
}

enum class ProjectFilter {
    ALL, FULLSCREEN, OFFLINE, PIN
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val fileStorage = FileStorageManager(application)
    private val archiveManager = HtapkArchiveManager(application, fileStorage)
    val repository = ProjectRepository(application, db, fileStorage, archiveManager)
    val prefs = PreferencesManager(application)

    // Current Screen
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Screen stack for Back navigation
    private val screenStack = mutableListOf<Screen>()

    // Search query & filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow(ProjectFilter.ALL)
    val activeFilter: StateFlow<ProjectFilter> = _activeFilter.asStateFlow()

    // Toast / Snack message events
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Wizard draft state
    private val _wizardManifest = MutableStateFlow(ProjectManifest())
    val wizardManifest: StateFlow<ProjectManifest> = _wizardManifest.asStateFlow()

    private val _wizardHtml = MutableStateFlow(getDefaultHtmlContent("Moje Aplikace"))
    val wizardHtml: StateFlow<String> = _wizardHtml.asSharedFlow().let {
        MutableStateFlow(getDefaultHtmlContent("Moje Aplikace"))
    }

    // Editor state
    private val _activeEditorManifest = MutableStateFlow<ProjectManifest?>(null)
    val activeEditorManifest: StateFlow<ProjectManifest?> = _activeEditorManifest.asStateFlow()

    private val _activeProjectFiles = MutableStateFlow<List<ProjectFileItem>>(emptyList())
    val activeProjectFiles: StateFlow<List<ProjectFileItem>> = _activeProjectFiles.asStateFlow()

    private val _viewingFileContent = MutableStateFlow<Pair<String, String>?>(null) // relativePath to content
    val viewingFileContent: StateFlow<Pair<String, String>?> = _viewingFileContent.asStateFlow()

    // Filtered project list
    val projects: StateFlow<List<ProjectEntity>> = combine(
        repository.allProjects,
        _searchQuery,
        _activeFilter
    ) { all, query, filter ->
        all.filter { project ->
            val matchesQuery = query.isBlank() ||
                project.name.contains(query, ignoreCase = true) ||
                project.tags.contains(query, ignoreCase = true) ||
                project.author.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                ProjectFilter.ALL -> true
                ProjectFilter.FULLSCREEN -> project.fullscreenMode != "off"
                ProjectFilter.OFFLINE -> project.isOfflineOnly
                ProjectFilter.PIN -> project.hasPinLock
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            screenStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (screenStack.isNotEmpty()) {
            _currentScreen.value = screenStack.removeAt(screenStack.size - 1)
            return true
        }
        return false
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setActiveFilter(filter: ProjectFilter) {
        _activeFilter.value = filter
    }

    fun showMessage(msg: String) {
        viewModelScope.launch { _userMessage.emit(msg) }
    }

    // Wizard actions
    fun startNewAppWizard() {
        val newId = UUID.randomUUID().toString()
        _wizardManifest.value = ProjectManifest(
            meta = MetaConfig(
                id = newId,
                name = "Nová Aplikace",
                version = "1.0.0",
                author = "Vývojář"
            ),
            icon = IconConfig(
                background = "#1E3A8A",
                initialLetter = "N",
                emoji = "🚀"
            ),
            source = SourceConfig(entry = "index.html"),
            window = WindowConfig(fullscreen = "edge_to_edge")
        )
        _wizardHtml.value = getDefaultHtmlContent("Nová Aplikace")
        navigateTo(Screen.Wizard(1))
    }

    fun updateWizardManifest(manifest: ProjectManifest) {
        _wizardManifest.value = manifest
    }

    fun updateWizardHtml(html: String) {
        _wizardHtml.value = html
    }

    fun saveWizardProject(thenRun: Boolean = false, thenPin: Boolean = false) {
        viewModelScope.launch {
            val manifest = _wizardManifest.value
            val html = _wizardHtml.value
            val success = repository.saveProject(manifest, html)
            if (success) {
                if (thenPin) {
                    repository.createDesktopShortcut(manifest)
                }
                if (thenRun) {
                    navigateTo(Screen.Runner(manifest.meta.id))
                } else {
                    navigateTo(Screen.Home)
                }
                showMessage("Aplikace '${manifest.meta.name}' byla uložena")
            } else {
                showMessage("Chyba při ukládání aplikace")
            }
        }
    }

    // Editor actions
    fun loadProjectForEditor(projectId: String, tab: Int = 0) {
        viewModelScope.launch {
            val manifest = repository.getProjectManifest(projectId)
            _activeEditorManifest.value = manifest
            refreshProjectFiles(projectId)
            navigateTo(Screen.Editor(projectId, tab))
        }
    }

    fun updateEditorManifest(manifest: ProjectManifest) {
        _activeEditorManifest.value = manifest
    }

    fun saveEditorManifest() {
        viewModelScope.launch {
            val manifest = _activeEditorManifest.value ?: return@launch
            val updated = manifest.copy(meta = manifest.meta.copy(updatedAt = System.currentTimeMillis()))
            val success = repository.saveProject(updated)
            if (success) {
                _activeEditorManifest.value = updated
                showMessage("Nastavení projektu uloženo")
            } else {
                showMessage("Chyba při ukládání")
            }
        }
    }

    fun refreshProjectFiles(projectId: String) {
        viewModelScope.launch {
            _activeProjectFiles.value = repository.listProjectFiles(projectId)
        }
    }

    fun viewFileContent(projectId: String, relativePath: String) {
        viewModelScope.launch {
            val content = repository.readFileContent(projectId, relativePath)
            if (content != null) {
                _viewingFileContent.value = relativePath to content
            } else {
                showMessage("Nelze zobrazit binární nebo nedostupný soubor")
            }
        }
    }

    fun closeFileViewer() {
        _viewingFileContent.value = null
    }

    fun deleteProjectFile(projectId: String, relativePath: String) {
        viewModelScope.launch {
            val success = repository.deleteFile(projectId, relativePath)
            if (success) {
                refreshProjectFiles(projectId)
                showMessage("Soubor $relativePath byl smazán")
            } else {
                showMessage("Chyba při mazání souboru")
            }
        }
    }

    fun addProjectFile(projectId: String, filename: String, content: String) {
        viewModelScope.launch {
            val success = repository.saveFileContent(projectId, filename, content)
            if (success) {
                refreshProjectFiles(projectId)
                showMessage("Soubor $filename byl přidán")
            } else {
                showMessage("Chyba při vytváření souboru")
            }
        }
    }

    // Home actions
    fun duplicateProject(id: String, name: String) {
        viewModelScope.launch {
            val newId = repository.duplicateProject(id, "$name (Kopie)")
            if (newId != null) {
                showMessage("Projekt byl duplikován")
            } else {
                showMessage("Chyba při duplikování projektu")
            }
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            val success = repository.deleteProject(id)
            if (success) {
                showMessage("Aplikace byla smazána")
            } else {
                showMessage("Chyba při mazání aplikace")
            }
        }
    }

    fun pinShortcut(projectId: String) {
        viewModelScope.launch {
            val manifest = repository.getProjectManifest(projectId)
            if (manifest != null) {
                val success = repository.createDesktopShortcut(manifest)
                if (success) {
                    showMessage("Zástupce přidán na plochu")
                } else {
                    showMessage("Přidání zástupce není na tomto zařízení podporováno")
                }
            }
        }
    }

    fun exportProject(projectId: String, outputStream: OutputStream) {
        viewModelScope.launch {
            val success = repository.exportProjectToStream(projectId, outputStream)
            if (success) {
                showMessage("Balíček .htapk byl úspěšně exportován")
            } else {
                showMessage("Chyba při exportu aplikace")
            }
        }
    }

    fun importProject(inputStream: InputStream) {
        viewModelScope.launch {
            val result = repository.importProjectFromStream(inputStream)
            result.onSuccess { manifest ->
                showMessage("Aplikace '${manifest.meta.name}' úspěšně importována")
            }.onFailure { err ->
                showMessage("Chyba importu: ${err.message}")
            }
        }
    }

    fun createSampleApp() {
        viewModelScope.launch {
            val sampleId = UUID.randomUUID().toString()
            val sampleManifest = ProjectManifest(
                meta = MetaConfig(
                    id = sampleId,
                    name = "Vzorová Kalkulačka",
                    description = "Ukázková offline kalkulačka vytvořená v HTML, CSS a JavaScriptu",
                    author = "HTMLPack",
                    version = "1.0.0",
                    tags = listOf("Nástroj", "Demo", "Kalkulačka")
                ),
                icon = IconConfig(
                    background = "#0F766E",
                    initialLetter = "K",
                    emoji = "🧮"
                ),
                window = WindowConfig(fullscreen = "edge_to_edge", orientation = "portrait")
            )

            val sampleHtml = """
                <!DOCTYPE html>
                <html lang="cs">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                  <title>Vzorová Kalkulačka</title>
                  <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; user-select: none; }
                    body {
                      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                      background: #0f172a;
                      color: #f8fafc;
                      display: flex;
                      flex-direction: column;
                      height: 100vh;
                      padding: 24px 16px 16px;
                    }
                    .header { font-size: 14px; color: #94a3b8; text-transform: uppercase; letter-spacing: 1.5px; margin-bottom: 12px; }
                    .display {
                      flex: 1;
                      display: flex;
                      align-items: flex-end;
                      justify-content: flex-end;
                      font-size: 52px;
                      font-weight: 300;
                      word-break: break-all;
                      padding: 12px 0;
                      color: #38bdf8;
                    }
                    .keypad {
                      display: grid;
                      grid-template-columns: repeat(4, 1fr);
                      gap: 12px;
                      margin-bottom: 16px;
                    }
                    button {
                      background: #1e293b;
                      color: #f1f5f9;
                      border: none;
                      border-radius: 18px;
                      font-size: 24px;
                      height: 64px;
                      cursor: pointer;
                      transition: transform 0.1s, background 0.1s;
                    }
                    button:active { transform: scale(0.94); background: #334155; }
                    button.op { background: #0284c7; color: white; }
                    button.clear { background: #e11d48; color: white; }
                    button.equal { background: #10b981; color: white; grid-column: span 2; }
                  </style>
                </head>
                <body>
                  <div class="header">HTMLPack Runner Demo</div>
                  <div class="display" id="display">0</div>
                  <div class="keypad">
                    <button class="clear" onclick="clearCalc()">C</button>
                    <button class="op" onclick="append('/')">÷</button>
                    <button class="op" onclick="append('*')">×</button>
                    <button class="op" onclick="backspace()">⌫</button>

                    <button onclick="append('7')">7</button>
                    <button onclick="append('8')">8</button>
                    <button onclick="append('9')">9</button>
                    <button class="op" onclick="append('-')">−</button>

                    <button onclick="append('4')">4</button>
                    <button onclick="append('5')">5</button>
                    <button onclick="append('6')">6</button>
                    <button class="op" onclick="append('+')">+</button>

                    <button onclick="append('1')">1</button>
                    <button onclick="append('2')">2</button>
                    <button onclick="append('3')">3</button>
                    <button onclick="append('.')">.</button>

                    <button onclick="append('0')">0</button>
                    <button class="equal" onclick="calculate()">=</button>
                  </div>
                  <script>
                    let current = '0';
                    function update() { document.getElementById('display').innerText = current; }
                    function append(val) {
                      if (current === '0' && val !== '.') current = val;
                      else current += val;
                      update();
                    }
                    function clearCalc() { current = '0'; update(); }
                    function backspace() {
                      current = current.slice(0, -1);
                      if (!current) current = '0';
                      update();
                    }
                    function calculate() {
                      try {
                        current = String(Function('"use strict";return (' + current + ')')());
                      } catch(e) {
                        current = 'Chyba';
                      }
                      update();
                    }
                  </script>
                </body>
                </html>
            """.trimIndent()

            repository.saveProject(sampleManifest, sampleHtml)
            showMessage("Vzorová aplikace byla vytvořena")
        }
    }

    private fun getDefaultHtmlContent(title: String): String {
        return """
            <!DOCTYPE html>
            <html lang="cs">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
              <title>$title</title>
              <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                  background: #0f172a;
                  color: #f8fafc;
                  display: flex;
                  flex-direction: column;
                  align-items: center;
                  justify-content: center;
                  min-height: 100vh;
                  padding: 24px;
                  text-align: center;
                }
                .card {
                  background: #1e293b;
                  border: 1px solid #334155;
                  border-radius: 20px;
                  padding: 32px 24px;
                  max-width: 400px;
                  box-shadow: 0 10px 25px rgba(0,0,0,0.4);
                }
                h1 { font-size: 24px; margin-bottom: 12px; color: #38bdf8; }
                p { font-size: 15px; color: #94a3b8; line-height: 1.5; margin-bottom: 24px; }
                button {
                  background: #0284c7;
                  color: white;
                  border: none;
                  border-radius: 12px;
                  padding: 14px 24px;
                  font-size: 16px;
                  font-weight: 600;
                  cursor: pointer;
                  transition: transform 0.1s;
                }
                button:active { transform: scale(0.96); }
                .counter { margin-top: 16px; font-size: 18px; color: #34d399; font-weight: bold; }
              </style>
            </head>
            <body>
              <div class="card">
                <h1>$title</h1>
                <p>Vaše nová HTMLPack aplikace běží v izolovaném sandboxu s plným přístupem k offline úložišti a JavaScriptu.</p>
                <button onclick="count++ ; document.getElementById('count').innerText = count;">Klepněte na mě</button>
                <div class="counter">Počet klepnutí: <span id="count">0</span></div>
              </div>
              <script>
                let count = 0;
              </script>
            </body>
            </html>
        """.trimIndent()
    }
}
