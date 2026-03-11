package com.kavita.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey val id: Long = 0,
    val remoteId: Int,
    val serverId: Long,
    val seriesId: Int,
    val volumeId: Int,
    val range: String? = null,
    val number: String = "0",
    val title: String? = null,
    val pages: Int = 0,
    val pagesRead: Int = 0,
    val isSpecial: Boolean = false,
)
