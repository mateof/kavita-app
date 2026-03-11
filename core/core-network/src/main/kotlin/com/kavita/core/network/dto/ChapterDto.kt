package com.kavita.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChapterDto(
    val id: Int = 0,
    val range: String = "",
    val number: String = "0",
    val title: String = "",
    val pages: Int = 0,
    val pagesRead: Int = 0,
    val coverImage: String = "",
    val isSpecial: Boolean = false,
    val volumeId: Int = 0,
    val seriesId: Int = 0,
    val created: String = "",
    val releaseDate: String = "",
    val sortOrder: Float = 0f,
    val minNumber: Float = 0f,
    val maxNumber: Float = 0f,
    val wordCount: Long = 0,
)
