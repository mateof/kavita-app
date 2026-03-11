package com.kavita.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kavita.core.database.entity.SeriesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeriesDao {

    @Query("SELECT * FROM series WHERE serverId = :serverId ORDER BY sortName ASC")
    fun observeByServer(serverId: Long): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE serverId = :serverId AND libraryId = :libraryId ORDER BY sortName ASC")
    fun observeByLibrary(serverId: Long, libraryId: Int): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE serverId = :serverId ORDER BY sortName ASC")
    suspend fun getByServer(serverId: Long): List<SeriesEntity>

    @Query("SELECT * FROM series WHERE serverId = :serverId AND libraryId = :libraryId ORDER BY sortName ASC")
    suspend fun getByLibrary(serverId: Long, libraryId: Int): List<SeriesEntity>

    @Query("SELECT * FROM series WHERE id = :id")
    suspend fun getById(id: Long): SeriesEntity?

    @Query("SELECT * FROM series WHERE remoteId = :remoteId AND serverId = :serverId")
    suspend fun getByRemoteId(remoteId: Int, serverId: Long): SeriesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(series: SeriesEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(series: List<SeriesEntity>)

    @Query("DELETE FROM series WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE series SET pagesRead = :pagesRead WHERE remoteId = :seriesId AND serverId = :serverId")
    suspend fun updatePagesRead(seriesId: Int, serverId: Long, pagesRead: Int)

    @Query("DELETE FROM series WHERE serverId = :serverId")
    suspend fun deleteByServer(serverId: Long)
}
