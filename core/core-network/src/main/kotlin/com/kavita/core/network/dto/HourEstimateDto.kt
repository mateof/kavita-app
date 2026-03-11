package com.kavita.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class HourEstimateDto(
    val minHours: Float = 0f,
    val maxHours: Float = 0f,
    val avgHours: Float = 0f,
)
