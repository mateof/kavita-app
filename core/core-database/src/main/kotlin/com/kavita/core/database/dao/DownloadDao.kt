package com.kavita.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kavita.core.database.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY id DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE chapterId = :chapterId AND serverId = :serverId LIMIT 1")
    suspend fun getByChapter(chapterId: Int, serverId: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED'")
    suspend fun getCompleted(): List<DownloadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity): Long

    @Update
    suspend fun update(download: DownloadEntity)

    @Delete
    suspend fun delete(download: DownloadEntity)

    @Query("DELETE FROM downloads")
    suspend fun deleteAll()

    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE downloads SET progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Int)

    @Query("SELECT DISTINCT seriesId FROM downloads WHERE status = 'COMPLETED'")
    fun observeDownloadedSeriesIds(): Flow<List<Int>>

    @Query("SELECT chapterId FROM downloads WHERE seriesId = :seriesId AND serverId = :serverId AND status = 'COMPLETED'")
    fun observeDownloadedChapterIds(seriesId: Int, serverId: Long): Flow<List<Int>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY seriesName ASC")
    fun observeCompleted(): Flow<List<DownloadEntity>>
}
