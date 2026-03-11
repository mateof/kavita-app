package com.kavita.core.network.api

import com.kavita.core.network.dto.ReadHistoryEventDto
import com.kavita.core.network.dto.ReadingSessionDto
import com.kavita.core.network.dto.StatCountDto
import retrofit2.http.GET
import retrofit2.http.Query

interface KavitaStatsApi {

    @GET("api/Stats/user/reading-history")
    suspend fun getReadingHistory(@Query("userId") userId: Int): List<ReadHistoryEventDto>

    @GET("api/Stats/reading-count-by-day")
    suspend fun getReadingCountByDay(
        @Query("userId") userId: Int = 0,
        @Query("days") days: Int = 30,
    ): List<StatCountDto>

    @GET("api/Stats/server/stats")
    suspend fun getServerStats(): Map<String, Any>

    @GET("api/Stats/server/top/users")
    suspend fun getTopUsers(@Query("days") days: Int = 30): List<ReadingSessionDto>

    @GET("api/Stats/user/pages-per-year")
    suspend fun getPagesPerYear(@Query("userId") userId: Int = 0): List<StatCountDto>

    @GET("api/Stats/user/words-per-year")
    suspend fun getWordsPerYear(@Query("userId") userId: Int = 0): List<StatCountDto>
}
