package com.kavita.core.network.interceptor

import com.kavita.core.network.ActiveServerProvider
import com.kavita.core.network.ServerTokenStore
import com.kavita.core.network.TokenRefresher
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ante un 401 del servidor activo, intenta renovar el token (refresh-token o apiKey) y
 * reintenta la petición con el token nuevo. Si no se puede renovar, deja pasar el 401
 * (el [TokenRefresher] habrá emitido SessionExpired para que la UI pida re-login).
 */
@Singleton
class SessionAuthenticator @Inject constructor(
    private val activeServerProvider: ActiveServerProvider,
    private val tokenStore: ServerTokenStore,
    private val tokenRefresher: TokenRefresher,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val serverId = activeServerProvider.activeServerId.value ?: return null

        // Cortar reintentos en cadena (evita bucles si el nuevo token también da 401).
        if (responseCount(response) >= MAX_RETRIES) return null

        synchronized(lock) {
            val requestToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")?.trim()
            val currentToken = tokenStore.getAccessToken(serverId)

            // Si otro hilo ya renovó el token mientras esperábamos, reintentar con ese.
            if (!currentToken.isNullOrBlank() && currentToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val newToken = runBlocking { tokenRefresher.refresh(serverId) }
            if (newToken.isNullOrBlank()) return null

            return response.request.newBuilder()
                .header("Authorization", "Bearer $newToken")
                .build()
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val MAX_RETRIES = 2
        val lock = Any()
    }
}
