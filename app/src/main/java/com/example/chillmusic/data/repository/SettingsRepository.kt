package com.example.chillmusic.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.chillmusic.data.model.AppSettings
import com.example.chillmusic.data.model.MotionSettings
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("chillrun_settings", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val KEY_SETTINGS = "app_settings"

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val json = prefs.getString(KEY_SETTINGS, null)
        return if (json != null) {
            try {
                gson.fromJson(json, AppSettings::class.java)
            } catch (e: Exception) {
                AppSettings()
            }
        } else {
            AppSettings()
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        prefs.edit().putString(KEY_SETTINGS, gson.toJson(newSettings)).apply()
    }
    
    fun updateMotionSettings(motionSettings: MotionSettings) {
        val current = _settings.value
        updateSettings(current.copy(motion = motionSettings))
    }

    fun updateLanguage(language: String) {
        val current = _settings.value
        updateSettings(current.copy(language = language))
    }
}
