package com.kavita.core.model.repository

import com.kavita.core.model.AppUpdate
import java.io.File

interface AppUpdateRepository {
    /** Obtiene la última release publicada en GitHub, o null si falla la consulta. */
    suspend fun getLatestRelease(): AppUpdate?

    /**
     * Descarga el APK de [update] a un fichero local, informando del progreso
     * (bytes descargados, bytes totales). Devuelve el fichero descargado.
     */
    suspend fun downloadApk(
        update: AppUpdate,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ): File
}
