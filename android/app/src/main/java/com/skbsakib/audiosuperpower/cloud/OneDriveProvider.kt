package com.skbsakib.audiosuperpower.cloud

import android.content.Context
import android.util.Log

object OneDriveProvider : CloudProvider {
    private const val TAG = "SKB-OneDrive"
    override val id = "onedrive"
    override val displayName = "OneDrive"
    override val description = "Stream music from Microsoft OneDrive"

    override fun isConfigured(ctx: Context): Boolean = false

    override suspend fun isAuthenticated(ctx: Context): Boolean =
        CloudAuthStore.hasAnyToken(ctx.applicationContext, id)

    override suspend fun beginAuth(ctx: Context): AuthResult {
        Log.i(TAG, "beginAuth: provider not yet implemented")
        return AuthResult(
            ok = false,
            error = "OneDrive client ID not configured. " +
                    "Register an app in Azure Portal and paste it here.",
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
