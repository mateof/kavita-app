package com.kavita.core.model.repository

import com.kavita.core.model.DailyReadingStat
import com.kavita.core.model.ReadingStatsOverview
import kotlinx.coroutines.flow.Flow

interface StatsRepository {
    fun observeOverview(): Flow<ReadingStatsOverview>
    fun observeDailyStats(days: Int = 30): Flow<List<DailyReadingStat>>
    fun observeTodayReadingTimeSeconds(): Flow<Long>
    suspend fun startSession(chapterId: Int, seriesId: Int, serverId: Long, format: String)
    suspend fun endSession()
    suspend fun recordPageRead()
    suspend fun syncWithServer()
}
