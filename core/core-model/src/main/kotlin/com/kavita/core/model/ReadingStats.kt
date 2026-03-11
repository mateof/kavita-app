package com.kavita.core.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class ReadingSession(
    val id: Long = 0,
    val chapterId: Int,
    val seriesId: Int,
    val serverId: Long,
    @Contextual val startTime: Instant,
    @Contextual val endTime: Instant? = null,
    val pagesRead: Int = 0,
    val format: String = "",
)

data class DailyReadingStat(
    val day: String,
    val seconds: Long,
)

data class ReadingStatsOverview(
    val totalReadingTimeSeconds: Long = 0,
    val totalPagesRead: Int = 0,
    val chaptersCompleted: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
)
