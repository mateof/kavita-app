package com.kavita.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChapterInfoDto(
    val chapterId: Int = 0,
    val volumeId: Int = 0,
    val seriesId: Int = 0,
    val libraryId: Int = 0,
    val chapterNumber: String = "",
    val chapterTitle: String = "",
    val volumeNumber: Int = 0,
    val seriesName: String = "",
    val seriesFormat: Int = 0,
    val libraryType: Int = 0,
    val pages: Int = 0,
    val fileName: String = "",
    val isSpecial: Boolean = false,
    val subtitle: String = "",
    val title: String = "",
)
