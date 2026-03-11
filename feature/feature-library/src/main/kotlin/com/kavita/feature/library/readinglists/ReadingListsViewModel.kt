package com.kavita.feature.library.readinglists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kavita.core.model.ReadingList
import com.kavita.core.model.repository.ReadingListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReadingListsUiState(
    val readingLists: List<ReadingList> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class ReadingListsViewModel @Inject constructor(
    private val readingListRepository: ReadingListRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReadingListsUiState())
    val uiState: StateFlow<ReadingListsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val lists = readingListRepository.getReadingLists()
                _uiState.update { it.copy(readingLists = lists, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Error: ${e.message}") }
            }
        }
    }
}
