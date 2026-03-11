package com.kavita.core.data.repository

import com.kavita.core.database.dao.ServerDao
import com.kavita.core.database.entity.ServerEntity
import com.kavita.core.model.Server
import com.kavita.core.model.ServerType
import com.kavita.core.model.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepositoryImpl @Inject constructor(
    private val serverDao: ServerDao,
) : ServerRepository {

    override fun observeServers(): Flow<List<Server>> =
        serverDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: Long): Server? =
        serverDao.getById(id)?.toDomain()

    override suspend fun getActiveServer(): Server? =
        serverDao.getActive()?.toDomain()

    override suspend fun addServer(server: Server): Long =
        serverDao.upsert(server.toEntity())

    override suspend fun updateServer(server: Server) {
        serverDao.upsert(server.toEntity())
    }

    override suspend fun removeServer(serverId: Long) {
        serverDao.getById(serverId)?.let { serverDao.delete(it) }
    }

    override suspend fun setActiveServer(serverId: Long) {
        serverDao.deactivateAll()
        serverDao.activate(serverId)
    }

    private fun ServerEntity.toDomain(): Server = Server(
        id = id,
        name = name,
        url = url,
        type = ServerType.valueOf(type),
        username = username,
        apiKey = apiKey,
        token = token,
        refreshToken = refreshToken,
        isActive = isActive,
    )

    private fun Server.toEntity(): ServerEntity = ServerEntity(
        id = id,
        name = name,
        url = url,
        type = type.name,
        username = username,
        apiKey = apiKey,
        token = token,
        refreshToken = refreshToken,
        isActive = isActive,
    )
}
