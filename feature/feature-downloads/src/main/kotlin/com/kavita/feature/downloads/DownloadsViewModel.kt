package com.kavita.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kavita.core.model.DownloadTask
import com.kavita.core.model.repository.DownloadRepository
import com.kavita.core.network.ActiveServerProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SeriesDownloadGroup(
    val seriesId: Int,
    val serverId: Long,
    val seriesName: String,
    val downloads: List<DownloadTask>,
)

data class DownloadsUiState(
    val storageUsed: Long = 0,
    val error: String? = null,
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
    private val activeServerProvider: ActiveServerProvider,
) : ViewModel() {

    val downloads: StateFlow<List<DownloadTask>> = downloadRepository.observeDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupedDownloads: StateFlow<List<SeriesDownloadGroup>> = downloadRepository.observeDownloads()
        .map { tasks ->
            tasks.groupBy { it.seriesId to it.serverId }
                .map { (key, groupTasks) ->
                    SeriesDownloadGroup(
                        seriesId = key.first,
                        serverId = key.second,
                        seriesName = groupTasks.first().seriesName,
                        downloads = groupTasks,
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    init {
        loadStorageInfo()
    }

    private fun loadStorageInfo() {
        viewModelScope.launch {
            val used = downloadRepository.getStorageUsed()
            _uiState.update { it.copy(storageUsed = used) }
        }
    }

    fun getCoverUrl(seriesId: Int): String? {
        val baseUrl = try { activeServerProvider.requireUrl() } catch (_: Exception) { return null }
        return "$baseUrl/api/image/series-cover?seriesId=$seriesId"
    }

    fun cancelDownload(downloadId: Long) {
        viewModelScope.launch {
            downloadRepository.cancelDownload(downloadId)
        }
    }

    fun deleteDownload(downloadId: Long) {
        viewModelScope.launch {
            downloadRepository.deleteDownload(downloadId)
            loadStorageInfo()
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            downloadRepository.deleteAllDownloads()
            loadStorageInfo()
        }
    }
}
