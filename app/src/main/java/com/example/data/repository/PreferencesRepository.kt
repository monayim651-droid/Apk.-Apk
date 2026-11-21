package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.montanuDataStore by preferencesDataStore(name = "montanu_prefs")

data class LocalAppPrefs(
    val darkMode: Boolean = true,
    val highContrast: Boolean = false,
    val languageCode: String = "en"
)

class PreferencesRepository(private val context: Context) {
    private val keyDarkMode = booleanPreferencesKey("dark_mode")
    private val keyHighContrast = booleanPreferencesKey("high_contrast")
    private val keyLanguage = stringPreferencesKey("language_code")

    val prefsFlow: Flow<LocalAppPrefs> = context.montanuDataStore.data.map { prefs ->
        LocalAppPrefs(
            darkMode = prefs[keyDarkMode] ?: true,
            highContrast = prefs[keyHighContrast] ?: false,
            languageCode = prefs[keyLanguage] ?: "en"
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.montanuDataStore.edit { it[keyDarkMode] = enabled }
    }

    suspend fun setHighContrast(enabled: Boolean) {
        context.montanuDataStore.edit { it[keyHighContrast] = enabled }
    }

    suspend fun setLanguage(code: String) {
        context.montanuDataStore.edit { it[keyLanguage] = code }
    }
}
