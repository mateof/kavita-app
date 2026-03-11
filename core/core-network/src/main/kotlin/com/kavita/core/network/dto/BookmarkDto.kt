package com.kavita.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class BookmarkDto(
    val id: Int = 0,
    val page: Int = 0,
    val chapterId: Int = 0,
    val seriesId: Int = 0,
    val volumeId: Int = 0,
)

@Serializable
data class MarkReadDto(
    val seriesId: Int = 0,
    val volumeIds: List<Int> = emptyList(),
    val chapterIds: List<Int> = emptyList(),
)
