package com.kavita.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReadingListDto(
    val id: Int = 0,
    val title: String = "",
    val summary: String? = null,
    val promoted: Boolean = false,
    val coverImage: String? = null,
    val items: List<ReadingListItemDto> = emptyList(),
)

@Serializable
data class ReadingListItemDto(
    val id: Int = 0,
    val seriesId: Int = 0,
    val seriesName: String = "",
    val chapterId: Int = 0,
    val chapterNumber: String = "",
    val order: Int = 0,
)
