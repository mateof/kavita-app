package com.kavita.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PaginationInfo(
    val currentPage: Int = 1,
    val itemsPerPage: Int = 20,
    val totalItems: Int = 0,
    val totalPages: Int = 1,
)
