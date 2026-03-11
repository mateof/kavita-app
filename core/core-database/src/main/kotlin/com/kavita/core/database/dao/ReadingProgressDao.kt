package com.kavita.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kavita.core.database.entity.ReadingProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingProgressDao {
    @Query("SELECT * FROM reading_progress WHERE chapterId = :chapterId AND serverId = :serverId LIMIT 1")
    suspend fun getByChapter(chapterId: Int, serverId: Long): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress WHERE isSynced = 0")
    suspend fun getUnsynced(): List<ReadingProgressEntity>

    @Query("SELECT * FROM reading_progress WHERE serverId = :serverId ORDER BY lastModified DESC")
    fun observeByServer(serverId: Long): Flow<List<ReadingProgressEntity>>

    @Query("SELECT * FROM reading_progress WHERE serverId = :serverId AND pageNum > 0 ORDER BY lastModified DESC LIMIT :limit")
    suspend fun getRecentByServer(serverId: Long, limit: Int = 30): List<ReadingProgressEntity>

    @Upsert
    suspend fun upsert(progress: ReadingProgressEntity)

    @Query("UPDATE reading_progress SET isSynced = 1 WHERE chapterId = :chapterId AND serverId = :serverId")
    suspend fun markSynced(chapterId: Int, serverId: Long)
}
