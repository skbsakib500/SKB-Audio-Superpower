package com.skbsakib.audiosuperpower.cloud

import android.content.Context
import android.util.Log

/**
 * Google Drive provider scaffold.
 *
 * To activate this provider, the user (developer/owner) must:
 *   1. Create an OAuth 2.0 client ID in Google Cloud Console
 *      (type: Android, package: com.skbsakib.audiosuperpower)
 *   2. Add the SHA-1 of their debug/release signing key
 *   3. Paste the client ID in Cloud Source screen
 *
 * Once configured, this scaffold will use Android AccountManager + Google
 * Play Services Auth (a future dependency) to obtain tokens. Until then it
 * returns isConfigured()=false and beginAuth() = requiresUserAction.
 *
 * The scaffold does NOT ship any client id and will NOT fake authentication.
 */
object GoogleDriveProvider : CloudProvider {

    private const val TAG = "SKB-GDrive"

    override val id = "gdrive"
    override val displayName = "Google Drive"
    override val description = "Stream music from your Drive library"

    override fun isConfigured(ctx: Context): Boolean = false  // no client id

    override suspend fun isAuthenticated(ctx: Context): Boolean =
        CloudAuthStore.hasAnyToken(ctx.applicationContext, id)

    override suspend fun beginAuth(ctx: Context): AuthResult {
        Log.i(TAG, "beginAuth: provider not yet implemented")
        return AuthResult(
            ok = false,
            error = "Google Drive client ID not configured. " +
                    "Create one in Google Cloud Console and paste it here.",
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
