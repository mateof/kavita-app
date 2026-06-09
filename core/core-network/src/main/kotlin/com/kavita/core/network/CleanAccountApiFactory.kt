package com.kavita.core.network

import com.kavita.core.network.api.KavitaAccountApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Crea instancias de [KavitaAccountApi] sobre un OkHttpClient **limpio**, sin el
 * AuthInterceptor ni el SessionAuthenticator. Se usa para las llamadas de autenticación
 * (login, refresh-token, plugin/authenticate) para evitar que el propio Authenticator
 * intente renovar la sesión recursivamente cuando esas llamadas devuelven 401.
 */
@Singleton
class CleanAccountApiFactory @Inject constructor(
    private val json: Json,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val cache = ConcurrentHashMap<String, KavitaAccountApi>()

    fun create(baseUrl: String): KavitaAccountApi {
        val normalized = baseUrl.trimEnd('/') + "/"
        return cache.getOrPut(normalized) {
            Retrofit.Builder()
                .baseUrl(normalized)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(KavitaAccountApi::class.java)
        }
    }
}
