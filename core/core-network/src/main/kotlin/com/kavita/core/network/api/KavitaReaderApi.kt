package com.kavita.core.network.api

import com.kavita.core.network.dto.BookmarkDto
import com.kavita.core.network.dto.ChapterInfoDto
import com.kavita.core.network.dto.HourEstimateDto
import com.kavita.core.network.dto.MarkReadDto
import com.kavita.core.network.dto.ProgressDto
import com.kavita.core.network.dto.SaveProgressDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface KavitaReaderApi {

    @GET("api/Reader/image")
    suspend fun getPageImage(
        @Query("chapterId") chapterId: Int,
        @Query("page") page: Int,
    ): ResponseBody

    @GET("api/Reader/chapter-info")
    suspend fun getChapterInfo(@Query("chapterId") chapterId: Int): ChapterInfoDto

    @GET("api/Reader/get-progress")
    suspend fun getProgress(@Query("chapterId") chapterId: Int): ProgressDto

    @POST("api/Reader/progress")
    suspend fun saveProgress(@Body progress: SaveProgressDto): Response<Unit>

    @GET("api/Reader/get-bookmarks")
    suspend fun getBookmarks(@Query("chapterId") chapterId: Int): List<BookmarkDto>

    @POST("api/Reader/bookmark")
    suspend fun createBookmark(@Body bookmark: BookmarkDto): Response<Unit>

    @POST("api/Reader/unbookmark")
    suspend fun removeBookmark(@Body bookmark: BookmarkDto): Response<Unit>

    @GET("api/Reader/next-chapter")
    suspend fun getNextChapter(
        @Query("seriesId") seriesId: Int,
        @Query("volumeId") volumeId: Int,
        @Query("currentChapterId") currentChapterId: Int,
    ): Int

    @GET("api/Reader/prev-chapter")
    suspend fun getPrevChapter(
        @Query("seriesId") seriesId: Int,
        @Query("volumeId") volumeId: Int,
        @Query("currentChapterId") currentChapterId: Int,
    ): Int

    @POST("api/Reader/mark-read")
    suspend fun markRead(@Body dto: MarkReadDto): Response<Unit>

    @POST("api/Reader/mark-unread")
    suspend fun markUnread(@Body dto: MarkReadDto): Response<Unit>

    @GET("api/Reader/time-left")
    suspend fun getTimeLeft(
        @Query("seriesId") seriesId: Int,
    ): HourEstimateDto

    @GET("api/Reader/continue-point")
    suspend fun getContinuePoint(@Query("seriesId") seriesId: Int): ChapterInfoDto
}
