package com.kavita.core.network.api

import com.kavita.core.network.dto.CreateReadingListRequest
import com.kavita.core.network.dto.ReadingListDto
import com.kavita.core.network.dto.ReadingListItemDto
import com.kavita.core.network.dto.UpdateReadingListByMultipleSeriesDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface KavitaReadingListApi {
    @POST("api/ReadingList/lists")
    suspend fun getReadingLists(): List<ReadingListDto>

    @GET("api/ReadingList")
    suspend fun getReadingList(@Query("readingListId") readingListId: Int): ReadingListDto

    @GET("api/ReadingList/items")
    suspend fun getReadingListItems(@Query("readingListId") readingListId: Int): List<ReadingListItemDto>

    @POST("api/ReadingList/create")
    suspend fun createReadingList(@Body body: CreateReadingListRequest): ReadingListDto

    @POST("api/ReadingList/update-by-multiple-series")
    suspend fun addSeriesToReadingList(@Body body: UpdateReadingListByMultipleSeriesDto)
}
