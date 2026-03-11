package com.kavita.core.data.repository

import com.kavita.core.database.dao.ServerDao
import com.kavita.core.model.Server
import com.kavita.core.model.ServerType
import com.kavita.core.model.repository.AuthRepository
import com.kavita.core.model.repository.ServerInfo
import com.kavita.core.network.ActiveServerProvider
import com.kavita.core.network.RetrofitFactory
import com.kavita.core.network.ServerTokenStore
import com.kavita.core.network.api.KavitaAccountApi
import com.kavita.core.network.api.KavitaServerApi
import com.kavita.core.network.dto.LoginRequest
import com.kavita.core.network.dto.RefreshTokenRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
    private val serverTokenStore: ServerTokenStore,
    private val activeServerProvider: ActiveServerProvider,
    private val serverDao: ServerDao,
) : AuthRepository {

    override suspend fun login(serverUrl: String, username: String, password: String): Result<Server> =
        runCatching {
            val api = retrofitFactory.createApi<KavitaAccountApi>(serverUrl)
            val response = api.login(LoginRequest(username, password))

            val server = Server(
                name = "Kavita",
                url = serverUrl.trimEnd('/'),
                type = ServerType.KAVITA,
                username = username,
                apiKey = response.apiKey,
                token = response.token,
                refreshToken = response.refreshToken,
                isActive = true,
            )

            serverDao.deactivateAll()
            val serverId = serverDao.upsert(
                com.kavita.core.database.entity.ServerEntity(
                    name = server.name,
                    url = server.url,
                    type = server.type.name,
                    username = server.username,
                    apiKey = server.apiKey,
                    token = server.token,
                    refreshToken = server.refreshToken,
                    isActive = true,
                )
            )

            activeServerProvider.setActiveServer(serverId, server.url, server.apiKey)
            serverTokenStore.setTokens(serverId, response.token, response.refreshToken)

            server.copy(id = serverId)
        }

    override suspend fun refreshToken(serverId: Long): Result<String> =
        runCatching {
            val server = serverDao.getById(serverId)
                ?: throw IllegalStateException("Server not found")
            val api = retrofitFactory.createApi<KavitaAccountApi>(server.url)
            val currentToken = serverTokenStore.getAccessToken(serverId) ?: server.token ?: ""
            val currentRefresh = serverTokenStore.getRefreshToken(serverId) ?: server.refreshToken ?: ""

            val response = api.refreshToken(
                RefreshTokenRequest(currentToken, currentRefresh)
            )

            serverTokenStore.setTokens(serverId, response.token, response.refreshToken)
            serverDao.updateTokens(serverId, response.token, response.refreshToken)

            response.token
        }

    override suspend fun validateServerUrl(url: String): Result<ServerInfo> =
        runCatching {
            val api = retrofitFactory.createApi<KavitaServerApi>(url)
            api.health()
            ServerInfo(
                serverName = "Kavita",
                version = "",
                isKavita = true,
            )
        }

    override suspend fun logout(serverId: Long) {
        serverTokenStore.removeTokens(serverId)
        serverDao.getById(serverId)?.let { serverDao.delete(it) }
    }
}
