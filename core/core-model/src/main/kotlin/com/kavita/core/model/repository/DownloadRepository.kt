package com.kavita.core.model.repository

import com.kavita.core.model.DownloadTask
import com.kavita.core.model.DownloadedSeriesInfo
import kotlinx.coroutines.flow.Flow
import java.io.File

interface DownloadRepository {
    fun observeDownloads(): Flow<List<DownloadTask>>
    suspend fun enqueueDownload(chapterId: Int, seriesId: Int, serverId: Long, seriesName: String, chapterName: String, format: String = "IMAGE")
    suspend fun registerCompletedDownload(chapterId: Int, seriesId: Int, serverId: Long, seriesName: String, chapterName: String, format: String, filePath: String)
    suspend fun cancelDownload(downloadId: Long)
    suspend fun deleteDownload(downloadId: Long)
    suspend fun deleteAllDownloads()
    suspend fun getDownloadedFile(chapterId: Int, serverId: Long): File?
    suspend fun isChapterDownloaded(chapterId: Int, serverId: Long): Boolean
    suspend fun getStorageUsed(): Long
    fun observeDownloadedSeriesIds(): Flow<Set<Int>>
    fun observeDownloadedChapterIds(seriesId: Int, serverId: Long): Flow<Set<Int>>
    fun observeDownloadedSeries(): Flow<List<DownloadedSeriesInfo>>
    suspend fun exportToDownloads(chapterId: Int, serverId: Long, fileName: String): Boolean
    suspend fun downloadAndExport(
        chapterId: Int,
        seriesId: Int,
        serverId: Long,
        seriesName: String,
        chapterName: String,
        format: String,
        fileName: String,
    ): Boolean
}
