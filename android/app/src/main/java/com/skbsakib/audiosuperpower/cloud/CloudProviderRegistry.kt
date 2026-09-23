package com.skbsakib.audiosuperpower.cloud

import android.content.Context

/**
 * Registry of all known cloud providers.
 *
 * GoogleDriveProvider and DropboxProvider are scaffolds that require the user
 * to paste their own OAuth client id before they can authenticate. They will
 * report isConfigured()=false until then — no fake connections.
 */
object CloudProviderRegistry {

    private val providers: List<CloudProvider> = listOf(
        GoogleDriveProvider,
        DropboxProvider,
        OneDriveProvider,
        WebDavProvider
    )

    fun all(): List<CloudProvider> = providers

    fun byId(id: String): CloudProvider? = providers.firstOrNull { it.id == id }

    suspend fun configured(ctx: Context): List<CloudProvider> =
        providers.filter { it.isConfigured(ctx.applicationContext) }

    suspend fun authenticated(ctx: Context): List<CloudProvider> =
        providers.filter { it.isAuthenticated(ctx.applicationContext) }
}
