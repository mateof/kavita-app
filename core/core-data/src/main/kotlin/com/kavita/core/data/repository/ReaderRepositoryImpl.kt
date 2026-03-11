package com.kavita.core.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.kavita.core.database.dao.BookmarkDao
import com.kavita.core.database.dao.ReadingProgressDao
import com.kavita.core.database.dao.SeriesDao
import com.kavita.core.database.entity.BookmarkEntity
import com.kavita.core.database.entity.ReadingProgressEntity
import com.kavita.core.model.Bookmark
import com.kavita.core.model.ReadingProgress
import com.kavita.core.model.repository.ChapterInfo
import com.kavita.core.model.repository.ReaderRepository
import com.kavita.core.network.ActiveServerProvider
import com.kavita.core.network.RetrofitFactory
import com.kavita.core.network.api.KavitaReaderApi
import com.kavita.core.network.dto.SaveProgressDto
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(kotlin.time.ExperimentalTime::class)
@Singleton
class ReaderRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val retrofitFactory: RetrofitFactory,
    private val activeServerProvider: ActiveServerProvider,
    private val progressDao: ReadingProgressDao,
    private val bookmarkDao: BookmarkDao,
    private val seriesDao: SeriesDao,
    private val okHttpClient: okhttp3.OkHttpClient,
) : ReaderRepository {

    private fun getApi(): KavitaReaderApi =
        retrofitFactory.createApi<KavitaReaderApi>(activeServerProvider.requireUrl())

    private fun serverId(): Long = activeServerProvider.requireId()

    override suspend fun getProgress(chapterId: Int): ReadingProgress? {
        val local = progressDao.getByChapter(chapterId, serverId())

        val remote = try {
            getApi().getProgress(chapterId)
        } catch (_: Exception) {
            null
        }

        // Parse server timestamp for comparison
        val remoteTimestamp = remote?.lastModifiedUtc?.let { utc ->
            if (utc.isNotBlank()) {
                runCatching {
                    java.time.Instant.parse(utc).toEpochMilli()
                }.getOrNull()
            } else null
        } ?: 0L

        val localTimestamp = local?.lastModified ?: 0L

        // Prefer whichever is more recent
        return if (local != null && localTimestamp >= remoteTimestamp) {
            ReadingProgress(
                chapterId = local.chapterId,
                seriesId = local.seriesId,
                volumeId = local.volumeId,
                libraryId = local.libraryId,
                serverId = local.serverId,
                pageNum = local.pageNum,
                bookScrollId = local.bookScrollId,
                lastModified = kotlinx.datetime.Instant.fromEpochMilliseconds(localTimestamp),
            )
        } else if (remote != null) {
            // Cache the server progress locally
            val serverProgress = ReadingProgress(
                chapterId = remote.chapterId,
                seriesId = remote.seriesId,
                volumeId = remote.volumeId,
                libraryId = remote.libraryId,
                serverId = serverId(),
                pageNum = remote.pageNum,
                bookScrollId = remote.bookScrollId,
                lastModified = if (remoteTimestamp > 0) kotlinx.datetime.Instant.fromEpochMilliseconds(remoteTimestamp) else null,
            )
            // Store server progress locally so it's available offline
            progressDao.upsert(
                ReadingProgressEntity(
                    id = "${remote.chapterId}_${serverId()}".hashCode().toLong(),
                    chapterId = remote.chapterId,
                    seriesId = remote.seriesId,
                    volumeId = remote.volumeId,
                    libraryId = remote.libraryId,
                    serverId = serverId(),
                    pageNum = remote.pageNum,
                    bookScrollId = remote.bookScrollId,
                    lastModified = if (remoteTimestamp > 0) remoteTimestamp else System.currentTimeMillis(),
                    isSynced = true,
                )
            )
            serverProgress
        } else {
            null
        }
    }

    override suspend fun saveProgress(progress: ReadingProgress) {
        progressDao.upsert(
            ReadingProgressEntity(
                id = "${progress.chapterId}_${progress.serverId}".hashCode().toLong(),
                chapterId = progress.chapterId,
                seriesId = progress.seriesId,
                volumeId = progress.volumeId,
                libraryId = progress.libraryId,
                serverId = progress.serverId,
                pageNum = progress.pageNum,
                bookScrollId = progress.bookScrollId,
                lastModified = System.currentTimeMillis(),
                isSynced = false,
            )
        )
        // Update pagesRead in the series cache so the UI reflects current progress
        seriesDao.updatePagesRead(
            seriesId = progress.seriesId,
            serverId = progress.serverId,
            pagesRead = progress.pageNum + 1,
        )
        // Schedule sync to server (will retry when network is available)
        scheduleProgressSync()
    }

    override suspend fun syncProgressToServer(chapterId: Int) {
        val local = progressDao.getByChapter(chapterId, serverId()) ?: return
        try {
            val dto = SaveProgressDto(
                chapterId = local.chapterId,
                seriesId = local.seriesId,
                volumeId = local.volumeId,
                libraryId = local.libraryId,
                pageNum = local.pageNum,
                bookScrollId = local.bookScrollId,
            )
            val response = getApi().saveProgress(dto)
            if (response.isSuccessful) {
                progressDao.markSynced(chapterId, serverId())
            } else {
                scheduleProgressSync()
            }
        } catch (_: Exception) {
            // Schedule worker to retry when network is available
            scheduleProgressSync()
        }
    }

    private fun scheduleProgressSync() {
        try {
            val workerClass = Class.forName("com.kavita.sync.progress.ProgressSyncWorker")
                .asSubclass(androidx.work.ListenableWorker::class.java)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequest.Builder(workerClass)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "progress_sync",
                    ExistingWorkPolicy.REPLACE,
                    request,
                )
        } catch (_: Exception) {
            // Worker class not available
        }
    }

    override suspend fun getBookmarks(chapterId: Int): List<Bookmark> =
        bookmarkDao.getByChapter(chapterId, serverId()).map {
            Bookmark(it.id, it.chapterId, it.seriesId, it.volumeId, it.serverId, it.page)
        }

    override suspend fun addBookmark(bookmark: Bookmark) {
        bookmarkDao.insert(
            BookmarkEntity(
                chapterId = bookmark.chapterId,
                seriesId = bookmark.seriesId,
                volumeId = bookmark.volumeId,
                serverId = bookmark.serverId,
                page = bookmark.page,
            )
        )
        try {
            getApi().createBookmark(
                com.kavita.core.network.dto.BookmarkDto(
                    page = bookmark.page,
                    volumeId = bookmark.volumeId,
                    seriesId = bookmark.seriesId,
                    chapterId = bookmark.chapterId,
                )
            )
        } catch (_: Exception) { }
    }

    override suspend fun removeBookmark(bookmark: Bookmark) {
        bookmarkDao.deleteByPage(bookmark.chapterId, bookmark.page, bookmark.serverId)
        try {
            getApi().removeBookmark(
                com.kavita.core.network.dto.BookmarkDto(
                    page = bookmark.page,
                    volumeId = bookmark.volumeId,
                    seriesId = bookmark.seriesId,
                    chapterId = bookmark.chapterId,
                )
            )
        } catch (_: Exception) { }
    }

    override suspend fun getNextChapterId(seriesId: Int, volumeId: Int, currentChapterId: Int): Int? =
        try {
            val id = getApi().getNextChapter(seriesId, volumeId, currentChapterId)
            if (id == -1) null else id
        } catch (_: Exception) { null }

    override suspend fun getPrevChapterId(seriesId: Int, volumeId: Int, currentChapterId: Int): Int? =
        try {
            val id = getApi().getPrevChapter(seriesId, volumeId, currentChapterId)
            if (id == -1) null else id
        } catch (_: Exception) { null }

    override suspend fun getChapterInfo(chapterId: Int): ChapterInfo {
        val info = getApi().getChapterInfo(chapterId)
        return ChapterInfo(
            chapterId = chapterId,
            seriesId = info.seriesId,
            volumeId = info.volumeId,
            libraryId = info.libraryId,
            seriesName = info.seriesName,
            chapterTitle = info.chapterTitle,
            pages = info.pages,
            fileName = info.fileName,
            isBookmark = false,
            subtitle = info.subtitle,
        )
    }

    override fun getPageImageUrl(chapterId: Int, page: Int): String {
        val apiKey = activeServerProvider.getApiKey()?.let { "&apiKey=$it" } ?: ""
        return "${activeServerProvider.requireUrl()}/api/Reader/image?chapterId=$chapterId&page=$page$apiKey"
    }

    override fun getBookPageUrl(chapterId: Int, page: Int): String {
        val baseUrl = activeServerProvider.requireUrl()
        val apiKey = activeServerProvider.getApiKey()?.let { "&apiKey=$it" } ?: ""
        return "$baseUrl/api/Book/$chapterId/book-page?page=$page$apiKey"
    }

    override fun getBookResourceUrl(chapterId: Int, file: String): String {
        val baseUrl = activeServerProvider.requireUrl()
        val apiKey = activeServerProvider.getApiKey()?.let { "&apiKey=$it" } ?: ""
        return "$baseUrl/api/Book/$chapterId/book-resources?file=$file$apiKey"
    }

    override suspend fun getBookPageHtml(chapterId: Int, page: Int): String {
        val url = getBookPageUrl(chapterId, page)
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val request = okhttp3.Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                response.body?.string() ?: ""
            }
        }
    }

    override fun getBaseUrl(): String = activeServerProvider.requireUrl()

    override suspend fun downloadChapterFile(chapterId: Int, cacheDir: java.io.File): java.io.File =
        downloadChapterFile(chapterId, cacheDir) { _, _ -> }

    override suspend fun downloadChapterFile(
        chapterId: Int,
        cacheDir: java.io.File,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit,
    ): java.io.File {
        // Buscar si ya existe con cualquier extension y es valido
        val existing = cacheDir.listFiles()?.firstOrNull {
            it.name.startsWith("chapter_$chapterId.") && it.length() > 1024
        }
        if (existing != null) {
            onProgress(existing.length(), existing.length())
            return existing
        }

        // Descargar a fichero temporal primero
        val tempFile = java.io.File(cacheDir, "chapter_${chapterId}_tmp")
        try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val apiKey = activeServerProvider.getApiKey()?.let { "&apiKey=$it" } ?: ""
                val url = "${activeServerProvider.requireUrl()}/api/Download/chapter?chapterId=$chapterId$apiKey"
                val request = okhttp3.Request.Builder().url(url).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("Descarga fallida: HTTP ${response.code}")
                    val body = response.body ?: error("Respuesta vacia")
                    val totalBytes = body.contentLength()
                    var bytesDownloaded = 0L
                    tempFile.outputStream().use { output ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                bytesDownloaded += bytesRead
                                onProgress(bytesDownloaded, totalBytes)
                            }
                        }
                    }
                }
            }

            // Verificar que el fichero tiene contenido
            if (tempFile.length() < 1024) {
                tempFile.delete()
                error("Fichero descargado demasiado pequeno: ${tempFile.length()} bytes")
            }

            // Renombrar atomicamente
            val finalFile = java.io.File(cacheDir, "chapter_$chapterId.bin")
            finalFile.delete() // Borrar si existia uno corrupto
            if (!tempFile.renameTo(finalFile)) {
                // Fallback: copiar si renameTo falla
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }
            return finalFile
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }
}
