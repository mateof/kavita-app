package com.kavita.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateReadingListByMultipleSeriesDto(
    val readingListId: Int,
    val seriesIds: List<Int>,
)
