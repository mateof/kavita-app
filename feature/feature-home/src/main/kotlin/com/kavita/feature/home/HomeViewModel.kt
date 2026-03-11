package com.kavita.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kavita.core.model.ContinueReadingItem
import com.kavita.core.model.RecentlyUpdatedSeries
import com.kavita.core.model.Series
import com.kavita.core.model.repository.DownloadRepository
import com.kavita.core.model.repository.SeriesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val continueReading: List<ContinueReadingItem> = emptyList(),
    val recentlyUpdated: List<RecentlyUpdatedSeries> = emptyList(),
    val recentlyAdded: List<Series> = emptyList(),
    val onDeck: List<ContinueReadingItem> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val seriesRepository: SeriesRepository,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val downloadedSeriesIds: StateFlow<Set<Int>> = downloadRepository.observeDownloadedSeriesIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        loadHome()
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadHome()
    }

    fun refreshContinueReading() {
        viewModelScope.launch {
            val continueResult = runCatching { seriesRepository.getContinueReading() }
            val onDeckResult = runCatching { seriesRepository.getOnDeck() }
            _uiState.update {
                it.copy(
                    continueReading = continueResult.getOrDefault(it.continueReading),
                    onDeck = onDeckResult.getOrDefault(it.onDeck),
                )
            }
        }
    }

    private fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }

            val continueResult = runCatching {
                seriesRepository.getContinueReading()
            }
            val recentlyUpdatedResult = runCatching {
                seriesRepository.getRecentlyUpdated(pageSize = 20)
            }
            val recentResult = runCatching {
                seriesRepository.getRecentlyAdded(pageSize = 20)
            }
            val onDeckResult = runCatching {
                seriesRepository.getOnDeck()
            }

            _uiState.update {
                it.copy(
                    continueReading = continueResult.getOrDefault(it.continueReading),
                    recentlyUpdated = recentlyUpdatedResult.getOrDefault(it.recentlyUpdated),
                    recentlyAdded = recentResult.map { r -> r.items }.getOrDefault(it.recentlyAdded),
                    onDeck = onDeckResult.getOrDefault(it.onDeck),
                    isLoading = false,
                    isRefreshing = false,
                    error = if (continueResult.isFailure && recentResult.isFailure) {
                        "Error al cargar el inicio"
                    } else null,
                )
            }
        }
    }
}
