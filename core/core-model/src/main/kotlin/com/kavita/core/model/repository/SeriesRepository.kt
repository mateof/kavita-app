package com.kavita.core.model.repository

import com.kavita.core.model.Chapter
import com.kavita.core.model.ContinueReadingItem
import com.kavita.core.model.RecentlyUpdatedSeries
import com.kavita.core.model.Series
import com.kavita.core.model.SeriesDetail
import com.kavita.core.model.SeriesMetadata
import com.kavita.core.model.Volume

interface SeriesRepository {
    suspend fun getSeries(
        libraryId: Int? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): PagedResult<Series>

    suspend fun getRecentlyAdded(page: Int = 1, pageSize: Int = 20): PagedResult<Series>
    suspend fun getRecentlyUpdated(pageSize: Int = 20): List<RecentlyUpdatedSeries>
    suspend fun getContinueReading(): List<ContinueReadingItem>
    suspend fun getOnDeck(): List<ContinueReadingItem>
    suspend fun getSeriesDetail(seriesId: Int): Series
    suspend fun getSeriesDetailInfo(seriesId: Int): SeriesDetail
    suspend fun getVolumes(seriesId: Int): List<Volume>
    suspend fun getChapter(chapterId: Int): Chapter
    suspend fun searchSeries(query: String, page: Int = 1, pageSize: Int = 20): PagedResult<Series>
    suspend fun getSeriesMetadata(seriesId: Int): SeriesMetadata
    suspend fun markSeriesAsRead(seriesId: Int)
    suspend fun markSeriesAsUnread(seriesId: Int)

    /** Guarda el total real de paginas de un capitulo EPUB (calculado por Readium). */
    fun setEpubPageCount(chapterId: Int, totalPages: Int)

    /** Obtiene el total real de paginas de un capitulo EPUB, o null si no se ha resuelto. */
    fun getEpubPageCount(chapterId: Int): Int?
}

data class PagedResult<T>(
    val items: List<T>,
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Int,
)
