package com.kavita.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_sessions")
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapterId: Int,
    val seriesId: Int,
    val serverId: Long,
    val startTime: Long,
    val endTime: Long? = null,
    val pagesRead: Int = 0,
    val format: String = "",
)
