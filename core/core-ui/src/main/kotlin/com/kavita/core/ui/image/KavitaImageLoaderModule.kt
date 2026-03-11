package com.kavita.core.ui.image

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object KavitaImageLoaderModule {

    /**
     * Interceptor de red que añade cabeceras Cache-Control a las respuestas de imágenes
     * cuando el servidor no incluye una. Esto permite que OkHttp cachee las imágenes
     * durante 24 horas en la caché HTTP.
     */
    private class CacheInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val response = chain.proceed(chain.request())
            val cacheControl = response.header("Cache-Control")
            // Si el servidor no incluye Cache-Control, añadimos uno de 24 horas
            return if (cacheControl.isNullOrBlank() || cacheControl.contains("no-store")) {
                val newCacheControl = CacheControl.Builder()
                    .maxAge(86400, TimeUnit.SECONDS) // 24 horas
                    .build()
                response.newBuilder()
                    .removeHeader("Pragma")
                    .removeHeader("Cache-Control")
                    .header("Cache-Control", newCacheControl.toString())
                    .build()
            } else {
                response
            }
        }
    }

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
    ): ImageLoader {
        // Crear un OkHttpClient derivado del existente con caché HTTP para imágenes
        val imageCacheDir = context.cacheDir.resolve("http_image_cache")
        val imageOkHttpClient = okHttpClient.newBuilder()
            .cache(Cache(imageCacheDir, 10L * 1024 * 1024)) // 10 MB de caché HTTP
            .addNetworkInterceptor(CacheInterceptor())
            .build()

        return ImageLoader.Builder(context)
            .okHttpClient(imageOkHttpClient)
            .crossfade(true)
            .respectCacheHeaders(true)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    // Usar filesDir en vez de cacheDir para que no se borre automaticamente
                    .directory(context.filesDir.resolve("image_cache"))
                    .maxSizeBytes(500L * 1024 * 1024) // 500 MB
                    .build()
            }
            .build()
    }
}
