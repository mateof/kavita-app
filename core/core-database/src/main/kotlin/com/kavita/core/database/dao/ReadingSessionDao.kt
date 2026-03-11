package com.kavita.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kavita.core.database.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingSessionDao {
    @Insert
    suspend fun insert(session: ReadingSessionEntity): Long

    @Update
    suspend fun update(session: ReadingSessionEntity)

    @Query("SELECT * FROM reading_sessions WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSession(): ReadingSessionEntity?

    @Query("""
        SELECT COALESCE(SUM(
            CASE WHEN endTime IS NOT NULL
            THEN (endTime - startTime) / 1000
            ELSE 0 END
        ), 0)
        FROM reading_sessions
        WHERE startTime >= :sinceMillis
    """)
    fun getTotalReadingTimeSeconds(sinceMillis: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(pagesRead), 0) FROM reading_sessions WHERE startTime >= :sinceMillis")
    fun getTotalPagesRead(sinceMillis: Long): Flow<Int>

    @Query("""
        SELECT COUNT(DISTINCT date(startTime / 1000, 'unixepoch', 'localtime'))
        FROM reading_sessions
        WHERE startTime >= :sinceMillis
    """)
    fun getDaysRead(sinceMillis: Long): Flow<Int>

    @Query("SELECT * FROM reading_sessions ORDER BY startTime DESC LIMIT :limit")
    fun getRecentSessions(limit: Int = 100): Flow<List<ReadingSessionEntity>>

    @Query("""
        SELECT DISTINCT date(startTime / 1000, 'unixepoch', 'localtime') as readDate
        FROM reading_sessions
        ORDER BY readDate DESC
    """)
    fun observeReadingDates(): Flow<List<String>>
}
