package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.MainActivity
import com.example.MainTab
import com.example.data.repository.FirestoreSyncRepository
import com.example.ui.components.ParticleDensity
import com.example.ui.i18n.AppLanguage
import com.example.ui.theme.ThemePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("hsstore_prefs", Context.MODE_PRIVATE)
    private val firestoreRepo = FirestoreSyncRepository(application.applicationContext)

    private val _currentTab = MutableStateFlow(MainTab.TODAY)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _currentLanguage = MutableStateFlow(
        if (prefs.getString("selected_lang", AppLanguage.ENGLISH.code) == AppLanguage.KURDISH_SORANI.code) {
            AppLanguage.KURDISH_SORANI
        } else {
            AppLanguage.ENGLISH
        }
    )
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("theme_dark", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _themePreset = MutableStateFlow(ThemePreset.APPLE_BLUE)
    val themePreset: StateFlow<ThemePreset> = _themePreset.asStateFlow()

    private val _particlesEnabled = MutableStateFlow(prefs.getBoolean("particles_enabled", false))
    val particlesEnabled: StateFlow<Boolean> = _particlesEnabled.asStateFlow()

    private val _particleDensity = MutableStateFlow(ParticleDensity.MEDIUM)
    val particleDensity: StateFlow<ParticleDensity> = _particleDensity.asStateFlow()

    fun setTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun setLanguage(lang: AppLanguage) {
        _currentLanguage.value = lang
        prefs.edit().putString("selected_lang", lang.code).apply()
    }

    fun toggleDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        prefs.edit().putBoolean("theme_dark", isDark).apply()
    }

    fun setThemePreset(preset: ThemePreset) {
        _themePreset.value = preset
    }

    fun toggleParticles(enabled: Boolean) {
        _particlesEnabled.value = enabled
        prefs.edit().putBoolean("particles_enabled", enabled).apply()
    }

    fun setParticleDensity(density: ParticleDensity) {
        _particleDensity.value = density
    }

    suspend fun submitFeedback(type: String, subject: String, details: String, deviceInfo: String): Result<String> {
        return try {
            val docId = firestoreRepo.submitFeedback(type, subject, details, deviceInfo)
            Result.success(docId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
