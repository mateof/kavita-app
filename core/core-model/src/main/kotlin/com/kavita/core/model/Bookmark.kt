package com.kavita.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Bookmark(
    val id: Long = 0,
    val chapterId: Int,
    val seriesId: Int,
    val volumeId: Int,
    val serverId: Long,
    val page: Int,
)
