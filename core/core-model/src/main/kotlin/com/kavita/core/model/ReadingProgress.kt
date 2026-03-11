package com.kavita.core.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class ReadingProgress(
    val chapterId: Int,
    val seriesId: Int,
    val volumeId: Int,
    val libraryId: Int = 0,
    val serverId: Long,
    val pageNum: Int = 0,
    val bookScrollId: String? = null,
    @Contextual val lastModified: Instant? = null,
    val isSynced: Boolean = false,
)
