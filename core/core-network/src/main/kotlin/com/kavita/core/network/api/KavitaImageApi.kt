package com.kavita.core.network.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface KavitaImageApi {

    @GET("api/Image/series-cover")
    suspend fun getSeriesCover(@Query("seriesId") seriesId: Int): ResponseBody

    @GET("api/Image/volume-cover")
    suspend fun getVolumeCover(@Query("volumeId") volumeId: Int): ResponseBody

    @GET("api/Image/chapter-cover")
    suspend fun getChapterCover(@Query("chapterId") chapterId: Int): ResponseBody

    @GET("api/Image/library-cover")
    suspend fun getLibraryCover(@Query("libraryId") libraryId: Int): ResponseBody

    @GET("api/Image/collection-cover")
    suspend fun getCollectionCover(@Query("collectionTagId") collectionTagId: Int): ResponseBody

    @GET("api/Image/reading-list-cover")
    suspend fun getReadingListCover(@Query("readingListId") readingListId: Int): ResponseBody

    companion object {
        fun seriesCoverUrl(baseUrl: String, seriesId: Int): String =
            "${baseUrl.trimEnd('/')}/api/Image/series-cover?seriesId=$seriesId"

        fun volumeCoverUrl(baseUrl: String, volumeId: Int): String =
            "${baseUrl.trimEnd('/')}/api/Image/volume-cover?volumeId=$volumeId"

        fun chapterCoverUrl(baseUrl: String, chapterId: Int): String =
            "${baseUrl.trimEnd('/')}/api/Image/chapter-cover?chapterId=$chapterId"

        fun libraryCoverUrl(baseUrl: String, libraryId: Int): String =
            "${baseUrl.trimEnd('/')}/api/Image/library-cover?libraryId=$libraryId"
    }
}
