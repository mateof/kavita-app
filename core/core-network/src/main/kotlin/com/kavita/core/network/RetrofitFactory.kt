package com.kavita.core.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrofitFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {

    private val cache = ConcurrentHashMap<String, Retrofit>()

    fun create(baseUrl: String): Retrofit {
        val normalizedUrl = baseUrl.trimEnd('/') + "/"
        return cache.getOrPut(normalizedUrl) {
            Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
        }
    }

    inline fun <reified T> createApi(baseUrl: String): T =
        create(baseUrl).create(T::class.java)

    fun clearCache() {
        cache.clear()
    }
}
