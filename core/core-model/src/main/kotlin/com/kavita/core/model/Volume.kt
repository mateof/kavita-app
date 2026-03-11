package com.kavita.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Volume(
    val id: Int,
    val number: Int,
    val name: String,
    val pages: Int = 0,
    val pagesRead: Int = 0,
    val coverImage: String? = null,
    val chapters: List<Chapter> = emptyList(),
    val seriesId: Int = 0,
    val serverId: Long = 0,
)
