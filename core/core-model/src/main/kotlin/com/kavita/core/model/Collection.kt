package com.kavita.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Collection(
    val id: Int,
    val title: String,
    val summary: String? = null,
    val coverImage: String? = null,
    val promoted: Boolean = false,
    val seriesCount: Int = 0,
    val serverId: Long = 0,
)
