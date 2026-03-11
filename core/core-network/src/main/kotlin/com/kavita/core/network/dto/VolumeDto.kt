package com.kavita.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class VolumeDto(
    val id: Int = 0,
    val number: Int = 0,
    val name: String = "",
    val pages: Int = 0,
    val pagesRead: Int = 0,
    val coverImage: String = "",
    val chapters: List<ChapterDto> = emptyList(),
    val seriesId: Int = 0,
    val created: String = "",
    val lastModified: String = "",
    val minNumber: Float = 0f,
    val maxNumber: Float = 0f,
)
