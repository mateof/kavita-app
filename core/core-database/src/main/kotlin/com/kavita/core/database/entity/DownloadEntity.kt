package com.kavita.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapterId: Int,
    val seriesId: Int,
    val serverId: Long,
    val seriesName: String = "",
    val chapterName: String = "",
    val status: String = "PENDING",
    val progress: Int = 0,
    val totalPages: Int = 0,
    val filePath: String? = null,
    val format: String = "IMAGE",
)
