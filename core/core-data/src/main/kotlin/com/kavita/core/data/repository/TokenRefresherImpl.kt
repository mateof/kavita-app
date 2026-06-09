package com.kavita.core.data.repository

import com.kavita.core.database.dao.ServerDao
import com.kavita.core.network.ActiveServerProvider
import com.kavita.core.network.AuthEvent
import com.kavita.core.network.AuthEventBus
import com.kavita.core.network.CleanAccountApiFactory
import com.kavita.core.network.ServerTokenStore
import com.kavita.core.network.TokenRefresher
import com.kavita.core.network.dto.RefreshTokenRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRefresherImpl @Inject constructor(
    private val serverDao: ServerDao,
    private val serverTokenStore: ServerTokenStore,
    private val activeServerProvider: ActiveServerProvider,
    private val authEventBus: AuthEventBus,
    private val cleanAccountApiFactory: CleanAccountApiFactory,
) : TokenRefresher {

    override suspend fun refresh(serverId: Long): String? {
        val server = serverDao.getById(serverId) ?: return null
        val api = cleanAccountApiFactory.create(server.url)

        // 1) refresh-token con el par de tokens actual
        val token = serverTokenStore.getAccessToken(serverId) ?: server.token
        val refreshToken = serverTokenStore.getRefreshToken(serverId) ?: server.refreshToken
        if (!token.isNullOrBlank() && !refreshToken.isNullOrBlank()) {
            val refreshed = runCatching {
                api.refreshToken(RefreshTokenRequest(token, refreshToken))
            }.getOrNull()
            if (refreshed != null && refreshed.token.isNotBlank()) {
                persist(serverId, refreshed.token, refreshed.refreshToken)
                return refreshed.token
            }
        }

        // 2) apiKey (no caduca): Plugin/authenticate
        val apiKey = server.apiKey
        if (!apiKey.isNullOrBlank()) {
            val authed = runCatching {
                api.pluginAuthenticate(apiKey, PLUGIN_NAME)
            }.getOrNull()
            if (authed != null && authed.token.isNotBlank()) {
                persist(serverId, authed.token, authed.refreshToken)
                return authed.token
            }
        }

        // 3) No se pudo renovar: avisar para re-login (sin borrar el servidor).
        authEventBus.emit(AuthEvent.SessionExpired(serverId))
        return null
    }

    private suspend fun persist(serverId: Long, accessToken: String, refreshToken: String) {
        serverTokenStore.setTokens(serverId, accessToken, refreshToken)
        serverDao.updateTokens(serverId, accessToken, refreshToken)
    }

    private companion object {
        const val PLUGIN_NAME = "Kavita Android"
    }
}
