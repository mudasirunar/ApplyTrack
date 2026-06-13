package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

class PreferencesHelper(context: Context) {

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(
        "applytrack_preferences",
        Context.MODE_PRIVATE
    )

    private val _themeFlow = MutableStateFlow(getSavedTheme())
    val themeFlow: StateFlow<AppTheme> = _themeFlow.asStateFlow()

    private val _autoSyncFlow = MutableStateFlow(isAutoSyncEnabled())
    val autoSyncFlow: StateFlow<Boolean> = _autoSyncFlow.asStateFlow()

    fun getSavedTheme(): AppTheme {
        val themeName = sharedPrefs.getString("app_theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        return try {
            AppTheme.valueOf(themeName)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    }

    fun setTheme(theme: AppTheme) {
        sharedPrefs.edit().putString("app_theme", theme.name).apply()
        _themeFlow.value = theme
    }

    fun isAutoSyncEnabled(): Boolean {
        return sharedPrefs.getBoolean("auto_sync", true)
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("auto_sync", enabled).apply()
        _autoSyncFlow.value = enabled
    }
}
