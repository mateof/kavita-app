package com.kavita.core.network.api

import com.kavita.core.network.dto.CollectionTagBulkAddDto
import com.kavita.core.network.dto.CollectionTagDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface KavitaCollectionApi {
    @GET("api/Collection")
    suspend fun getCollections(): List<CollectionTagDto>

    /** Si collectionTagId == 0, crea una coleccion nueva con collectionTagTitle */
    @POST("api/Collection/update-for-series")
    suspend fun addSeriesToCollection(@Body body: CollectionTagBulkAddDto)
}
