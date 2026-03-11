package com.kavita.core.model.repository

import com.kavita.core.model.Server

interface AuthRepository {
    suspend fun login(serverUrl: String, username: String, password: String): Result<Server>
    suspend fun refreshToken(serverId: Long): Result<String>
    suspend fun validateServerUrl(url: String): Result<ServerInfo>
    suspend fun logout(serverId: Long)
}

data class ServerInfo(
    val serverName: String,
    val version: String,
    val isKavita: Boolean,
)
