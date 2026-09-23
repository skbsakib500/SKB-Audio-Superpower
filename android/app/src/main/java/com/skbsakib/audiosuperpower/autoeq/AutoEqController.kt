package com.skbsakib.audiosuperpower.autoeq

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.skbsakib.audiosuperpower.NativeBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.autoEqDataStore: DataStore<Preferences> by preferencesDataStore("skb_autoeq")

data class AutoEqState(
    val enabled: Boolean = false,
    val profileId: String = "flat"
)

object AutoEqController {

    private val KEY_ENABLED = booleanPreferencesKey("enabled")
    private val KEY_PROFILE = stringPreferencesKey("profile_id")

    private val _state = MutableStateFlow(AutoEqState())
    val state: StateFlow<AutoEqState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** Load persisted state + push to native. Call once on app start. */
    suspend fun init(context: Context) {
        val prefs = context.autoEqDataStore.data.first()
        val enabled = prefs[KEY_ENABLED] ?: false
        val profileId = prefs[KEY_PROFILE] ?: "flat"
        _state.value = AutoEqState(enabled = enabled, profileId = profileId)
        if (enabled) applyProfile(profileId)
        NativeBridge.nativeSetAutoEqEnabled(enabled)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        _state.value = _state.value.copy(enabled = enabled)
        NativeBridge.nativeSetAutoEqEnabled(enabled)
        if (enabled) applyProfile(_state.value.profileId)
        scope.launch {
            context.autoEqDataStore.edit { it[KEY_ENABLED] = enabled }
        }
    }

    fun selectProfile(context: Context, profileId: String) {
        _state.value = _state.value.copy(profileId = profileId)
        if (_state.value.enabled) applyProfile(profileId)
        scope.launch {
            context.autoEqDataStore.edit { it[KEY_PROFILE] = profileId }
        }
    }

    fun toggleProfile(context: Context, profileId: String) {
        if (_state.value.profileId == profileId && _state.value.enabled) {
            setEnabled(context, false)
        } else {
            selectProfile(context, profileId)
            setEnabled(context, true)
        }
    }

    private fun applyProfile(profileId: String) {
        NativeBridge.nativeAutoEqClear()
        val p = AutoEqProfiles.byId(profileId) ?: return
        NativeBridge.nativeAutoEqSetPreamp(p.preampDb)
        p.filters.forEach { f ->
            NativeBridge.nativeAutoEqAddFilter(f.type, f.freq, f.q, f.gainDb)
        }
    }
}
