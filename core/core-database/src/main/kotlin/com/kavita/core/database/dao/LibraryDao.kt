package com.kavita.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kavita.core.database.entity.LibraryEntity

@Dao
interface LibraryDao {

    @Query("SELECT * FROM libraries WHERE serverId = :serverId ORDER BY name ASC")
    suspend fun getByServer(serverId: Long): List<LibraryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(libraries: List<LibraryEntity>)

    @Query("DELETE FROM libraries WHERE serverId = :serverId")
    suspend fun deleteByServer(serverId: Long)
}
