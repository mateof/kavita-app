package com.kavita.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val id: Long = 0,
    val chapterId: Int,
    val seriesId: Int,
    val volumeId: Int,
    val libraryId: Int = 0,
    val serverId: Long,
    val pageNum: Int = 0,
    val bookScrollId: String? = null,
    val lastModified: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
)
