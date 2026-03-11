package com.kavita.core.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kavita.core.database.dao.DownloadDao
import com.kavita.core.database.entity.DownloadEntity
import com.kavita.core.model.DownloadStatus
import com.kavita.core.model.DownloadTask
import com.kavita.core.model.DownloadedSeriesInfo
import com.kavita.core.model.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val okHttpClient: okhttp3.OkHttpClient,
    private val activeServerProvider: com.kavita.core.network.ActiveServerProvider,
) : DownloadRepository {

    companion object {
        private const val KEY_CHAPTER_ID = "chapter_id"
        private const val KEY_SERIES_ID = "series_id"
        private const val KEY_SERVER_ID = "server_id"
        private const val KEY_SERIES_NAME = "series_name"
        private const val KEY_CHAPTER_NAME = "chapter_name"
        private const val KEY_FORMAT = "format"

        private val DOCUMENT_FORMATS = setOf("PDF", "EPUB")
    }

    override fun observeDownloads(): Flow<List<DownloadTask>> {
        return downloadDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun enqueueDownload(
        chapterId: Int,
        seriesId: Int,
        serverId: Long,
        seriesName: String,
        chapterName: String,
        format: String,
    ) {
        val entity = DownloadEntity(
            chapterId = chapterId,
            seriesId = seriesId,
            serverId = serverId,
            seriesName = seriesName,
            chapterName = chapterName,
            status = DownloadStatus.PENDING.name,
            progress = 0,
            totalPages = 0,
            filePath = null,
            format = format,
        )
        downloadDao.insert(entity)

        @Suppress("UNCHECKED_CAST")
        val workerClass = Class.forName("com.kavita.sync.download.DownloadWorker") as Class<androidx.work.ListenableWorker>
        val workRequest = OneTimeWorkRequest.Builder(workerClass)
            .setInputData(
                workDataOf(
                    KEY_CHAPTER_ID to chapterId,
                    KEY_SERIES_ID to seriesId,
                    KEY_SERVER_ID to serverId,
                    KEY_SERIES_NAME to seriesName,
                    KEY_CHAPTER_NAME to chapterName,
                    KEY_FORMAT to format,
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "download_$chapterId",
                ExistingWorkPolicy.KEEP,
                workRequest,
            )
    }

    override suspend fun registerCompletedDownload(
        chapterId: Int,
        seriesId: Int,
        serverId: Long,
        seriesName: String,
        chapterName: String,
        format: String,
        filePath: String,
    ) {
        val existing = downloadDao.getByChapter(chapterId, serverId)
        if (existing != null && existing.status == DownloadStatus.COMPLETED.name) return

        if (existing != null) {
            downloadDao.update(
                existing.copy(
                    status = DownloadStatus.COMPLETED.name,
                    progress = 100,
                    filePath = filePath,
                    format = format,
                    seriesName = seriesName,
                    chapterName = chapterName,
                )
            )
        } else {
            downloadDao.insert(
                DownloadEntity(
                    chapterId = chapterId,
                    seriesId = seriesId,
                    serverId = serverId,
                    seriesName = seriesName,
                    chapterName = chapterName,
                    status = DownloadStatus.COMPLETED.name,
                    progress = 100,
                    totalPages = 0,
                    filePath = filePath,
                    format = format,
                )
            )
        }
    }

    override suspend fun cancelDownload(downloadId: Long) {
        downloadDao.updateStatus(downloadId, DownloadStatus.CANCELLED.name)
    }

    override suspend fun deleteDownload(downloadId: Long) {
        val download = downloadDao.getById(downloadId) ?: return
        deleteDownloadFiles(download)
        downloadDao.delete(download)
    }

    override suspend fun deleteAllDownloads() {
        val downloads = downloadDao.getCompleted()
        downloads.forEach { download -> deleteDownloadFiles(download) }
        downloadDao.deleteAll()
    }

    private fun deleteDownloadFiles(download: DownloadEntity) {
        if (download.format.uppercase() in DOCUMENT_FORMATS) {
            // PDF/EPUB: single file at downloads/{serverId}/{seriesId}/chapter_{chapterId}.{ext}
            val ext = download.format.lowercase()
            val file = File(context.filesDir, "downloads/${download.serverId}/${download.seriesId}/chapter_${download.chapterId}.$ext")
            file.delete()
            // Also try filePath if stored
            download.filePath?.let { File(it).delete() }
        } else {
            // IMAGE: directory at downloads/{serverId}/{seriesId}/{chapterId}/
            val dir = File(context.filesDir, "downloads/${download.serverId}/${download.seriesId}/${download.chapterId}")
            dir.deleteRecursively()
        }
    }

    override suspend fun getDownloadedFile(chapterId: Int, serverId: Long): File? {
        val download = downloadDao.getByChapter(chapterId, serverId) ?: return null
        if (download.format.uppercase() in DOCUMENT_FORMATS) {
            // Check stored filePath first
            download.filePath?.let { path ->
                val file = File(path)
                if (file.exists()) return file
            }
            // Fallback to convention path
            val ext = download.format.lowercase()
            val file = File(context.filesDir, "downloads/$serverId/${download.seriesId}/chapter_$chapterId.$ext")
            return if (file.exists()) file else null
        }
        // IMAGE format: return directory
        val dir = File(context.filesDir, "downloads/$serverId/${download.seriesId}/$chapterId")
        return if (dir.exists() && dir.listFiles()?.isNotEmpty() == true) dir else null
    }

    override suspend fun isChapterDownloaded(chapterId: Int, serverId: Long): Boolean {
        val download = downloadDao.getByChapter(chapterId, serverId)
        return download?.status == DownloadStatus.COMPLETED.name
    }

    override suspend fun getStorageUsed(): Long {
        val downloadsDir = File(context.filesDir, "downloads")
        return if (downloadsDir.exists()) {
            downloadsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0L
    }

    override fun observeDownloadedSeriesIds(): Flow<Set<Int>> {
        return downloadDao.observeDownloadedSeriesIds().map { it.toSet() }
    }

    override fun observeDownloadedChapterIds(seriesId: Int, serverId: Long): Flow<Set<Int>> {
        return downloadDao.observeDownloadedChapterIds(seriesId, serverId).map { it.toSet() }
    }

    override fun observeDownloadedSeries(): Flow<List<DownloadedSeriesInfo>> {
        return downloadDao.observeCompleted().map { entities ->
            entities
                .groupBy { it.seriesId to it.serverId }
                .map { (key, downloads) ->
                    DownloadedSeriesInfo(
                        seriesId = key.first,
                        serverId = key.second,
                        seriesName = downloads.first().seriesName,
                        chapterCount = downloads.size,
                    )
                }
                .sortedBy { it.seriesName }
        }
    }

    override suspend fun exportToDownloads(chapterId: Int, serverId: Long, fileName: String): Boolean {
        val sourceFile = getDownloadedFile(chapterId, serverId) ?: return false
        if (!sourceFile.isFile) return false

        return withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    exportViaMediaStore(sourceFile, fileName)
                } else {
                    exportViaDirectAccess(sourceFile, fileName)
                }
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun exportViaMediaStore(sourceFile: File, fileName: String): Boolean {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/Kavita")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return false
        resolver.openOutputStream(uri)?.use { output ->
            sourceFile.inputStream().use { input -> input.copyTo(output) }
        } ?: run {
            resolver.delete(uri, null, null)
            return false
        }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return true
    }

    @Suppress("DEPRECATION")
    private fun exportViaDirectAccess(sourceFile: File, fileName: String): Boolean {
        val kavitaDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Kavita",
        )
        kavitaDir.mkdirs()
        val destFile = File(kavitaDir, fileName)
        sourceFile.inputStream().use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        }
        return destFile.exists()
    }

    override suspend fun downloadAndExport(
        chapterId: Int,
        seriesId: Int,
        serverId: Long,
        seriesName: String,
        chapterName: String,
        format: String,
        fileName: String,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Comprobar si ya esta descargado internamente
            var file = getDownloadedFile(chapterId, serverId)

            // 2. Si no, descargar directamente al almacenamiento interno
            if (file == null || !file.exists()) {
                val ext = format.lowercase()
                val dir = File(context.filesDir, "downloads/$serverId/$seriesId")
                dir.mkdirs()
                val destFile = File(dir, "chapter_$chapterId.$ext")
                val apiKey = activeServerProvider.getApiKey()?.let { "&apiKey=$it" } ?: ""
                val url = "${activeServerProvider.requireUrl()}/api/Download/chapter?chapterId=$chapterId$apiKey"
                val request = okhttp3.Request.Builder().url(url).build()
                val tempFile = File(dir, "chapter_${chapterId}_tmp")
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext false
                    val body = response.body ?: return@withContext false
                    tempFile.outputStream().use { output ->
                        body.byteStream().use { input -> input.copyTo(output) }
                    }
                }
                if (tempFile.length() < 1024) {
                    tempFile.delete()
                    return@withContext false
                }
                destFile.delete()
                if (!tempFile.renameTo(destFile)) {
                    tempFile.copyTo(destFile, overwrite = true)
                    tempFile.delete()
                }
                // Registrar descarga
                registerCompletedDownload(chapterId, seriesId, serverId, seriesName, chapterName, format, destFile.absolutePath)
                file = destFile
            }

            // 3. Exportar a carpeta publica Descargas/Kavita
            if (file.isFile) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    exportViaMediaStore(file, fileName)
                } else {
                    exportViaDirectAccess(file, fileName)
                }
            } else false
        } catch (_: Exception) {
            false
        }
    }

    private fun DownloadEntity.toDomain() = DownloadTask(
        id = id,
        chapterId = chapterId,
        seriesId = seriesId,
        serverId = serverId,
        seriesName = seriesName,
        chapterName = chapterName,
        status = try { DownloadStatus.valueOf(status) } catch (_: Exception) { DownloadStatus.PENDING },
        progress = progress,
        totalPages = totalPages,
        filePath = filePath,
        format = format,
    )
}
