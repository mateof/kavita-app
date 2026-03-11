package com.kavita.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class BookInfoDto(
    val bookTitle: String = "",
    val seriesId: Int = 0,
    val volumeId: Int = 0,
    val libraryId: Int = 0,
    val chapterId: Int = 0,
    val pages: Int = 0,
) {
    @Serializable
    data class BookChapterItem(
        val title: String = "",
        val page: Int = 0,
        val part: String = "",
        val children: List<BookChapterItem> = emptyList(),
    )
}
