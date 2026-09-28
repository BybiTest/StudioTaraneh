package com.studiotaraneh.app.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("studio_settings")

data class AppSettings(val theme: String = "Neon Studio", val darkMode: String = "system", val fontScale: Float = 1f, val language: String = "fa")

class SettingsRepository(private val context: Context) {
    private object Keys { val theme = stringPreferencesKey("theme"); val dark = stringPreferencesKey("dark_mode"); val font = floatPreferencesKey("font_scale"); val lang = stringPreferencesKey("language") }
    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p -> AppSettings(p[Keys.theme] ?: "Neon Studio", p[Keys.dark] ?: "system", p[Keys.font] ?: 1f, p[Keys.lang] ?: "fa") }
    suspend fun setTheme(v: String) = context.settingsDataStore.edit { it[Keys.theme] = v }
    suspend fun setDarkMode(v: String) = context.settingsDataStore.edit { it[Keys.dark] = v }
    suspend fun setFontScale(v: Float) = context.settingsDataStore.edit { it[Keys.font] = v }
    suspend fun setLanguage(v: String) = context.settingsDataStore.edit { it[Keys.lang] = v }
}
