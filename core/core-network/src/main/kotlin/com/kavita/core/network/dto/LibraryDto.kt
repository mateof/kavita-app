package com.kavita.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class LibraryDto(
    val id: Int = 0,
    val name: String = "",
    val type: Int = 0,
    val coverImage: String? = null,
    val lastScanned: String = "",
    val folders: List<String> = emptyList(),
)
