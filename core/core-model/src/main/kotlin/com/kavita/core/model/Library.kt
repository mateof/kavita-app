package com.kavita.core.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class Library(
    val id: Int,
    val name: String,
    val type: LibraryType,
    val coverImage: String? = null,
    @Contextual val lastScanned: Instant? = null,
    val folders: List<String> = emptyList(),
    val serverId: Long = 0,
)

@Serializable
enum class LibraryType {
    MANGA,
    COMIC,
    BOOK,
    IMAGE,
    LIGHT_NOVEL,
    COMIC_VINE,
}
