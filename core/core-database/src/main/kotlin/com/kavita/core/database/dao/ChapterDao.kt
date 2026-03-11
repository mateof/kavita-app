package com.kavita.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kavita.core.database.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {

    @Query("SELECT * FROM chapters WHERE serverId = :serverId AND seriesId = :seriesId ORDER BY number ASC")
    fun observeBySeries(serverId: Long, seriesId: Int): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE serverId = :serverId AND volumeId = :volumeId ORDER BY number ASC")
    fun observeByVolume(serverId: Long, volumeId: Int): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getById(id: Long): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE remoteId = :remoteId AND serverId = :serverId")
    suspend fun getByRemoteId(remoteId: Int, serverId: Long): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(chapter: ChapterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(chapters: List<ChapterEntity>)

    @Query("DELETE FROM chapters WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM chapters WHERE serverId = :serverId")
    suspend fun deleteByServer(serverId: Long)
}
