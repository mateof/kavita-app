package com.kavita.core.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class Series(
    val id: Int,
    val name: String,
    val sortName: String = "",
    val localizedName: String? = null,
    val originalName: String? = null,
    val coverImage: String? = null,
    val summary: String? = null,
    val format: MangaFormat = MangaFormat.UNKNOWN,
    val libraryId: Int = 0,
    val libraryName: String? = null,
    val pagesRead: Int = 0,
    val pages: Int = 0,
    val userRating: Float = 0f,
    @Contextual val lastChapterAdded: Instant? = null,
    val serverId: Long = 0,
)

@Serializable
enum class MangaFormat {
    IMAGE,      // 0
    ARCHIVE,    // 1 (CBR/CBZ)
    UNKNOWN,    // 2
    EPUB,       // 3
    PDF,        // 4
}
