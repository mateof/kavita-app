package com.kavita.core.network.api

import com.kavita.core.network.dto.BookInfoDto
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface KavitaBookApi {

    @GET("api/Book/{chapterId}/book-info")
    suspend fun getBookInfo(@Path("chapterId") chapterId: Int): BookInfoDto

    @GET("api/Book/{chapterId}/book-page")
    suspend fun getBookPage(
        @Path("chapterId") chapterId: Int,
        @Query("page") page: Int,
    ): String

    @GET("api/Book/{chapterId}/book-resources")
    suspend fun getBookResources(
        @Path("chapterId") chapterId: Int,
        @Query("file") file: String,
    ): ResponseBody

    @GET("api/Book/{chapterId}/chapters")
    suspend fun getBookChapters(@Path("chapterId") chapterId: Int): List<BookInfoDto.BookChapterItem>
}
