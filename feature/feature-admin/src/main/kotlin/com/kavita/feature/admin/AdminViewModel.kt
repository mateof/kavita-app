package com.kavita.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kavita.core.model.Library
import com.kavita.core.model.repository.AdminRepository
import com.kavita.core.model.repository.ServerAdminInfo
import com.kavita.core.model.repository.UserInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val isAdmin: Boolean = false,
    val serverInfo: ServerAdminInfo? = null,
    val libraries: List<Library> = emptyList(),
    val isLoading: Boolean = true,
    val isScanningLibrary: Int? = null,
    val error: String? = null,
    val users: List<UserInfo> = emptyList(),
    val isLoadingUsers: Boolean = false,
    val showInviteDialog: Boolean = false,
    val isDeletingUser: Int? = null,
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        checkAdmin()
    }

    private fun checkAdmin() {
        viewModelScope.launch {
            val isAdmin = adminRepository.isAdmin()
            if (!isAdmin) {
                _uiState.update { it.copy(isAdmin = false, isLoading = false) }
                return@launch
            }
            _uiState.update { it.copy(isAdmin = true) }
            loadAdminData()
        }
    }

    private fun loadAdminData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val info = adminRepository.getServerInfo()
                _uiState.update { it.copy(serverInfo = info) }
            } catch (_: Exception) { /* ignore */ }

            try {
                val libs = adminRepository.getLibraries()
                _uiState.update { it.copy(libraries = libs) }
            } catch (_: Exception) { /* ignore */ }

            _uiState.update { it.copy(isLoading = false) }

            loadUsers()
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingUsers = true) }
            try {
                val users = adminRepository.getUsers()
                _uiState.update { it.copy(users = users, isLoadingUsers = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingUsers = false, error = "Error al cargar usuarios: ${e.message}") }
            }
        }
    }

    fun inviteUser(email: String, roles: List<String> = listOf("Pleb")) {
        viewModelScope.launch {
            try {
                adminRepository.inviteUser(email, roles)
                _uiState.update { it.copy(showInviteDialog = false) }
                loadUsers()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al invitar usuario: ${e.message}") }
            }
        }
    }

    fun deleteUser(userId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingUser = userId) }
            try {
                adminRepository.deleteUser(userId)
                loadUsers()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al eliminar usuario: ${e.message}") }
            }
            _uiState.update { it.copy(isDeletingUser = null) }
        }
    }

    fun showInviteDialog() {
        _uiState.update { it.copy(showInviteDialog = true) }
    }

    fun dismissInviteDialog() {
        _uiState.update { it.copy(showInviteDialog = false) }
    }

    fun scanLibrary(libraryId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningLibrary = libraryId) }
            adminRepository.scanLibrary(libraryId)
            _uiState.update { it.copy(isScanningLibrary = null) }
        }
    }

    fun refreshMetadata(libraryId: Int) {
        viewModelScope.launch {
            adminRepository.refreshMetadata(libraryId)
        }
    }

    fun refresh() {
        loadAdminData()
    }
}
