package com.kavita.feature.opds

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kavita.core.common.navigation.OpdsBrowseRoute
import com.kavita.core.model.OpdsFeed
import com.kavita.core.model.OpdsEntry
import com.kavita.core.model.repository.OpdsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OpdsBrowseUiState(
    val feedStack: List<OpdsFeed> = emptyList(),
    val currentFeed: OpdsFeed? = null,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val searchQuery: String = "",
    val error: String? = null,
)

@HiltViewModel
class OpdsBrowseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val opdsRepository: OpdsRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<OpdsBrowseRoute>()
    private val serverUrl = route.serverUrl

    private val _uiState = MutableStateFlow(OpdsBrowseUiState())
    val uiState: StateFlow<OpdsBrowseUiState> = _uiState.asStateFlow()

    init {
        loadFeed(serverUrl)
    }

    private fun loadFeed(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            opdsRepository.fetchFeed(url)
                .onSuccess { feed ->
                    _uiState.update {
                        it.copy(
                            currentFeed = feed,
                            feedStack = it.feedStack + feed,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = "Error al cargar: ${e.message}")
                    }
                }
        }
    }

    fun loadMore() {
        val nextUrl = _uiState.value.currentFeed?.links?.next ?: return
        if (_uiState.value.isLoadingMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            opdsRepository.fetchFeed(nextUrl)
                .onSuccess { nextFeed ->
                    _uiState.update { state ->
                        val current = state.currentFeed ?: return@update state
                        val merged = current.copy(
                            entries = current.entries + nextFeed.entries,
                            links = nextFeed.links,
                        )
                        val newStack = state.feedStack.dropLast(1) + merged
                        state.copy(
                            currentFeed = merged,
                            feedStack = newStack,
                            isLoadingMore = false,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoadingMore = false, error = "Error al cargar más: ${e.message}")
                    }
                }
        }
    }

    fun navigateToEntry(entry: OpdsEntry) {
        val link = entry.navigationLink ?: return
        loadFeed(link)
    }

    fun navigateBack(): Boolean {
        val stack = _uiState.value.feedStack
        if (stack.size <= 1) return false

        _uiState.update {
            val newStack = stack.dropLast(1)
            it.copy(
                feedStack = newStack,
                currentFeed = newStack.lastOrNull(),
            )
        }
        return true
    }

    fun search(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, searchQuery = query) }
            opdsRepository.searchCatalog(serverUrl, query)
                .onSuccess { feed ->
                    _uiState.update {
                        it.copy(
                            currentFeed = feed,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = "Error: ${e.message}")
                    }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
}
