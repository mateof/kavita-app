package com.kavita.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DownloadTask(
    val id: Long = 0,
    val chapterId: Int,
    val seriesId: Int,
    val serverId: Long,
    val seriesName: String = "",
    val chapterName: String = "",
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Int = 0,
    val totalPages: Int = 0,
    val filePath: String? = null,
    val format: String = "IMAGE",
)

@Serializable
enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED,
}

data class DownloadedSeriesInfo(
    val seriesId: Int,
    val serverId: Long,
    val seriesName: String,
    val chapterCount: Int,
)
