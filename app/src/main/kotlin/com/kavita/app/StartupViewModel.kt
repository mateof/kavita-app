package com.kavita.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kavita.core.model.repository.AuthRepository
import com.kavita.core.model.repository.ServerRepository
import com.kavita.core.network.ActiveServerProvider
import com.kavita.core.network.ServerTokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface StartupState {
    data object Loading : StartupState
    data object NeedsLogin : StartupState
    data object Ready : StartupState
}

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val authRepository: AuthRepository,
    private val activeServerProvider: ActiveServerProvider,
    private val tokenStore: ServerTokenStore,
) : ViewModel() {

    private val _state = MutableStateFlow<StartupState>(StartupState.Loading)
    val state: StateFlow<StartupState> = _state.asStateFlow()

    init {
        checkStartup()
    }

    private fun checkStartup() {
        viewModelScope.launch {
            val activeServer = serverRepository.getActiveServer()
            if (activeServer == null) {
                _state.value = StartupState.NeedsLogin
                return@launch
            }

            // Configurar el servidor activo
            activeServerProvider.setActiveServer(activeServer.id, activeServer.url, activeServer.apiKey)

            val token = activeServer.token
            val refreshTk = activeServer.refreshToken

            if (token.isNullOrBlank()) {
                _state.value = StartupState.NeedsLogin
                return@launch
            }

            tokenStore.setTokens(activeServer.id, token, refreshTk.orEmpty())

            // Intentar refrescar el token
            authRepository.refreshToken(activeServer.id)
                .onSuccess { newToken ->
                    tokenStore.setTokens(
                        activeServer.id,
                        newToken,
                        tokenStore.getRefreshToken(activeServer.id).orEmpty(),
                    )
                    _state.value = StartupState.Ready
                }
                .onFailure {
                    // Token inválido, pero intentamos con el existente
                    _state.value = StartupState.Ready
                }
        }
    }
}
