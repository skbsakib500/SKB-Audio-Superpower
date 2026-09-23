package com.skbsakib.audiosuperpower.library

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastScanTime = MutableStateFlow(0L)
    val lastScanTime: StateFlow<Long> = _lastScanTime.asStateFlow()

    fun rescan(ctx: Context, folderUris: Set<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            val list = runCatching {
                SafScanner.scan(ctx.applicationContext, folderUris)
            }.getOrElse { emptyList() }
            _tracks.value = list
            _lastScanTime.value = System.currentTimeMillis()
            _isLoading.value = false
        }
    }
}
