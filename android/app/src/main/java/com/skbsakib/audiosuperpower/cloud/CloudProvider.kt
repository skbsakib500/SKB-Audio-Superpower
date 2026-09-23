package com.skbsakib.audiosuperpower.cloud

import android.content.Context

/**
 * Abstract contract for a cloud music provider.
 *
 * Implementations (Google Drive, Dropbox, OneDrive, WebDAV, ...) provide:
 *   - OAuth configuration (client id / scopes)
 *   - Token lifecycle
 *   - Folder/file listing
 *   - Streaming URL or local download
 *
 * The framework NEVER stores credentials in code. Users register their own
 * OAuth client IDs via the CloudSourceScreen UI; those are stored in
 * DataStore on-device and never leave the device.
 */
interface CloudProvider {

    /** Stable id, e.g. "gdrive", "dropbox", "onedrive", "webdav". */
    val id: String

    /** Display name shown in UI. */
    val displayName: String

    /** Optional short description. */
    val description: String

    /** True if the provider has enough config (client id, redirect uri) to attempt auth. */
    fun isConfigured(ctx: Context): Boolean

    /** True if a valid/refreshable token is currently stored. */
    suspend fun isAuthenticated(ctx: Context): Boolean

    /**
     * Kick off the OAuth flow. Implementations may use AppAuth, Google Sign-In,
     * or a custom WebView. Must be called from an Activity context.
     */
    suspend fun beginAuth(ctx: Context): AuthResult

    /** Revoke + clear stored tokens. */
    suspend fun signOut(ctx: Context)

    /**
     * List the user's root folders. Used as the entry point for browsing.
     * Only valid after isAuthenticated() returns true.
     */
    suspend fun listRootFolders(ctx: Context): List<CloudFolder>

    /** List children (folders + audio files) of a folder. */
    suspend fun listChildren(ctx: Context, folderId: String): List<CloudItem>

    /**
     * Return a local file path OR a streamable URL for the item.
     * For files we cannot stream directly, implementations should cache a
     * temporary local copy under app cache and return that path.
     */
    suspend fun resolvePlayablePath(ctx: Context, item: CloudItem): String?
}

data class AuthResult(
    val ok: Boolean,
    val error: String? = null,
    val requiresUserAction: Boolean = false   // e.g. open browser, paste redirect
)

data class CloudFolder(
    val id: String,
    val name: String,
    val providerId: String
)

sealed class CloudItem {
    val id: String
    val name: String
    val providerId: String
    constructor(id: String, name: String, providerId: String) {
        this.id = id; this.name = name; this.providerId = providerId
    }

    data class Folder(
        val fid: String, val fname: String, val prov: String
    ) : CloudItem(fid, fname, prov)

    data class AudioFile(
        val fid: String, val fname: String, val prov: String,
        val sizeBytes: Long, val mimeType: String?
    ) : CloudItem(fid, fname, prov)
}
