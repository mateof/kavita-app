package com.kavita.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReadingSessionDto(
    val userId: Int = 0,
    val userName: String = "",
    val libraryId: Int = 0,
    val seriesId: Int = 0,
    val seriesName: String = "",
    val pagesRead: Int = 0,
    val timeSpentReading: Long = 0,
)

@Serializable
data class ReadHistoryEventDto(
    val userId: Int = 0,
    val userName: String = "",
    val seriesName: String = "",
    val seriesId: Int = 0,
    val chapterId: Int = 0,
    val readDate: String = "",
)

@Serializable
data class StatCountDto(
    val value: Long = 0,
    val count: Long = 0,
)
