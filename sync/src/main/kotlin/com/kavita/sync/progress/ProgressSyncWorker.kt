package com.kavita.sync.progress

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kavita.core.database.dao.ReadingProgressDao
import com.kavita.core.network.ActiveServerProvider
import com.kavita.core.network.RetrofitFactory
import com.kavita.core.network.api.KavitaReaderApi
import com.kavita.core.network.dto.SaveProgressDto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ProgressSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val readingProgressDao: ReadingProgressDao,
    private val retrofitFactory: RetrofitFactory,
    private val activeServerProvider: ActiveServerProvider,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val url = activeServerProvider.requireUrl()
            val api = retrofitFactory.createApi<KavitaReaderApi>(url)
            val serverId = activeServerProvider.requireId()

            val unsyncedProgress = readingProgressDao.getUnsynced()
            for (progress in unsyncedProgress) {
                try {
                    // Consultar el estado del servidor para comparar timestamps
                    val serverProgress = try {
                        api.getProgress(progress.chapterId)
                    } catch (_: Exception) {
                        null
                    }

                    val serverTimestamp = serverProgress?.lastModifiedUtc?.let { utc ->
                        if (utc.isNotBlank()) {
                            runCatching {
                                java.time.Instant.parse(utc).toEpochMilli()
                            }.getOrNull()
                        } else null
                    } ?: 0L

                    if (progress.lastModified >= serverTimestamp) {
                        // Local es mas reciente o igual: enviar al servidor
                        val response = api.saveProgress(
                            SaveProgressDto(
                                chapterId = progress.chapterId,
                                pageNum = progress.pageNum,
                                seriesId = progress.seriesId,
                                volumeId = progress.volumeId,
                                libraryId = progress.libraryId,
                                bookScrollId = progress.bookScrollId,
                            )
                        )
                        if (!response.isSuccessful) continue
                    }
                    // En ambos casos marcar como sincronizado
                    readingProgressDao.markSynced(progress.chapterId, progress.serverId)
                } catch (_: Exception) {
                    // Reintentar en la siguiente ejecucion
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
