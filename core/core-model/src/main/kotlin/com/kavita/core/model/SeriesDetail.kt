package com.kavita.core.model

data class SeriesDetail(
    val specials: List<Chapter> = emptyList(),
    val chapters: List<Chapter> = emptyList(),
    val volumes: List<Volume> = emptyList(),
    val storylineChapters: List<Chapter> = emptyList(),
    val totalCount: Int = 0,
    val unreadCount: Int = 0,
)
