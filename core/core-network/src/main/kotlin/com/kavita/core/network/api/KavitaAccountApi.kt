package com.kavita.core.network.api

import com.kavita.core.network.dto.LoginRequest
import com.kavita.core.network.dto.LoginResponse
import com.kavita.core.network.dto.RefreshTokenRequest
import com.kavita.core.network.dto.TokenResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface KavitaAccountApi {
    @POST("api/Account/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/Account/refresh-token")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): TokenResponse

    /**
     * Autenticación mediante apiKey (no caduca). Devuelve un JWT nuevo.
     * Se usa para recuperar la sesión cuando el refresh-token ha caducado.
     */
    @POST("api/Plugin/authenticate")
    suspend fun pluginAuthenticate(
        @Query("apiKey") apiKey: String,
        @Query("pluginName") pluginName: String,
    ): LoginResponse

    @GET("api/Account/opds-url")
    suspend fun getOpdsUrl(): String

    @GET("api/Account")
    suspend fun getCurrentUser(): LoginResponse
}
