package com.example.ui.runner

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.model.ProjectManifest
import com.example.data.repository.ProjectRepository
import com.example.util.StringsHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HtmlRunnerScreen(
    projectId: String,
    repository: ProjectRepository,
    language: String,
    onNavigateHome: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var manifest by remember { mutableStateOf<ProjectManifest?>(null) }
    var isLoadingManifest by remember { mutableStateOf(true) }
    var isUnlocked by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    var showSplash by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showConsoleLogs by remember { mutableStateOf(false) }
    val consoleLogs = remember { mutableStateListOf<String>() }

    // Load project manifest
    LaunchedEffect(projectId) {
        val loaded = repository.getProjectManifest(projectId)
        manifest = loaded
        isLoadingManifest = false

        if (loaded != null) {
            // Check if PIN lock is enabled
            if (!loaded.security.pinLock.enabled || loaded.security.pinLock.pinHash.isBlank()) {
                isUnlocked = true
            }

            // Splash timing
            if (loaded.behavior.splash.enabled) {
                delay(loaded.behavior.splash.durationMs.coerceAtLeast(600L))
                showSplash = false
            } else {
                showSplash = false
            }
        }
    }

    // Configure window insets and orientation based on manifest
    DisposableEffect(manifest) {
        val activity = context as? Activity
        val currentOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val window = activity?.window

        if (manifest != null && window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)

            when (manifest?.window?.fullscreen) {
                "hide_status" -> {
                    controller.hide(WindowInsetsCompat.Type.statusBars())
                    controller.show(WindowInsetsCompat.Type.navigationBars())
                }
                "immersive" -> {
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
                "edge_to_edge" -> {
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                }
                else -> {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            when (manifest?.window?.orientation) {
                "portrait", "locked_portrait" -> activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                "landscape", "locked_landscape" -> activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                else -> activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }

        onDispose {
            activity?.requestedOrientation = currentOrientation
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
        }
    }

    // Back handling
    BackHandler {
        val wv = webViewRef
        if (manifest?.behavior?.backButton == "history" && wv?.canGoBack() == true) {
            wv.goBack()
        } else {
            onNavigateHome()
        }
    }

    if (isLoadingManifest) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val currentManifest = manifest
    if (currentManifest == null) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Aplikace nebyla nalezena",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onNavigateHome) {
                    Text(StringsHelper.get("back", language))
                }
            }
        }
        return
    }

    // PIN Lock Gate
    if (!isUnlocked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Zámek",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = currentManifest.meta.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = StringsHelper.get("pin_prompt", language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            enteredPin = it
                            pinError = false
                        },
                        label = { Text("PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        isError = pinError,
                        supportingText = if (pinError) {
                            { Text(StringsHelper.get("pin_incorrect", language)) }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onNavigateHome) {
                            Text(StringsHelper.get("cancel", language))
                        }
                        Button(
                            onClick = {
                                val enteredHash = hashPin(enteredPin)
                                if (enteredHash == currentManifest.security.pinLock.pinHash) {
                                    isUnlocked = true
                                } else {
                                    pinError = true
                                }
                            }
                        ) {
                            Text(StringsHelper.get("unlock", language))
                        }
                    }
                }
            }
        }
        return
    }

    // Main Runner Layout
    Box(modifier = Modifier.fillMaxSize()) {
        // Sandboxed WebView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    settings.apply {
                        javaScriptEnabled = currentManifest.webview.javaScript
                        domStorageEnabled = currentManifest.webview.domStorage
                        databaseEnabled = true
                        setSupportZoom(currentManifest.webview.zoom)
                        builtInZoomControls = currentManifest.webview.zoom
                        displayZoomControls = false
                        textZoom = currentManifest.webview.textZoom
                        allowFileAccess = currentManifest.webview.allowFileAccess
                        allowContentAccess = currentManifest.webview.allowContentAccess
                        mediaPlaybackRequiresUserGesture = currentManifest.webview.mediaPlaybackRequiresGesture
                        mixedContentMode = when (currentManifest.webview.mixedContent) {
                            "always" -> WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            "compat" -> WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            else -> WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        }
                    }

                    // Intercept appassets.androidplatform.net requests to serve from local app storage safely
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val url = request?.url ?: return null
                            if (url.scheme == "https" && url.host == "appassets.androidplatform.net") {
                                val path = url.path ?: ""
                                val relativePath = path.removePrefix("/")
                                val resolvedPath = if (relativePath.isBlank()) currentManifest.source.entry else relativePath

                                val projectSrcDir = repository.fileStorage.getProjectSrcDir(projectId)
                                val targetFile = File(projectSrcDir, resolvedPath)

                                if (targetFile.exists() && targetFile.isFile) {
                                    val mimeType = when {
                                        targetFile.name.endsWith(".html", true) -> "text/html"
                                        targetFile.name.endsWith(".css", true) -> "text/css"
                                        targetFile.name.endsWith(".js", true) -> "application/javascript"
                                        targetFile.name.endsWith(".json", true) -> "application/json"
                                        targetFile.name.endsWith(".png", true) -> "image/png"
                                        targetFile.name.endsWith(".jpg", true) || targetFile.name.endsWith(".jpeg", true) -> "image/jpeg"
                                        targetFile.name.endsWith(".svg", true) -> "image/svg+xml"
                                        else -> "application/octet-stream"
                                    }
                                    return WebResourceResponse(
                                        mimeType,
                                        "UTF-8",
                                        FileInputStream(targetFile)
                                    )
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Injections if specified
                            if (currentManifest.injections.customCss.isNotBlank()) {
                                val css = currentManifest.injections.customCss.replace("'", "\\'")
                                view?.evaluateJavascript(
                                    "(function() { var s = document.createElement('style'); s.innerHTML = '$css'; document.head.appendChild(s); })();",
                                    null
                                )
                            }
                            if (currentManifest.injections.customJs.isNotBlank()) {
                                view?.evaluateJavascript(currentManifest.injections.customJs, null)
                            }
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            consoleMessage?.let {
                                val log = "[${it.messageLevel()}] ${it.message()} (line ${it.lineNumber()})"
                                consoleLogs.add(log)
                            }
                            return super.onConsoleMessage(consoleMessage)
                        }
                    }

                    val baseUrl = "https://appassets.androidplatform.net/${currentManifest.source.entry}"
                    loadUrl(baseUrl)
                    webViewRef = this
                }
            }
        )

        // Floating Service Edge Button (Long press or tap to open mini menu)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            IconButton(
                onClick = { showMenu = !showMenu },
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Runner Menu",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Mini Runner Action Bar
        AnimatedVisibility(
            visible = showMenu,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                tonalElevation = 8.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            webViewRef?.reload()
                            showMenu = false
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                    }
                    IconButton(
                        onClick = {
                            showConsoleLogs = true
                            showMenu = false
                        }
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = "Logs")
                    }
                    IconButton(
                        onClick = {
                            showMenu = false
                            onNavigateHome()
                        }
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Home")
                    }
                    IconButton(
                        onClick = { showMenu = false }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Menu")
                    }
                }
            }
        }

        // Console Logs Dialog
        if (showConsoleLogs) {
            AlertDialog(
                onDismissRequest = { showConsoleLogs = false },
                title = { Text("JavaScript Console Logs") },
                text = {
                    if (consoleLogs.isEmpty()) {
                        Text("Žádné záznamy v konzoli")
                    } else {
                        LazyColumn(modifier = Modifier.height(260.dp)) {
                            items(consoleLogs) { log ->
                                Text(
                                    text = log,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showConsoleLogs = false }) {
                        Text("Zavřít")
                    }
                }
            )
        }

        // Splash Screen Overlay
        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val splashBg = try {
                Color.parseColor(currentManifest.behavior.splash.bgColor)
            } catch (e: Exception) {
                Color.parseColor("#0F172A")
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color(splashBg)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(androidx.compose.ui.graphics.Color(0xFF1E3A8A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentManifest.icon.emoji.ifBlank { currentManifest.meta.name.take(1) },
                            fontSize = 44.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = currentManifest.meta.name,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "HTMLPack Runtime",
                        fontSize = 14.sp,
                        color = androidx.compose.ui.graphics.Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

private fun hashPin(pin: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(pin.toByteArray())
    return digest.fold("") { str, it -> str + "%02x".format(it) }
}
