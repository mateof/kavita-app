package com.kavita.core.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class Chapter(
    val id: Int,
    val range: String? = null,
    val number: String = "0",
    val title: String? = null,
    val pages: Int = 0,
    val pagesRead: Int = 0,
    val coverImage: String? = null,
    val isSpecial: Boolean = false,
    val volumeId: Int = 0,
    val seriesId: Int = 0,
    val serverId: Long = 0,
    @Contextual val releaseDate: Instant? = null,
    @Contextual val lastReadingProgress: Instant? = null,
)
