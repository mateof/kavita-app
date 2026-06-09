package com.kavita.core.model.repository

import com.kavita.core.model.Server

interface AuthRepository {
    suspend fun login(serverUrl: String, username: String, password: String): Result<Server>
    suspend fun refreshToken(serverId: Long): Result<String>
    suspend fun validateServerUrl(url: String): Result<ServerInfo>
    suspend fun logout(serverId: Long)

    /**
     * Vuelve a iniciar sesión en un servidor ya existente (tras caducar la sesión) usando su
     * usuario guardado y la [password] que introduce el usuario. Actualiza los tokens del
     * mismo servidor SIN borrarlo ni perder los datos en caché.
     */
    suspend fun reauthenticate(serverId: Long, password: String): Result<Unit>
}

data class ServerInfo(
    val serverName: String,
    val version: String,
    val isKavita: Boolean,
)
