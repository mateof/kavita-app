package com.kavita.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ReadingList(
    val id: Int,
    val title: String,
    val summary: String? = null,
    val coverImage: String? = null,
    val promoted: Boolean = false,
    val itemCount: Int = 0,
    val serverId: Long = 0,
)

@Serializable
data class ReadingListItem(
    val id: Int,
    val seriesId: Int,
    val seriesName: String,
    val chapterId: Int,
    val chapterNumber: String = "",
    val order: Int = 0,
    val coverImage: String? = null,
)
