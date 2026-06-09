package com.kavita.core.network

/**
 * Renueva la sesión de un servidor cuando el access token ha caducado.
 * La implementación (en core-data) prueba: refresh-token, luego apiKey (Plugin/authenticate),
 * persiste los nuevos tokens, y si todo falla emite [AuthEvent.SessionExpired].
 *
 * Vive en core-network como interfaz para que el [SessionAuthenticator] pueda depender de
 * ella sin que core-network dependa de core-database.
 */
interface TokenRefresher {
    /** Devuelve el nuevo access token, o null si no se pudo renovar la sesión. */
    suspend fun refresh(serverId: Long): String?
}
