package com.example.data.model

import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class ProjectManifest(
    val meta: MetaConfig = MetaConfig(),
    val icon: IconConfig = IconConfig(),
    val source: SourceConfig = SourceConfig(),
    val window: WindowConfig = WindowConfig(),
    val webview: WebviewConfig = WebviewConfig(),
    val behavior: BehaviorConfig = BehaviorConfig(),
    val appearance: AppearanceConfig = AppearanceConfig(),
    val security: SecurityConfig = SecurityConfig(),
    val permissions: PermissionsConfig = PermissionsConfig(),
    val injections: InjectionsConfig = InjectionsConfig(),
    val pwa: PwaConfig = PwaConfig(),
    val urlPolicy: UrlPolicyConfig = UrlPolicyConfig(),
    val shortcut: ShortcutConfig = ShortcutConfig()
)

@JsonClass(generateAdapter = true)
data class MetaConfig(
    val schemaVersion: Int = 1,
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Moje Aplikace",
    val description: String = "",
    val author: String = "HTMLPack User",
    val version: String = "1.0.0",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class IconConfig(
    val foreground: String = "icon/foreground.png",
    val background: String = "#1E293B",
    val legacy: String? = null,
    val adaptiveShape: String = "rounded", // auto, circle, squircle, rounded
    val initialLetter: String = "H",
    val emoji: String = "⚡"
)

@JsonClass(generateAdapter = true)
data class SourceConfig(
    val entry: String = "index.html",
    val files: List<String> = listOf("index.html"),
    val type: String = "single", // single | multi
    val baseUrl: String = "https://appassets.androidplatform.net/"
)

@JsonClass(generateAdapter = true)
data class WindowConfig(
    val fullscreen: String = "off", // off | hide_status | immersive | edge_to_edge
    val orientation: String = "auto", // auto | portrait | landscape | locked_portrait | locked_landscape
    val statusBarColor: String = "#00000000",
    val navigationBarColor: String = "#00000000",
    val statusBarIconStyle: String = "auto", // auto | light | dark
    val keepScreenOn: Boolean = false,
    val cutoutMode: String = "default" // default | shortEdges | never
)

@JsonClass(generateAdapter = true)
data class WebviewConfig(
    val javaScript: Boolean = true,
    val domStorage: Boolean = true,
    val cookies: Boolean = true,
    val zoom: Boolean = false,
    val textZoom: Int = 100,
    val userAgent: String = "default", // default | desktop | custom
    val userAgentCustom: String = "",
    val hardwareAcceleration: Boolean = true,
    val allowFileAccess: Boolean = false,
    val allowContentAccess: Boolean = false,
    val mixedContent: String = "never", // never | compat | always
    val safeBrowsing: Boolean = true,
    val mediaPlaybackRequiresGesture: Boolean = false
)

@JsonClass(generateAdapter = true)
data class BehaviorConfig(
    val splash: SplashConfig = SplashConfig(),
    val backButton: String = "history", // history | close | confirm
    val pullToRefresh: Boolean = false,
    val externalLinks: String = "browser", // in_app | browser | ask
    val targetBlank: String = "same_window", // same_window | new_activity
    val downloads: String = "allow", // allow | block
    val rememberScroll: Boolean = false,
    val preventSleep: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SplashConfig(
    val enabled: Boolean = true,
    val bgColor: String = "#0F172A",
    val logo: String = "icon/foreground.png",
    val durationMs: Long = 1200L,
    val fadeOut: Boolean = true
)

@JsonClass(generateAdapter = true)
data class AppearanceConfig(
    val themeColorLight: String = "#1E3A8A",
    val themeColorDark: String = "#0F172A",
    val preferredColorScheme: String = "auto", // auto | light | dark
    val forceDark: String = "auto", // off | auto | on
    val fontScale: Float = 1.0f
)

@JsonClass(generateAdapter = true)
data class SecurityConfig(
    val pinLock: PinLockConfig = PinLockConfig(),
    val biometric: Boolean = false,
    val flagSecure: Boolean = false,
    val hideFromRecents: Boolean = false,
    val allowScreenshots: Boolean = true
)

@JsonClass(generateAdapter = true)
data class PinLockConfig(
    val enabled: Boolean = false,
    val pinHash: String = "" // SHA-256 hash of PIN
)

@JsonClass(generateAdapter = true)
data class PermissionsConfig(
    val camera: String = "ask", // grant | deny | ask
    val microphone: String = "ask",
    val geolocation: String = "ask",
    val notifications: String = "ask",
    val storage: String = "ask",
    val webPermissionAutoGrant: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class InjectionsConfig(
    val customCss: String = "",
    val customJs: String = "",
    val injectBeforeLoad: Boolean = true,
    val polyfills: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PwaConfig(
    val generateWebManifest: Boolean = false,
    val generateServiceWorker: Boolean = false,
    val swCacheStrategy: String = "stale_while_revalidate",
    val includeInExport: Boolean = false
)

@JsonClass(generateAdapter = true)
data class UrlPolicyConfig(
    val whitelist: List<String> = emptyList(),
    val blacklist: List<String> = emptyList(),
    val offlineOnly: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ShortcutConfig(
    val label: String = "",
    val iconOverride: String = "",
    val enabled: Boolean = true
)

data class ProjectFileItem(
    val name: String,
    val relativePath: String,
    val size: Long,
    val isDirectory: Boolean,
    val lastModified: Long,
    val mimeType: String
)
