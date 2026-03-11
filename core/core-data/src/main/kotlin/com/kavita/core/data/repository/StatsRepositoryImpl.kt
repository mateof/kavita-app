package com.kavita.core.data.repository

import com.kavita.core.database.dao.ReadingSessionDao
import com.kavita.core.database.entity.ReadingSessionEntity
import com.kavita.core.model.DailyReadingStat
import com.kavita.core.model.ReadingStatsOverview
import com.kavita.core.model.repository.StatsRepository
import com.kavita.core.network.ActiveServerProvider
import com.kavita.core.network.RetrofitFactory
import com.kavita.core.network.api.KavitaStatsApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class StatsRepositoryImpl @Inject constructor(
    private val readingSessionDao: ReadingSessionDao,
    private val retrofitFactory: RetrofitFactory,
    private val activeServerProvider: ActiveServerProvider,
) : StatsRepository {

    override fun observeOverview(): Flow<ReadingStatsOverview> {
        val sinceMillis = 0L
        return combine(
            readingSessionDao.getTotalReadingTimeSeconds(sinceMillis),
            readingSessionDao.getTotalPagesRead(sinceMillis),
            readingSessionDao.observeReadingDates(),
        ) { totalSeconds, totalPages, readingDates ->
            val (currentStreak, longestStreak) = calculateStreaks(readingDates)
            ReadingStatsOverview(
                totalReadingTimeSeconds = totalSeconds,
                totalPagesRead = totalPages,
                chaptersCompleted = 0,
                currentStreak = currentStreak,
                longestStreak = longestStreak,
            )
        }
    }

    override fun observeTodayReadingTimeSeconds(): Flow<Long> {
        val todayStartMillis = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return readingSessionDao.getTotalReadingTimeSeconds(todayStartMillis)
    }

    private fun calculateStreaks(sortedDatesDesc: List<String>): Pair<Int, Int> {
        if (sortedDatesDesc.isEmpty()) return 0 to 0

        val dates = sortedDatesDesc.map { LocalDate.parse(it) }
        val today = LocalDate.now()

        // Current streak: count consecutive days backwards from today or yesterday
        var currentStreak = 0
        var expectedDate = today
        if (dates.first() != today) {
            // Allow yesterday to still count (haven't read today yet)
            expectedDate = today.minusDays(1)
        }
        for (date in dates) {
            if (date == expectedDate) {
                currentStreak++
                expectedDate = expectedDate.minusDays(1)
            } else if (date < expectedDate) {
                break
            }
        }

        // Longest streak: find the longest consecutive sequence
        var longestStreak = 1
        var streak = 1
        for (i in 1 until dates.size) {
            if (dates[i] == dates[i - 1].minusDays(1)) {
                streak++
            } else if (dates[i] != dates[i - 1]) {
                longestStreak = maxOf(longestStreak, streak)
                streak = 1
            }
        }
        longestStreak = maxOf(longestStreak, streak)

        return currentStreak to longestStreak
    }

    override fun observeDailyStats(days: Int): Flow<List<DailyReadingStat>> {
        return readingSessionDao.getRecentSessions(limit = days * 10).map { sessions ->
            sessions.groupBy { entity ->
                Instant.ofEpochMilli(entity.startTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .toString()
            }.map { (day, sessionsForDay) ->
                DailyReadingStat(
                    day = day,
                    seconds = sessionsForDay.sumOf { session ->
                        val end = session.endTime ?: System.currentTimeMillis()
                        (end - session.startTime) / 1000
                    },
                )
            }.sortedBy { it.day }
        }
    }

    override suspend fun startSession(chapterId: Int, seriesId: Int, serverId: Long, format: String) {
        // Cerrar sesiones huerfanas (por crash o cierre abrupto)
        readingSessionDao.getActiveSession()?.let { orphan ->
            readingSessionDao.update(orphan.copy(endTime = System.currentTimeMillis()))
        }
        val entity = ReadingSessionEntity(
            chapterId = chapterId,
            seriesId = seriesId,
            serverId = serverId,
            startTime = System.currentTimeMillis(),
            endTime = null,
            pagesRead = 0,
            format = format,
        )
        readingSessionDao.insert(entity)
    }

    override suspend fun endSession() {
        readingSessionDao.getActiveSession()?.let { session ->
            readingSessionDao.update(
                session.copy(endTime = System.currentTimeMillis())
            )
        }
    }

    override suspend fun recordPageRead() {
        readingSessionDao.getActiveSession()?.let { session ->
            readingSessionDao.update(
                session.copy(pagesRead = session.pagesRead + 1)
            )
        }
    }

    override suspend fun syncWithServer() {
        try {
            val url = activeServerProvider.requireUrl()
            val api = retrofitFactory.createApi<KavitaStatsApi>(url)
            api.getReadingCountByDay(days = 30)
        } catch (_: Exception) {
            // Ignorar errores de sync silenciosamente
        }
    }
}
