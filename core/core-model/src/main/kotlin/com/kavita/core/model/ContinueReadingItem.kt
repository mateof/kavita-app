package com.kavita.core.model

data class ContinueReadingItem(
    val chapterId: Int,
    val seriesId: Int,
    val volumeId: Int,
    val libraryId: Int = 0,
    val serverId: Long,
    val format: MangaFormat = MangaFormat.UNKNOWN,
    val seriesName: String,
    val chapterTitle: String,
    val coverUrl: String? = null,
    val pagesRead: Int = 0,
    val totalPages: Int = 0,
    val lastModified: Long = 0,
)
