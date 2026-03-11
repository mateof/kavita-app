package com.kavita.core.network.api

import com.kavita.core.network.dto.LibraryDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface KavitaLibraryApi {

    @GET("api/Library/libraries")
    suspend fun getLibraries(): List<LibraryDto>

    @GET("api/Library/{libraryId}")
    suspend fun getLibrary(@Path("libraryId") libraryId: Int): LibraryDto

    @POST("api/Library/scan")
    suspend fun scanLibrary(@Query("libraryId") libraryId: Int, @Query("force") force: Boolean = false)

    @POST("api/Library/refresh-metadata")
    suspend fun refreshMetadata(@Query("libraryId") libraryId: Int, @Query("force") force: Boolean = false)

    @DELETE("api/Library/{libraryId}")
    suspend fun deleteLibrary(@Path("libraryId") libraryId: Int)

    @GET("api/Library/type")
    suspend fun getLibraryType(@Query("libraryId") libraryId: Int): Int
}
