package com.kavita.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapterId: Int,
    val seriesId: Int,
    val volumeId: Int,
    val serverId: Long,
    val page: Int,
)
