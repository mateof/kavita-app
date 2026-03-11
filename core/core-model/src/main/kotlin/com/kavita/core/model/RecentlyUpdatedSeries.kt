package com.kavita.core.model

data class RecentlyUpdatedSeries(
    val seriesId: Int,
    val seriesName: String,
    val coverImage: String? = null,
    val libraryId: Int = 0,
    val format: MangaFormat = MangaFormat.UNKNOWN,
    val count: Int = 0,
    val serverId: Long = 0,
)
