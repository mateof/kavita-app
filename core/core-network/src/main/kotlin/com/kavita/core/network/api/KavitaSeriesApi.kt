package com.kavita.core.network.api

import com.kavita.core.network.dto.ChapterDto
import com.kavita.core.network.dto.FilterV2Dto
import com.kavita.core.network.dto.GroupedSeriesDto
import com.kavita.core.network.dto.PersonDto
import com.kavita.core.network.dto.SeriesDetailDto
import com.kavita.core.network.dto.SeriesDto
import com.kavita.core.network.dto.SeriesMetadataDto
import com.kavita.core.network.dto.VolumeDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface KavitaSeriesApi {

    @POST("api/Series/v2")
    suspend fun getSeriesV2(
        @Body filter: FilterV2Dto,
        @Query("pageNumber") pageNumber: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): Response<List<SeriesDto>>

    @GET("api/Series/{seriesId}")
    suspend fun getSeries(@Path("seriesId") seriesId: Int): SeriesDto

    @GET("api/Series/series-detail")
    suspend fun getSeriesDetail(@Query("seriesId") seriesId: Int): SeriesDetailDto

    @GET("api/Series/volumes")
    suspend fun getVolumes(@Query("seriesId") seriesId: Int): List<VolumeDto>

    @GET("api/Series/chapter")
    suspend fun getChapter(@Query("chapterId") chapterId: Int): ChapterDto

    @GET("api/Series/metadata")
    suspend fun getSeriesMetadata(@Query("seriesId") seriesId: Int): SeriesMetadataDto

    @POST("api/Series/recently-added-v2")
    suspend fun getRecentlyAdded(
        @Body filter: FilterV2Dto = FilterV2Dto(),
        @Query("pageNumber") pageNumber: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): List<SeriesDto>

    @POST("api/Series/recently-updated-series")
    suspend fun getRecentlyUpdated(
        @Query("pageNumber") pageNumber: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): List<GroupedSeriesDto>

    @POST("api/Series/on-deck")
    suspend fun getOnDeck(
        @Query("libraryId") libraryId: Int = 0,
        @Query("pageNumber") pageNumber: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): List<SeriesDto>

    @GET("api/Series/currently-reading")
    suspend fun getContinueReading(
        @Query("pageNumber") pageNumber: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): List<SeriesDto>

    @GET("api/Series/series-by-collection")
    suspend fun getSeriesByCollection(
        @Query("collectionId") collectionId: Int,
        @Query("pageNumber") pageNumber: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): List<SeriesDto>

    @GET("api/Person/search")
    suspend fun searchPersons(@Query("queryString") query: String): List<PersonDto>

    @GET("api/Person/series-known-for")
    suspend fun getSeriesByPerson(@Query("personId") personId: Int): List<SeriesDto>
}
