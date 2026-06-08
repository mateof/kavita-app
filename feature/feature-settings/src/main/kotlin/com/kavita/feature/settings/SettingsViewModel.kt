package com.kavita.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.kavita.core.model.AppTheme
import com.kavita.core.model.AppUpdate
import com.kavita.core.model.ReadingDirection
import com.kavita.core.model.UserPreferences
import com.kavita.core.data.preferences.UserPreferencesDataStore
import com.kavita.core.model.repository.AppUpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/** Estado de la comprobación/descarga de actualizaciones. */
data class UpdateUiState(
    val checking: Boolean = false,
    val checkFailed: Boolean = false,
    val upToDate: Boolean = false,
    /** Release más nueva que la instalada, o null si no hay novedad. */
    val available: AppUpdate? = null,
    val downloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadFailed: Boolean = false,
    /** APK ya descargado, listo para lanzar el instalador. */
    val downloadedApk: File? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore,
    private val imageLoader: ImageLoader,
    private val appUpdateRepository: AppUpdateRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesDataStore.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    private val _imageCacheSizeBytes = MutableStateFlow(0L)
    val imageCacheSizeBytes: StateFlow<Long> = _imageCacheSizeBytes.asStateFlow()

    /** Versión instalada (versionName del paquete). */
    val currentVersion: String = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "?"

    private val _updateState = MutableStateFlow(UpdateUiState())
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    init {
        refreshCacheSize()
        checkForUpdate()
    }

    fun checkForUpdate() {
        if (_updateState.value.checking) return
        _updateState.update {
            it.copy(checking = true, checkFailed = false, upToDate = false, available = null)
        }
        viewModelScope.launch {
            val latest = appUpdateRepository.getLatestRelease()
            if (latest == null) {
                _updateState.update { it.copy(checking = false, checkFailed = true) }
                return@launch
            }
            val newer = isNewerVersion(latest.versionName, currentVersion)
            _updateState.update {
                it.copy(
                    checking = false,
                    available = if (newer) latest else null,
                    upToDate = !newer,
                )
            }
        }
    }

    fun downloadUpdate() {
        val update = _updateState.value.available ?: return
        if (update.apkUrl == null) return
        _updateState.update {
            it.copy(downloading = true, downloadProgress = 0f, downloadFailed = false, downloadedApk = null)
        }
        viewModelScope.launch {
            try {
                val file = appUpdateRepository.downloadApk(update) { downloaded, total ->
                    val p = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f
                    _updateState.update { it.copy(downloadProgress = p) }
                }
                _updateState.update { it.copy(downloading = false, downloadProgress = 1f, downloadedApk = file) }
            } catch (_: Exception) {
                _updateState.update { it.copy(downloading = false, downloadFailed = true) }
            }
        }
    }

    /** Limpia el APK descargado tras lanzar el instalador (evita relanzarlo en recomposición). */
    fun onApkInstallLaunched() {
        _updateState.update { it.copy(downloadedApk = null) }
    }

    /** Compara versiones tipo "1.2.3"; true si [remote] es estrictamente mayor que [local]. */
    private fun isNewerVersion(remote: String, local: String): Boolean {
        fun parts(v: String) = v.trim().split('.', '-').mapNotNull { it.toIntOrNull() }
        val r = parts(remote)
        val l = parts(local)
        val n = maxOf(r.size, l.size)
        for (i in 0 until n) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private fun refreshCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            _imageCacheSizeBytes.value = imageLoader.diskCache?.size ?: 0L
        }
    }

    fun clearImageCache() {
        viewModelScope.launch(Dispatchers.IO) {
            imageLoader.diskCache?.clear()
            imageLoader.memoryCache?.clear()
            refreshCacheSize()
        }
    }

    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch { preferencesDataStore.updateTheme(theme) }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.updateDynamicColor(enabled) }
    }

    fun updateReadingDirection(direction: ReadingDirection) {
        viewModelScope.launch { preferencesDataStore.updateReadingDirection(direction) }
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.updateKeepScreenOn(enabled) }
    }

    fun updateWifiOnlyDownloads(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.updateWifiOnlyDownloads(enabled) }
    }

    fun updateDailyGoal(minutes: Int) {
        viewModelScope.launch { preferencesDataStore.updateDailyReadingGoal(minutes) }
    }
}
