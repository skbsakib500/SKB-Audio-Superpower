package com.skbsakib.audiosuperpower.cloud

import android.content.Context
import android.util.Log

object DropboxProvider : CloudProvider {
    private const val TAG = "SKB-Dropbox"
    override val id = "dropbox"
    override val displayName = "Dropbox"
    override val description = "Stream music from Dropbox"

    override fun isConfigured(ctx: Context): Boolean = false

    override suspend fun isAuthenticated(ctx: Context): Boolean =
        CloudAuthStore.hasAnyToken(ctx.applicationContext, id)

    override suspend fun beginAuth(ctx: Context): AuthResult {
        Log.i(TAG, "beginAuth: provider not yet implemented")
        return AuthResult(
            ok = false,
            error = "Dropbox app key not configured. " +
                    "Create one in Dropbox App Console and paste it here.",
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
