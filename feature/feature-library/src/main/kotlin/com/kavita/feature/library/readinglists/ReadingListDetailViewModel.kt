package com.kavita.feature.library.readinglists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kavita.core.common.navigation.ReadingListDetailRoute
import com.kavita.core.model.ReadingListItem
import com.kavita.core.model.repository.ReadingListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReadingListDetailUiState(
    val title: String = "Lista de lectura",
    val items: List<ReadingListItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class ReadingListDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val readingListRepository: ReadingListRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ReadingListDetailRoute>()
    private val readingListId: Int = route.readingListId

    private val _uiState = MutableStateFlow(ReadingListDetailUiState())
    val uiState: StateFlow<ReadingListDetailUiState> = _uiState.asStateFlow()

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
                val items = readingListRepository.getReadingListItems(readingListId)
                _uiState.update { it.copy(items = items, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Error: ${e.message}") }
            }
        }
    }
}
