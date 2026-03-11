package com.kavita.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateReadingListRequest(
    val title: String,
)
