package com.kavita.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "epub_page_count")
data class EpubPageCountEntity(
    @PrimaryKey val chapterId: Int,
    val totalPages: Int,
)
