package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("htmlpack_preferences", Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(prefs.getString("app_language", "cs") ?: "cs")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _viewMode = MutableStateFlow(prefs.getString("view_mode", "grid") ?: "grid")
    val viewMode: StateFlow<String> = _viewMode.asStateFlow()

    fun setLanguage(lang: String) {
        prefs.edit().putString("app_language", lang).apply()
        _language.value = lang
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    fun setViewMode(mode: String) {
        prefs.edit().putString("view_mode", mode).apply()
        _viewMode.value = mode
    }
}
