package com.kavita.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kavita.core.database.entity.OpdsFeedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OpdsFeedDao {

    @Query("SELECT * FROM opds_feeds WHERE serverId = :serverId ORDER BY title ASC")
    fun observeByServer(serverId: Long): Flow<List<OpdsFeedEntity>>

    @Query("SELECT * FROM opds_feeds WHERE id = :id")
    suspend fun getById(id: Long): OpdsFeedEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(feed: OpdsFeedEntity): Long

    @Query("DELETE FROM opds_feeds WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM opds_feeds WHERE serverId = :serverId")
    suspend fun deleteByServer(serverId: Long)
}
