package com.skbsakib.audiosuperpower.cloud

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.cloudStore: DataStore<Preferences> by preferencesDataStore("skb_cloud")

/**
 * On-device storage for OAuth config + tokens.
 *
 * ⚠️ Security: these credentials live in DataStore, sandboxed to this app.
 * We do NOT ship any client ids, secret values, or tokens in code.
 * Users must register their own OAuth app and paste the client id here.
 *
 * Refresh tokens and access tokens are stored separately per provider.
 * Access tokens get an expiry timestamp; refresh tokens are long-lived.
 */
object CloudAuthStore {

    private fun keyClientId(provider: String) = stringPreferencesKey("${provider}_client_id")
    private fun keyRedirect(provider: String) = stringPreferencesKey("${provider}_redirect_uri")
    private fun keyAccess(provider: String)   = stringPreferencesKey("${provider}_access_token")
    private fun keyRefresh(provider: String)  = stringPreferencesKey("${provider}_refresh_token")
    private fun keyExpiry(provider: String)   = stringPreferencesKey("${provider}_expiry_epoch_ms")

    // ── Client configuration ──
    suspend fun getClientId(ctx: Context, provider: String): String? =
        ctx.cloudStore.data.first()[keyClientId(provider)]

    suspend fun setClientId(ctx: Context, provider: String, clientId: String) {
        ctx.cloudStore.edit { it[keyClientId(provider)] = clientId }
    }

    suspend fun getRedirectUri(ctx: Context, provider: String): String? =
        ctx.cloudStore.data.first()[keyRedirect(provider)]

    suspend fun setRedirectUri(ctx: Context, provider: String, uri: String) {
        ctx.cloudStore.edit { it[keyRedirect(provider)] = uri }
    }

    // ── Tokens ──
    suspend fun storeTokens(
        ctx: Context, provider: String,
        accessToken: String, refreshToken: String?,
        expiryEpochMs: Long?
    ) {
        ctx.cloudStore.edit { prefs ->
            prefs[keyAccess(provider)] = accessToken
            if (refreshToken != null) prefs[keyRefresh(provider)] = refreshToken
            if (expiryEpochMs != null) prefs[keyExpiry(provider)] = expiryEpochMs.toString()
        }
    }

    suspend fun getAccessToken(ctx: Context, provider: String): String? =
        ctx.cloudStore.data.first()[keyAccess(provider)]

    suspend fun getRefreshToken(ctx: Context, provider: String): String? =
        ctx.cloudStore.data.first()[keyRefresh(provider)]

    suspend fun isTokenFresh(ctx: Context, provider: String): Boolean {
        val prefs = ctx.cloudStore.data.first()
        val exp = prefs[keyExpiry(provider)]?.toLongOrNull() ?: return false
        return System.currentTimeMillis() < exp - 30_000L
    }

    suspend fun clearTokens(ctx: Context, provider: String) {
        ctx.cloudStore.edit { prefs ->
            prefs.remove(keyAccess(provider))
            prefs.remove(keyRefresh(provider))
            prefs.remove(keyExpiry(provider))
        }
    }

    suspend fun hasAnyToken(ctx: Context, provider: String): Boolean =
        ctx.cloudStore.data.map { it[keyAccess(provider)] != null }.first()
}
