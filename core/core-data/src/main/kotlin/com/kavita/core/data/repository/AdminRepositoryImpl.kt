package com.kavita.core.data.repository

import com.kavita.core.model.Library
import com.kavita.core.model.LibraryType
import com.kavita.core.model.repository.AdminRepository
import com.kavita.core.model.repository.ServerAdminInfo
import com.kavita.core.model.repository.UserInfo
import com.kavita.core.network.ActiveServerProvider
import com.kavita.core.network.RetrofitFactory
import com.kavita.core.network.ServerTokenStore
import com.kavita.core.network.api.KavitaLibraryApi
import com.kavita.core.network.api.KavitaServerApi
import com.kavita.core.network.api.KavitaUserApi
import com.kavita.core.network.dto.InviteUserRequest
import com.kavita.core.network.dto.toDomain
import javax.inject.Inject

class AdminRepositoryImpl @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
    private val activeServerProvider: ActiveServerProvider,
    private val tokenStore: ServerTokenStore,
) : AdminRepository {

    override fun isAdmin(): Boolean {
        return try {
            val serverId = activeServerProvider.requireId()
            val token = tokenStore.getAccessToken(serverId) ?: return false
            // Decodificar JWT para verificar rol
            val payload = token.split(".").getOrNull(1) ?: return false
            val decoded = String(android.util.Base64.decode(payload, android.util.Base64.URL_SAFE))
            decoded.contains("\"Admin\"") || decoded.contains("\"role\":\"Admin\"")
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun getServerInfo(): ServerAdminInfo {
        val url = activeServerProvider.requireUrl()
        val api = retrofitFactory.createApi<KavitaServerApi>(url)
        val info = api.getServerInfo()
        return ServerAdminInfo(
            version = info.kavitaVersion,
            os = info.os,
            isDocker = info.isDocker,
            numOfCores = info.numOfCores,
        )
    }

    override suspend fun getLibraries(): List<Library> {
        val url = activeServerProvider.requireUrl()
        val serverId = activeServerProvider.requireId()
        val api = retrofitFactory.createApi<KavitaLibraryApi>(url)
        return api.getLibraries().map { it.toDomain(serverId, url, activeServerProvider.getApiKey()) }
    }

    override suspend fun createLibrary(name: String, type: Int, folders: List<String>): Library {
        // API de creacion de biblioteca - simplificado
        return Library(
            id = 0,
            name = name,
            type = LibraryType.entries.getOrElse(type) { LibraryType.MANGA },
            folders = folders,
        )
    }

    override suspend fun deleteLibrary(libraryId: Int) {
        val url = activeServerProvider.requireUrl()
        val api = retrofitFactory.createApi<KavitaLibraryApi>(url)
        api.deleteLibrary(libraryId)
    }

    override suspend fun scanLibrary(libraryId: Int) {
        val url = activeServerProvider.requireUrl()
        val api = retrofitFactory.createApi<KavitaLibraryApi>(url)
        api.scanLibrary(libraryId)
    }

    override suspend fun refreshMetadata(libraryId: Int) {
        val url = activeServerProvider.requireUrl()
        val api = retrofitFactory.createApi<KavitaLibraryApi>(url)
        api.refreshMetadata(libraryId)
    }

    override suspend fun getUsers(): List<UserInfo> {
        val url = activeServerProvider.requireUrl()
        val api = retrofitFactory.createApi<KavitaUserApi>(url)
        return api.getUsers().map { it.toDomain() }
    }

    override suspend fun inviteUser(email: String, roles: List<String>) {
        val url = activeServerProvider.requireUrl()
        val api = retrofitFactory.createApi<KavitaUserApi>(url)
        api.inviteUser(InviteUserRequest(email = email, roles = roles))
    }

    override suspend fun deleteUser(userId: Int) {
        val url = activeServerProvider.requireUrl()
        val api = retrofitFactory.createApi<KavitaUserApi>(url)
        api.deleteUser(userId)
    }
}
