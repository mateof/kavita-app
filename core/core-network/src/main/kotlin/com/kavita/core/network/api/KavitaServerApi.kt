package com.kavita.core.network.api

import com.kavita.core.network.dto.ServerInfoDto
import retrofit2.http.GET
import retrofit2.http.POST

interface KavitaServerApi {

    @GET("api/Server/server-info")
    suspend fun getServerInfo(): ServerInfoDto

    @GET("api/health")
    suspend fun health(): String

    @POST("api/Server/clear-cache")
    suspend fun clearCache()

    @POST("api/Server/backup-db")
    suspend fun backupDatabase()

    @GET("api/Server/check-update")
    suspend fun checkForUpdate(): Map<String, Any>

    @GET("api/Server/changelog")
    suspend fun getChangelog(): List<Map<String, Any>>
}
