package com.kavita.sync.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kavita.core.database.dao.DownloadDao
import com.kavita.core.network.ActiveServerProvider
import com.kavita.core.network.RetrofitFactory
import com.kavita.core.network.api.KavitaReaderApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadDao: DownloadDao,
    private val retrofitFactory: RetrofitFactory,
    private val activeServerProvider: ActiveServerProvider,
    private val okHttpClient: okhttp3.OkHttpClient,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_CHAPTER_ID = "chapter_id"
        const val KEY_SERIES_ID = "series_id"
        const val KEY_SERVER_ID = "server_id"
        const val KEY_TOTAL_PAGES = "total_pages"
        const val KEY_SERIES_NAME = "series_name"
        const val KEY_CHAPTER_NAME = "chapter_name"
        const val KEY_FORMAT = "format"
        const val CHANNEL_ID = "kavita_downloads"
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        val downloadId = inputData.getLong(KEY_DOWNLOAD_ID, -1)
        val chapterId = inputData.getInt(KEY_CHAPTER_ID, -1)
        val seriesId = inputData.getInt(KEY_SERIES_ID, -1)
        val serverId = inputData.getLong(KEY_SERVER_ID, -1)
        val totalPages = inputData.getInt(KEY_TOTAL_PAGES, 0)
        val seriesName = inputData.getString(KEY_SERIES_NAME) ?: ""
        val chapterName = inputData.getString(KEY_CHAPTER_NAME) ?: ""
        val format = inputData.getString(KEY_FORMAT) ?: "IMAGE"

        if (downloadId < 0 || chapterId < 0) return Result.failure()

        createNotificationChannel()

        return try {
            when (format) {
                "PDF", "EPUB" -> downloadFullFile(downloadId, chapterId, seriesId, serverId, seriesName, chapterName, format)
                else -> downloadPages(downloadId, chapterId, seriesId, serverId, totalPages, seriesName, chapterName)
            }
        } catch (e: Exception) {
            downloadDao.updateStatus(downloadId, "FAILED")
            Result.retry()
        }
    }

    private suspend fun downloadFullFile(
        downloadId: Long,
        chapterId: Int,
        seriesId: Int,
        serverId: Long,
        seriesName: String,
        chapterName: String,
        format: String,
    ): Result {
        setForeground(createForegroundInfo(seriesName, chapterName, 0, 1))
        downloadDao.updateStatus(downloadId, "DOWNLOADING")

        val ext = format.lowercase()
        val downloadDir = File(context.filesDir, "downloads/$serverId/$seriesId")
        downloadDir.mkdirs()

        val targetFile = File(downloadDir, "chapter_$chapterId.$ext")
        if (targetFile.exists() && targetFile.length() > 1024) {
            downloadDao.updateProgress(downloadId, 100)
            downloadDao.updateStatus(downloadId, "COMPLETED")
            return Result.success()
        }

        val tempFile = File(downloadDir, "chapter_${chapterId}_tmp")
        val apiKey = activeServerProvider.getApiKey()?.let { "&apiKey=$it" } ?: ""
        val url = "${activeServerProvider.requireUrl()}/api/Download/chapter?chapterId=$chapterId$apiKey"
        val request = okhttp3.Request.Builder().url(url).build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Descarga fallida: HTTP ${response.code}")
            val body = response.body ?: error("Respuesta vacia")
            val contentLength = body.contentLength()
            tempFile.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Long = 0
                    var len: Int
                    while (input.read(buffer).also { len = it } != -1) {
                        output.write(buffer, 0, len)
                        bytesRead += len
                        if (contentLength > 0) {
                            val progress = (bytesRead * 100 / contentLength).toInt()
                            downloadDao.updateProgress(downloadId, progress)
                            setForeground(createForegroundInfo(seriesName, chapterName, progress, 100))
                        }
                    }
                }
            }
        }

        if (tempFile.length() < 1024) {
            tempFile.delete()
            error("Fichero descargado demasiado pequeno")
        }

        targetFile.delete()
        if (!tempFile.renameTo(targetFile)) {
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
        }

        // Guardar metadata
        val metadataFile = File(downloadDir, "chapter_${chapterId}_metadata.json")
        metadataFile.writeText(
            """{"chapterId":$chapterId,"seriesId":$seriesId,"serverId":$serverId,"format":"$format","seriesName":"$seriesName","chapterName":"$chapterName"}"""
        )

        downloadDao.updateProgress(downloadId, 100)
        downloadDao.updateStatus(downloadId, "COMPLETED")
        return Result.success()
    }

    private suspend fun downloadPages(
        downloadId: Long,
        chapterId: Int,
        seriesId: Int,
        serverId: Long,
        totalPages: Int,
        seriesName: String,
        chapterName: String,
    ): Result {
        if (totalPages == 0) return Result.failure()

        setForeground(createForegroundInfo(seriesName, chapterName, 0, totalPages))
        downloadDao.updateStatus(downloadId, "DOWNLOADING")

        val downloadDir = File(context.filesDir, "downloads/$serverId/$seriesId/$chapterId")
        downloadDir.mkdirs()

        val url = activeServerProvider.requireUrl()
        val api = retrofitFactory.createApi<KavitaReaderApi>(url)

        for (page in 0 until totalPages) {
            val pageFile = File(downloadDir, "page_$page.jpg")
            if (pageFile.exists() && pageFile.length() > 0) continue

            val response = api.getPageImage(chapterId, page)
            pageFile.outputStream().use { output ->
                response.byteStream().use { input ->
                    input.copyTo(output)
                }
            }

            val progress = ((page + 1).toFloat() / totalPages * 100).toInt()
            setForeground(createForegroundInfo(seriesName, chapterName, page + 1, totalPages))
            downloadDao.updateProgress(downloadId, progress)
            setProgress(workDataOf("progress" to progress))
        }

        // Guardar metadata
        val metadataFile = File(downloadDir, "metadata.json")
        metadataFile.writeText(
            """{"chapterId":$chapterId,"seriesId":$seriesId,"serverId":$serverId,"totalPages":$totalPages,"seriesName":"$seriesName","chapterName":"$chapterName"}"""
        )

        downloadDao.updateStatus(downloadId, "COMPLETED")
        return Result.success()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Descargas",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Progreso de descargas"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createForegroundInfo(
        seriesName: String,
        chapterName: String,
        current: Int,
        total: Int,
    ): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Descargando: $seriesName")
            .setContentText("$chapterName - $current/$total")
            .setProgress(total, current, false)
            .setOngoing(true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }
}
