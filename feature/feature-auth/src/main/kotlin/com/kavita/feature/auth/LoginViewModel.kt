package com.kavita.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kavita.core.model.Server
import com.kavita.core.model.ServerType
import com.kavita.core.model.repository.AuthRepository
import com.kavita.core.model.repository.OpdsRepository
import com.kavita.core.model.repository.ServerRepository
import com.kavita.core.network.ActiveServerProvider
import com.kavita.core.network.ServerTokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val serverType: ServerType = ServerType.KAVITA,
    val isLoading: Boolean = false,
    val isValidating: Boolean = false,
    val serverValidated: Boolean = false,
    val serverName: String? = null,
    val serverVersion: String? = null,
    val error: String? = null,
)

sealed interface LoginEvent {
    data object LoginSuccess : LoginEvent
    data class OpdsConnected(val serverUrl: String) : LoginEvent
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val opdsRepository: OpdsRepository,
    private val serverRepository: ServerRepository,
    private val activeServerProvider: ActiveServerProvider,
    private val tokenStore: ServerTokenStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>()
    val events = _events.asSharedFlow()

    fun onServerUrlChanged(url: String) {
        _uiState.update {
            it.copy(
                serverUrl = url,
                serverValidated = false,
                serverName = null,
                serverVersion = null,
                error = null,
            )
        }
    }

    fun onUsernameChanged(username: String) {
        _uiState.update { it.copy(username = username, error = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun onServerTypeChanged(type: ServerType) {
        _uiState.update {
            it.copy(
                serverType = type,
                serverValidated = false,
                error = null,
            )
        }
    }

    fun validateServer() {
        val url = _uiState.value.serverUrl.trim()
        if (url.isBlank()) {
            _uiState.update { it.copy(error = "Introduce la URL del servidor") }
            return
        }

        if (_uiState.value.serverType == ServerType.OPDS) {
            validateOpdsServer(url)
        } else {
            validateKavitaServer(url)
        }
    }

    private fun validateKavitaServer(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isValidating = true, error = null) }
            authRepository.validateServerUrl(url)
                .onSuccess { info ->
                    _uiState.update {
                        it.copy(
                            isValidating = false,
                            serverValidated = true,
                            serverName = info.serverName,
                            serverVersion = info.version,
                            serverType = if (info.isKavita) ServerType.KAVITA else it.serverType,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isValidating = false,
                            error = "No se ha podido conectar: ${e.message}",
                        )
                    }
                }
        }
    }

    private fun validateOpdsServer(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isValidating = true, error = null) }
            opdsRepository.fetchFeed(url)
                .onSuccess { feed ->
                    _uiState.update {
                        it.copy(
                            isValidating = false,
                            serverValidated = true,
                            serverName = feed.title,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isValidating = false,
                            error = "No se ha podido conectar al feed OPDS: ${e.message}",
                        )
                    }
                }
        }
    }

    fun connectOpds() {
        val state = _uiState.value
        val url = state.serverUrl.trim()
        if (url.isBlank()) {
            _uiState.update { it.copy(error = "Introduce la URL del servidor OPDS") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val server = Server(
                    name = state.serverName ?: "OPDS",
                    url = url,
                    type = ServerType.OPDS,
                    isActive = true,
                )
                val serverId = serverRepository.addServer(server)
                serverRepository.setActiveServer(serverId)
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(LoginEvent.OpdsConnected(url))
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Error al guardar: ${e.message}")
                }
            }
        }
    }

    fun login() {
        val state = _uiState.value
        if (state.username.isBlank()) {
            _uiState.update { it.copy(error = "Introduce el nombre de usuario") }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(error = "Introduce la contraseña") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.login(
                serverUrl = state.serverUrl.trim(),
                username = state.username.trim(),
                password = state.password,
            ).onSuccess { server ->
                tokenStore.setTokens(
                    serverId = server.id,
                    accessToken = server.token.orEmpty(),
                    refreshToken = server.refreshToken.orEmpty(),
                )
                activeServerProvider.setActiveServer(server.id, server.url, server.apiKey)
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(LoginEvent.LoginSuccess)
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error de inicio de sesión: ${e.message}",
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
