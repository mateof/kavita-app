package com.kavita.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CollectionTagBulkAddDto(
    val collectionTagId: Int,
    val collectionTagTitle: String = "",
    val seriesIds: List<Int>,
)
