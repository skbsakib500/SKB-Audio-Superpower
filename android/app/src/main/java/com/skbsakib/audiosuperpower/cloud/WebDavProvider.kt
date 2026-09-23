package com.skbsakib.audiosuperpower.cloud

import android.content.Context
import android.util.Log

object WebDavProvider : CloudProvider {
    private const val TAG = "SKB-WebDAV"
    override val id = "webdav"
    override val displayName = "WebDAV"
    override val description = "Connect to any WebDAV server (Nextcloud, Synology, ownCloud)"

    override fun isConfigured(ctx: Context): Boolean = false

    override suspend fun isAuthenticated(ctx: Context): Boolean =
        CloudAuthStore.hasAnyToken(ctx.applicationContext, id)

    override suspend fun beginAuth(ctx: Context): AuthResult {
        Log.i(TAG, "beginAuth: provider not yet implemented")
        return AuthResult(
            ok = false,
            error = "WebDAV server URL not configured.",
            requiresUserAction = true
        )
    }

    override suspend fun signOut(ctx: Context) {
        CloudAuthStore.clearTokens(ctx.applicationContext, id)
    }

    override suspend fun listRootFolders(ctx: Context): List<CloudFolder> = emptyList()
    override suspend fun listChildren(ctx: Context, folderId: String): List<CloudItem> = emptyList()
    override suspend fun resolvePlayablePath(ctx: Context, item: CloudItem): String? = null
}
