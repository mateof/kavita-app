package com.kavita.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.kavita.core.database.entity.ServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY name")
    fun observeAll(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getById(id: Long): ServerEntity?

    @Query("SELECT * FROM servers WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): ServerEntity?

    @Upsert
    suspend fun upsert(server: ServerEntity): Long

    @Delete
    suspend fun delete(server: ServerEntity)

    @Query("UPDATE servers SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE servers SET isActive = 1 WHERE id = :serverId")
    suspend fun activate(serverId: Long)

    @Query("UPDATE servers SET token = :token, refreshToken = :refreshToken WHERE id = :serverId")
    suspend fun updateTokens(serverId: Long, token: String, refreshToken: String)
}
