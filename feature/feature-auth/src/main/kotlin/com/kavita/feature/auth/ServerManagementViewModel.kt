package com.kavita.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kavita.core.model.Server
import com.kavita.core.model.ServerType
import com.kavita.core.model.repository.AuthRepository
import com.kavita.core.model.repository.ServerRepository
import com.kavita.core.network.ActiveServerProvider
import com.kavita.core.network.ServerTokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ServerManagementEvent {
    data object NavigateToHome : ServerManagementEvent
    data class NavigateToOpds(val serverUrl: String) : ServerManagementEvent
}

data class ServerManagementUiState(
    val isDeleting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ServerManagementViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val authRepository: AuthRepository,
    private val activeServerProvider: ActiveServerProvider,
    private val tokenStore: ServerTokenStore,
) : ViewModel() {

    val servers: StateFlow<List<Server>> = serverRepository.observeServers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(ServerManagementUiState())
    val uiState: StateFlow<ServerManagementUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ServerManagementEvent>()
    val events = _events.asSharedFlow()

    fun setActiveServer(server: Server) {
        viewModelScope.launch {
            serverRepository.setActiveServer(server.id)
            activeServerProvider.setActiveServer(server.id, server.url, server.apiKey)
            if (server.token != null) {
                tokenStore.setTokens(server.id, server.token!!, server.refreshToken.orEmpty())
            }
            if (server.type == ServerType.OPDS) {
                _events.emit(ServerManagementEvent.NavigateToOpds(server.url))
            } else {
                _events.emit(ServerManagementEvent.NavigateToHome)
            }
        }
    }

    fun removeServer(serverId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            try {
                authRepository.logout(serverId)
                serverRepository.removeServer(serverId)
                tokenStore.removeTokens(serverId)
                if (activeServerProvider.activeServerId.value == serverId) {
                    activeServerProvider.clear()
                    val remaining = servers.value
                    remaining.firstOrNull()?.let { next ->
                        setActiveServer(next)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al eliminar: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isDeleting = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
