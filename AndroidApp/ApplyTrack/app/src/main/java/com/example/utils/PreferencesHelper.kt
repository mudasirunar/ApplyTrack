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

    private val _isOfflineGuest = MutableStateFlow(isOfflineGuestEnabled())
    val isOfflineGuest: StateFlow<Boolean> = _isOfflineGuest.asStateFlow()

    fun getSavedTheme(): AppTheme {
        val themeName = sharedPrefs.getString("app_theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        return try {
            AppTheme.valueOf(themeName)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    }

    fun setTheme(theme: AppTheme) {
        sharedPrefs.edit()
            .putString("app_theme", theme.name)
            .apply()
        _themeFlow.value = theme
    }

    fun isAutoSyncEnabled(): Boolean {
        return true
    }

    fun isOfflineGuestEnabled(): Boolean {
        return sharedPrefs.getBoolean("is_offline_guest", false)
    }

    fun setOfflineGuest(value: Boolean) {
        sharedPrefs.edit().putBoolean("is_offline_guest", value).apply()
        _isOfflineGuest.value = value
    }

    fun clearAll() {
        sharedPrefs.edit().clear().apply()
        _themeFlow.value = AppTheme.SYSTEM
        _isOfflineGuest.value = false
    }
}
