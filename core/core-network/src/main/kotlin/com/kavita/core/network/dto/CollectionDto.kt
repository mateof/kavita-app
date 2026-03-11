package com.kavita.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CollectionTagDto(
    val id: Int = 0,
    val title: String = "",
    val summary: String? = null,
    val promoted: Boolean = false,
    val coverImage: String? = null,
    val normalizedTitle: String? = null,
)
