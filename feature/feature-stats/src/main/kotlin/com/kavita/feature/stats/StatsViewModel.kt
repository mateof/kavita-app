package com.kavita.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kavita.core.data.preferences.UserPreferencesDataStore
import com.kavita.core.model.DailyReadingStat
import com.kavita.core.model.ReadingStatsOverview
import com.kavita.core.model.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatsUiState(
    val overview: ReadingStatsOverview? = null,
    val dailyStats: List<DailyReadingStat> = emptyList(),
    val todayReadingSeconds: Long = 0,
    val dailyGoalMinutes: Int = 30,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun refresh() {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            launch {
                statsRepository.observeOverview().collect { overview ->
                    _uiState.update { it.copy(overview = overview) }
                }
            }

            launch {
                statsRepository.observeDailyStats(days = 30).collect { daily ->
                    _uiState.update { it.copy(dailyStats = daily, isLoading = false) }
                }
            }

            launch {
                statsRepository.observeTodayReadingTimeSeconds().collect { seconds ->
                    _uiState.update { it.copy(todayReadingSeconds = seconds) }
                }
            }

            launch {
                userPreferencesDataStore.preferences
                    .map { it.dailyReadingGoalMinutes }
                    .collect { goalMinutes ->
                        _uiState.update { it.copy(dailyGoalMinutes = goalMinutes) }
                    }
            }
        }
    }

    fun syncWithServer() {
        viewModelScope.launch {
            statsRepository.syncWithServer()
        }
    }
}
