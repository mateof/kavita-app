package com.kavita.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kavita.core.database.dao.ServerDao
import com.kavita.core.model.repository.AuthRepository
import com.kavita.core.network.AuthEvent
import com.kavita.core.network.AuthEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionExpiryUiState(
    val visible: Boolean = false,
    val serverId: Long = -1L,
    val serverName: String = "",
    val username: String = "",
    val submitting: Boolean = false,
    val error: Boolean = false,
)

/**
 * Escucha [AuthEvent.SessionExpired] y muestra un diálogo para volver a iniciar sesión en el
 * servidor existente (sin borrarlo). El servidor y los datos en caché se conservan.
 */
@HiltViewModel
class SessionExpiryViewModel @Inject constructor(
    private val authEventBus: AuthEventBus,
    private val authRepository: AuthRepository,
    private val serverDao: ServerDao,
) : ViewModel() {

    private val _state = MutableStateFlow(SessionExpiryUiState())
    val state: StateFlow<SessionExpiryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authEventBus.events.collect { event ->
                if (event !is AuthEvent.SessionExpired) return@collect
                // No reabrir si ya está visible para ese servidor.
                if (_state.value.visible && _state.value.serverId == event.serverId) return@collect
                val server = serverDao.getById(event.serverId)
                _state.update {
                    SessionExpiryUiState(
                        visible = true,
                        serverId = event.serverId,
                        serverName = server?.name ?: "Kavita",
                        username = server?.username.orEmpty(),
                    )
                }
            }
        }
    }

    fun submit(password: String) {
        val serverId = _state.value.serverId
        if (serverId < 0 || password.isBlank()) return
        _state.update { it.copy(submitting = true, error = false) }
        viewModelScope.launch {
            val result = authRepository.reauthenticate(serverId, password)
            if (result.isSuccess) {
                _state.update { SessionExpiryUiState() }
            } else {
                _state.update { it.copy(submitting = false, error = true) }
            }
        }
    }

    fun dismiss() {
        _state.update { SessionExpiryUiState() }
    }
}
