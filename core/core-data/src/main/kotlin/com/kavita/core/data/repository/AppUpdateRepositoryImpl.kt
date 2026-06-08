package com.kavita.core.data.repository

import android.content.Context
import com.kavita.core.model.AppUpdate
import com.kavita.core.model.repository.AppUpdateRepository
import com.kavita.core.network.api.GitHubApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateRepositoryImpl @Inject constructor(
    private val gitHubApi: GitHubApi,
    @ApplicationContext private val context: Context,
) : AppUpdateRepository {

    override suspend fun getLatestRelease(): AppUpdate? = withContext(Dispatchers.IO) {
        try {
            val release = gitHubApi.getLatestRelease(OWNER, REPO)
            if (release.draft) return@withContext null
            val apk = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            AppUpdate(
                versionName = release.tag_name.removePrefix("v").trim(),
                releaseUrl = release.html_url,
                apkUrl = apk?.browser_download_url,
                apkSizeBytes = apk?.size ?: 0L,
                notes = release.body,
            )
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun downloadApk(
        update: AppUpdate,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val url = update.apkUrl ?: throw IllegalStateException("La release no incluye APK")
        val dir = File(context.cacheDir, UPDATES_DIR).apply { mkdirs() }
        // Limpiar descargas previas para no acumular APKs
        dir.listFiles()?.forEach { it.delete() }
        val outFile = File(dir, "kavita-${update.versionName}.apk")

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Error de descarga: HTTP ${response.code}")
            }
            val body = response.body ?: throw IllegalStateException("Respuesta vacía")
            val total = if (body.contentLength() > 0) body.contentLength() else update.apkSizeBytes
            body.byteStream().use { input ->
                outFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                    output.flush()
                }
            }
        }
        outFile
    }

    private companion object {
        const val OWNER = "mateof"
        const val REPO = "kavita-app"
        const val UPDATES_DIR = "updates"
    }
}
