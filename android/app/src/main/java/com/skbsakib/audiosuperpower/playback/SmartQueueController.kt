package com.skbsakib.audiosuperpower.playback

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.skbsakib.audiosuperpower.analyzer.TrackAnalysisCache
import com.skbsakib.audiosuperpower.library.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.smartQDataStore: DataStore<Preferences> by preferencesDataStore("skb_smartq")

/** Sequencing strategy for Smart Queue. */
enum class SmartQueueMode {
    OFF,        // use library order (existing behavior)
    TEMPO,      // same-BPM neighbors (±15%)
    ENERGY,     // energy continuity (build/drop)
    MIXED       // 60% tempo + 40% energy weight
}

data class SmartQueueState(
    val mode: SmartQueueMode = SmartQueueMode.OFF,
    val lastPickReason: String = ""
)

/**
 * Rule-based next-track selection.
 *
 * For each candidate track:
 *   • tempo score  = 1 - |bpm_a - bpm_b| / max(bpm_a, bpm_b)
 *   • energy score = 1 - |energy_a - energy_b|
 *   • artist penalty = 0.4 if same artist as the last track
 *   • recently-played penalty = 0.6 if in the last 10 picks
 *
 * Weighted by mode:
 *   TEMPO  → 100% tempo
 *   ENERGY → 100% energy
 *   MIXED  → 60% tempo + 40% energy
 *   OFF    → return null (caller falls back to library order)
 */
object SmartQueueController {

    private val KEY_MODE = stringPreferencesKey("mode")

    private val _state = MutableStateFlow(SmartQueueState())
    val state: StateFlow<SmartQueueState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val recentPicks = ArrayDeque<String>()   // track paths, max 10

    suspend fun init(context: Context) {
        val raw = context.smartQDataStore.data.first()[KEY_MODE] ?: "off"
        val mode = when (raw) {
            "tempo" -> SmartQueueMode.TEMPO
            "energy" -> SmartQueueMode.ENERGY
            "mixed" -> SmartQueueMode.MIXED
            else -> SmartQueueMode.OFF
        }
        _state.value = SmartQueueState(mode = mode)
    }

    fun setMode(context: Context, mode: SmartQueueMode) {
        _state.value = _state.value.copy(mode = mode)
        scope.launch {
            context.smartQDataStore.edit {
                it[KEY_MODE] = when (mode) {
                    SmartQueueMode.TEMPO -> "tempo"
                    SmartQueueMode.ENERGY -> "energy"
                    SmartQueueMode.MIXED -> "mixed"
                    else -> "off"
                }
            }
        }
    }

    fun rememberPick(path: String) {
        recentPicks.remove(path)
        recentPicks.addLast(path)
        while (recentPicks.size > 10) recentPicks.removeFirst()
    }

    /**
     * Pick the next track from `candidates` given the currently playing `current`.
     * Returns null if mode is OFF or no good pick found.
     */
    suspend fun pickNext(
        context: Context,
        current: Track,
        candidates: List<Track>
    ): Track? {
        val mode = _state.value.mode
        if (mode == SmartQueueMode.OFF) return null

        val cache = TrackAnalysisCache.loadAll(context.applicationContext)
        val cur = cache[current.path] ?: return null
        if (cur.bpm <= 0f && cur.energy <= 0f) return null

        var best: Track? = null
        var bestScore = -1f
        var bestReason = ""

        for (c in candidates) {
            if (c.path == current.path) continue
            val a = cache[c.path] ?: continue
            if (a.bpm <= 0f && a.energy <= 0f) continue

            val tempoScore = if (cur.bpm > 0f && a.bpm > 0f) {
                val diff = kotlin.math.abs(cur.bpm - a.bpm)
                val rel = diff / kotlin.math.max(cur.bpm, a.bpm)
                (1f - rel).coerceIn(0f, 1f)
            } else 0f

            val energyScore = (1f - kotlin.math.abs(cur.energy - a.energy))
                .coerceIn(0f, 1f)

            val base = when (mode) {
                SmartQueueMode.TEMPO -> tempoScore
                SmartQueueMode.ENERGY -> energyScore
                SmartQueueMode.MIXED -> 0.6f * tempoScore + 0.4f * energyScore
                SmartQueueMode.OFF -> 0f
            }

            var score = base
            if (c.artist == current.artist && c.artist != "Unknown artist") score *= 0.6f
            if (c.path in recentPicks) score *= 0.4f

            if (score > bestScore) {
                bestScore = score
                best = c
                bestReason = "tempo=%.2f energy=%.2f bpm=%.0f".format(
                    tempoScore, energyScore, a.bpm)
            }
        }

        if (best != null && bestScore > 0.3f) {
            _state.value = _state.value.copy(lastPickReason = bestReason)
            return best
        }
        return null
    }
}
