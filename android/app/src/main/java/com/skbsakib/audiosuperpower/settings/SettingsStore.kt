package com.skbsakib.audiosuperpower.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "skb_settings")

object SettingsKeys {
    val ACCENT_COLOR = stringPreferencesKey("accent_color")
    val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    val THEME_VARIANT = stringPreferencesKey("theme_variant")
}

data class SkbSettings(
    val accentColor: String = "cyan",
    val hapticsEnabled: Boolean = true,
    val themeVariant: String = "dark"
)

class SettingsStore(private val context: Context) {

    val settings: Flow<SkbSettings> = context.dataStore.data.map { prefs ->
        SkbSettings(
            accentColor = prefs[SettingsKeys.ACCENT_COLOR] ?: "cyan",
            hapticsEnabled = prefs[SettingsKeys.HAPTICS_ENABLED] ?: true,
            themeVariant = prefs[SettingsKeys.THEME_VARIANT] ?: "dark"
        )
    }

    suspend fun setAccent(color: String) {
        context.dataStore.edit { it[SettingsKeys.ACCENT_COLOR] = color }
    }

    suspend fun setHaptics(enabled: Boolean) {
        context.dataStore.edit { it[SettingsKeys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setThemeVariant(variant: String) {
        context.dataStore.edit { it[SettingsKeys.THEME_VARIANT] = variant }
    }
}
