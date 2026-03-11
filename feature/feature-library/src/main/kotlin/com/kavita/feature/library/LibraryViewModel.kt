package com.kavita.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kavita.core.model.Library
import com.kavita.core.model.Series
import com.kavita.core.model.repository.DownloadRepository
import com.kavita.core.model.repository.LibraryRepository
import com.kavita.core.model.repository.SeriesRepository
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

data class LibraryUiState(
    val libraries: List<Library> = emptyList(),
    val selectedLibraryId: Int? = null,
    val series: List<Series> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val error: String? = null,
    val showDownloadedOnly: Boolean = false,
    val scrollToIndex: Int? = null,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val seriesRepository: SeriesRepository,
    private val activeServerProvider: ActiveServerProvider,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    val downloadedSeriesIds: StateFlow<Set<Int>> = downloadRepository.observeDownloadedSeriesIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val downloadedSeries: StateFlow<List<Series>> = downloadRepository.observeDownloadedSeries()
        .map { infoList ->
            val baseUrl = try { activeServerProvider.requireUrl() } catch (_: Exception) { "" }
            val apiKey = activeServerProvider.getApiKey()
            infoList.map { info ->
                val coverParam = "seriesId=${info.seriesId}"
                val coverUrl = if (baseUrl.isNotEmpty()) {
                    "$baseUrl/api/image/series-cover?$coverParam" +
                        (apiKey?.let { "&apiKey=$it" } ?: "")
                } else null
                Series(
                    id = info.seriesId,
                    name = info.seriesName,
                    coverImage = coverUrl,
                    serverId = info.serverId,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadLibraries()
    }

    private fun loadLibraries() {
        viewModelScope.launch {
            try {
                val libs = libraryRepository.getLibraries(activeServerProvider.requireId())
                _uiState.update { it.copy(libraries = libs) }
                loadSeries(page = 1)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Error: ${e.message}") }
            }
        }
    }

    fun selectLibrary(libraryId: Int?) {
        _uiState.update {
            it.copy(
                selectedLibraryId = libraryId,
                series = emptyList(),
                currentPage = 1,
                hasMore = true,
                showDownloadedOnly = false,
            )
        }
        loadSeries(page = 1)
    }

    fun showDownloaded() {
        _uiState.update {
            it.copy(
                showDownloadedOnly = true,
                selectedLibraryId = null,
                isLoading = false,
                isRefreshing = false,
            )
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true, currentPage = 1, hasMore = true) }
        loadSeries(page = 1)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        _uiState.update { it.copy(isLoadingMore = true) }
        loadSeries(page = state.currentPage + 1)
    }

    fun jumpToLetter(letter: Char) {
        viewModelScope.launch {
            val state = _uiState.value
            // Check if we already have a series starting with this letter
            val existingIndex = state.series.indexOfFirst {
                val first = it.sortName.ifBlank { it.name }.firstOrNull()?.uppercaseChar() ?: '#'
                if (letter == '#') !first.isLetter() else first == letter
            }
            if (existingIndex >= 0) {
                _uiState.update { it.copy(scrollToIndex = existingIndex) }
                return@launch
            }
            // Need to load more pages until we find the letter
            if (!state.hasMore) return@launch
            _uiState.update { it.copy(isLoadingMore = true) }
            var currentPage = state.currentPage
            var allSeries = state.series
            var hasMore = state.hasMore
            while (hasMore) {
                currentPage++
                val result = try {
                    seriesRepository.getSeries(
                        libraryId = state.selectedLibraryId,
                        page = currentPage,
                        pageSize = 30,
                    )
                } catch (_: Exception) { break }
                allSeries = allSeries + result.items
                hasMore = currentPage < result.totalPages
                // Check if we found our letter
                val foundIndex = allSeries.indexOfFirst {
                    val first = it.sortName.ifBlank { it.name }.firstOrNull()?.uppercaseChar() ?: '#'
                    if (letter == '#') !first.isLetter() else first == letter
                }
                if (foundIndex >= 0) {
                    _uiState.update {
                        it.copy(
                            series = allSeries,
                            currentPage = currentPage,
                            hasMore = hasMore,
                            isLoadingMore = false,
                            scrollToIndex = foundIndex,
                        )
                    }
                    return@launch
                }
            }
            // Letter not found at all
            _uiState.update {
                it.copy(
                    series = allSeries,
                    currentPage = currentPage,
                    hasMore = hasMore,
                    isLoadingMore = false,
                )
            }
        }
    }

    fun clearScrollToIndex() {
        _uiState.update { it.copy(scrollToIndex = null) }
    }

    private fun loadSeries(page: Int) {
        viewModelScope.launch {
            try {
                val result = seriesRepository.getSeries(
                    libraryId = _uiState.value.selectedLibraryId,
                    page = page,
                    pageSize = 30,
                )
                _uiState.update { current ->
                    val newSeries = if (page == 1) result.items else current.series + result.items
                    current.copy(
                        series = newSeries,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        currentPage = page,
                        hasMore = page < result.totalPages,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        error = "Error: ${e.message}",
                    )
                }
            }
        }
    }
}
