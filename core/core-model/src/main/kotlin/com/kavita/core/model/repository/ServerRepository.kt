package com.kavita.core.model.repository

import com.kavita.core.model.Server
import kotlinx.coroutines.flow.Flow

interface ServerRepository {
    fun observeServers(): Flow<List<Server>>
    suspend fun getById(id: Long): Server?
    suspend fun getActiveServer(): Server?
    suspend fun addServer(server: Server): Long
    suspend fun updateServer(server: Server)
    suspend fun removeServer(serverId: Long)
    suspend fun setActiveServer(serverId: Long)
}
