package com.kavita.core.model.repository

import com.kavita.core.model.Bookmark
import com.kavita.core.model.ReadingProgress
import java.io.File

interface ReaderRepository {
    suspend fun getProgress(chapterId: Int): ReadingProgress?
    suspend fun saveProgress(progress: ReadingProgress)
    suspend fun syncProgressToServer(chapterId: Int)
    suspend fun getBookmarks(chapterId: Int): List<Bookmark>
    suspend fun addBookmark(bookmark: Bookmark)
    suspend fun removeBookmark(bookmark: Bookmark)
    suspend fun getNextChapterId(seriesId: Int, volumeId: Int, currentChapterId: Int): Int?
    suspend fun getPrevChapterId(seriesId: Int, volumeId: Int, currentChapterId: Int): Int?
    suspend fun getChapterInfo(chapterId: Int): ChapterInfo
    fun getPageImageUrl(chapterId: Int, page: Int): String
    fun getBookPageUrl(chapterId: Int, page: Int): String
    fun getBookResourceUrl(chapterId: Int, file: String): String
    suspend fun getBookPageHtml(chapterId: Int, page: Int): String
    fun getBaseUrl(): String
    suspend fun downloadChapterFile(chapterId: Int, cacheDir: File): File

    suspend fun downloadChapterFile(
        chapterId: Int,
        cacheDir: File,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit,
    ): File
}

data class ChapterInfo(
    val chapterId: Int,
    val seriesId: Int,
    val volumeId: Int,
    val libraryId: Int,
    val seriesName: String,
    val chapterTitle: String,
    val pages: Int,
    val fileName: String,
    val isBookmark: Boolean,
    val subtitle: String,
)
