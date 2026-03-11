package com.kavita.core.data.repository

import com.kavita.core.database.dao.EpubPageCountDao
import com.kavita.core.database.dao.ReadingProgressDao
import com.kavita.core.database.dao.SeriesDao
import com.kavita.core.database.entity.ReadingProgressEntity
import com.kavita.core.database.entity.SeriesEntity
import com.kavita.core.model.Chapter
import com.kavita.core.model.ContinueReadingItem
import com.kavita.core.model.MangaFormat
import com.kavita.core.model.RecentlyUpdatedSeries
import com.kavita.core.model.Series
import com.kavita.core.model.SeriesDetail
import com.kavita.core.model.SeriesMetadata
import com.kavita.core.model.Volume
import com.kavita.core.model.repository.PagedResult
import com.kavita.core.model.repository.SeriesRepository
import com.kavita.core.network.ActiveServerProvider
import com.kavita.core.network.RetrofitFactory
import com.kavita.core.network.api.KavitaReaderApi
import com.kavita.core.network.api.KavitaSeriesApi
import com.kavita.core.network.dto.FilterStatementDto
import com.kavita.core.network.dto.FilterV2Dto
import com.kavita.core.network.dto.MarkReadDto
import com.kavita.core.network.dto.PaginationInfo
import com.kavita.core.network.dto.SortOptionsDto
import com.kavita.core.network.dto.toDomain
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeriesRepositoryImpl @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
    private val activeServerProvider: ActiveServerProvider,
    private val seriesDao: SeriesDao,
    private val progressDao: ReadingProgressDao,
    private val epubPageCountDao: EpubPageCountDao,
    private val json: Json,
) : SeriesRepository {

    private fun seriesApi(): KavitaSeriesApi =
        retrofitFactory.createApi(activeServerProvider.requireUrl())

    private fun readerApi(): KavitaReaderApi =
        retrofitFactory.createApi(activeServerProvider.requireUrl())

    private fun serverId(): Long = activeServerProvider.requireId()

    private fun baseUrl(): String = activeServerProvider.requireUrl()

    private fun apiKey(): String? = activeServerProvider.getApiKey()

    override suspend fun getSeries(libraryId: Int?, page: Int, pageSize: Int): PagedResult<Series> {
        return try {
            val statements = if (libraryId != null) {
                listOf(FilterStatementDto(comparison = 0, field = 19, value = libraryId.toString()))
            } else {
                emptyList()
            }
            val filter = FilterV2Dto(
                id = 0, name = null, statements = statements,
                combination = 0,
                sortOptions = SortOptionsDto(sortField = 1, isAscending = true),
                limitTo = 0,
            )
            val response = seriesApi().getSeriesV2(filter, page, pageSize)
            val result = response.body() ?: emptyList()
            val pagination = parsePaginationHeader(response.headers()["Pagination"])
            val items = result.map { it.toDomain(serverId(), baseUrl(), apiKey()) }
            // Cache en Room
            seriesDao.upsertAll(items.map { it.toEntity(serverId()) })
            PagedResult(
                items = items,
                currentPage = pagination.currentPage,
                totalPages = pagination.totalPages,
                totalItems = pagination.totalItems,
            )
        } catch (e: Exception) {
            // Fallback: datos de Room
            val cached = if (libraryId != null) {
                seriesDao.getByLibrary(serverId(), libraryId)
            } else {
                seriesDao.getByServer(serverId())
            }
            if (cached.isNotEmpty()) {
                PagedResult(
                    items = cached.map { it.toDomain(baseUrl(), apiKey()) },
                    currentPage = 1, totalPages = 1, totalItems = cached.size,
                )
            } else throw e
        }
    }

    private fun parsePaginationHeader(header: String?): PaginationInfo {
        if (header.isNullOrBlank()) return PaginationInfo()
        return runCatching { json.decodeFromString<PaginationInfo>(header) }
            .getOrDefault(PaginationInfo())
    }

    override suspend fun getRecentlyAdded(page: Int, pageSize: Int): PagedResult<Series> {
        return try {
            val result = seriesApi().getRecentlyAdded(pageNumber = page, pageSize = pageSize)
            val items = result.map { it.toDomain(serverId(), baseUrl(), apiKey()) }
            seriesDao.upsertAll(items.map { it.toEntity(serverId()) })
            PagedResult(items = items, currentPage = page, totalPages = 1, totalItems = items.size)
        } catch (e: Exception) {
            val cached = seriesDao.getByServer(serverId())
            if (cached.isNotEmpty()) {
                PagedResult(
                    items = cached.sortedByDescending { it.lastUpdated }.take(pageSize).map { it.toDomain(baseUrl(), apiKey()) },
                    currentPage = 1, totalPages = 1, totalItems = cached.size,
                )
            } else throw e
        }
    }

    override suspend fun getRecentlyUpdated(pageSize: Int): List<RecentlyUpdatedSeries> =
        try {
            seriesApi().getRecentlyUpdated(
                pageNumber = 1, pageSize = pageSize,
            ).map { it.toDomain(serverId(), baseUrl(), apiKey()) }
        } catch (_: Exception) {
            emptyList()
        }

    override suspend fun getContinueReading(): List<ContinueReadingItem> {
        // Get recent local reading progress (chapter-level)
        val localProgress = progressDao.getRecentByServer(serverId(), limit = 20)
        if (localProgress.isEmpty()) return emptyList()

        // Look up series info for each progress entry
        val seriesIds = localProgress.map { it.seriesId }.distinct()
        val allSeries = seriesDao.getByServer(serverId())
        val seriesByRemoteId = allSeries.associateBy { it.remoteId }

        return localProgress.mapNotNull { progress ->
            buildContinueReadingItem(progress, seriesByRemoteId[progress.seriesId])
        }
    }

    private fun buildContinueReadingItem(
        progress: ReadingProgressEntity,
        series: SeriesEntity?,
    ): ContinueReadingItem? {
        val seriesName = series?.name ?: return null
        val format = series.format.let { f ->
            try { MangaFormat.valueOf(f) } catch (_: Exception) { MangaFormat.UNKNOWN }
        }
        val chapterCoverUrl = buildImageUrl(
            baseUrl(), "chapter-cover", "chapterId=${progress.chapterId}", apiKey(),
        )
        return ContinueReadingItem(
            chapterId = progress.chapterId,
            seriesId = progress.seriesId,
            volumeId = progress.volumeId,
            libraryId = progress.libraryId,
            serverId = progress.serverId,
            format = format,
            seriesName = seriesName,
            chapterTitle = "",
            coverUrl = chapterCoverUrl,
            pagesRead = progress.pageNum + 1,
            totalPages = series.pages,
            lastModified = progress.lastModified,
        )
    }

    override suspend fun getOnDeck(): List<ContinueReadingItem> {
        // Get on-deck series from server (the server knows which series are being read)
        val serverOnDeckSeries = try {
            val items = seriesApi().getOnDeck(0, 1, 20).map { it.toDomain(serverId(), baseUrl(), apiKey()) }
            seriesDao.upsertAll(items.map { it.toEntity(serverId()) })
            items
        } catch (_: Exception) {
            emptyList()
        }

        val allSeries = seriesDao.getByServer(serverId())
        val seriesByRemoteId = allSeries.associateBy { it.remoteId }
        val localProgress = progressDao.getRecentByServer(serverId(), limit = 30)
        val progressBySeriesId = localProgress.associateBy { it.seriesId }

        // Build items for server on-deck series, enriched with local chapter data
        val serverItems = serverOnDeckSeries.mapNotNull { series ->
            val progress = progressBySeriesId[series.id]
            if (progress != null) {
                buildContinueReadingItem(progress, seriesByRemoteId[series.id])
            } else {
                // No local progress: show series-level item (user read on another device)
                val entity = seriesByRemoteId[series.id] ?: return@mapNotNull null
                val format = entity.format.let { f ->
                    try { MangaFormat.valueOf(f) } catch (_: Exception) { MangaFormat.UNKNOWN }
                }
                ContinueReadingItem(
                    chapterId = 0,
                    seriesId = series.id,
                    volumeId = 0,
                    libraryId = series.libraryId,
                    serverId = serverId(),
                    format = format,
                    seriesName = series.name,
                    chapterTitle = "",
                    coverUrl = series.coverImage,
                    pagesRead = series.pagesRead,
                    totalPages = series.pages,
                    lastModified = 0,
                )
            }
        }

        // Add local-only entries not in server on-deck
        val serverSeriesIds = serverOnDeckSeries.map { it.id }.toSet()
        val localOnlyItems = localProgress
            .filter { it.seriesId !in serverSeriesIds }
            .mapNotNull { progress ->
                buildContinueReadingItem(progress, seriesByRemoteId[progress.seriesId])
            }

        val combined = serverItems + localOnlyItems
        return combined
            .distinctBy { "${it.serverId}-${it.chapterId}-${it.seriesId}" }
            .sortedByDescending { it.lastModified }
            .take(20)
            .ifEmpty {
                // Fallback: cached series with partial progress
                allSeries
                    .filter { it.pagesRead > 0 && it.pagesRead < it.pages }
                    .take(20)
                    .mapNotNull { entity ->
                        val progress = progressBySeriesId[entity.remoteId]
                        if (progress != null) {
                            buildContinueReadingItem(progress, entity)
                        } else null
                    }
            }
    }

    override suspend fun getSeriesDetail(seriesId: Int): Series {
        return try {
            val dto = seriesApi().getSeries(seriesId)
            val series = dto.toDomain(serverId(), baseUrl(), apiKey())
            seriesDao.upsert(series.toEntity(serverId()))
            series
        } catch (e: Exception) {
            val cached = seriesDao.getByRemoteId(seriesId, serverId())
            cached?.toDomain(baseUrl(), apiKey()) ?: throw e
        }
    }

    override suspend fun getSeriesDetailInfo(seriesId: Int): SeriesDetail =
        seriesApi().getSeriesDetail(seriesId).toDomain(serverId(), seriesId, baseUrl(), apiKey())

    override suspend fun getVolumes(seriesId: Int): List<Volume> =
        seriesApi().getVolumes(seriesId).map { it.toDomain(serverId(), seriesId, baseUrl(), apiKey()) }

    override suspend fun getChapter(chapterId: Int): Chapter =
        seriesApi().getChapter(chapterId).toDomain(serverId(), 0, baseUrl(), apiKey())

    override suspend fun searchSeries(query: String, page: Int, pageSize: Int): PagedResult<Series> {
        return try {
            // 1. Buscar series por nombre
            val filter = FilterV2Dto(
                id = 0, name = null,
                statements = listOf(FilterStatementDto(comparison = 7, field = 1, value = query)),
                combination = 0, sortOptions = null, limitTo = 0,
            )
            val response = seriesApi().getSeriesV2(filter, page, pageSize)
            val nameResults = (response.body() ?: emptyList()).map { it.toDomain(serverId(), baseUrl(), apiKey()) }
            val pagination = parsePaginationHeader(response.headers()["Pagination"])

            // 2. Buscar personas (autores) y obtener sus series
            val personSeries = searchByPerson(query)

            // 3. Combinar resultados sin duplicados
            val nameIds = nameResults.map { it.id }.toSet()
            val combined = nameResults + personSeries.filter { it.id !in nameIds }.distinctBy { it.id }

            PagedResult(
                items = combined,
                currentPage = pagination.currentPage,
                totalPages = pagination.totalPages,
                totalItems = pagination.totalItems + personSeries.count { it.id !in nameIds },
            )
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (e: Exception) {
            val cached = seriesDao.getByServer(serverId())
                .filter { it.name.contains(query, ignoreCase = true) }
            if (cached.isNotEmpty()) {
                PagedResult(
                    items = cached.map { it.toDomain(baseUrl(), apiKey()) },
                    currentPage = 1, totalPages = 1, totalItems = cached.size,
                )
            } else throw e
        }
    }

    private suspend fun searchByPerson(query: String): List<Series> = try {
        val persons = seriesApi().searchPersons(query)
        persons.flatMap { person ->
            try {
                seriesApi().getSeriesByPerson(person.id)
                    .map { it.toDomain(serverId(), baseUrl(), apiKey()) }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) { throw e }
            catch (_: Exception) { emptyList() }
        }
    } catch (e: kotlin.coroutines.cancellation.CancellationException) { throw e }
    catch (_: Exception) { emptyList() }

    override suspend fun getSeriesMetadata(seriesId: Int): SeriesMetadata =
        seriesApi().getSeriesMetadata(seriesId).toDomain()

    override suspend fun markSeriesAsRead(seriesId: Int) {
        readerApi().markRead(MarkReadDto(seriesId = seriesId))
    }

    override suspend fun markSeriesAsUnread(seriesId: Int) {
        readerApi().markUnread(MarkReadDto(seriesId = seriesId))
    }

    // Cache en memoria + Room para paginas reales de EPUB (Readium)
    private val epubPageCountCache = java.util.concurrent.ConcurrentHashMap<Int, Int>()

    override fun setEpubPageCount(chapterId: Int, totalPages: Int) {
        epubPageCountCache[chapterId] = totalPages
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                epubPageCountDao.upsert(
                    com.kavita.core.database.entity.EpubPageCountEntity(chapterId, totalPages),
                )
            } catch (_: Exception) { }
        }
    }

    override fun getEpubPageCount(chapterId: Int): Int? {
        epubPageCountCache[chapterId]?.let { return it }
        // Fallback: leer de Room (bloqueante, pero se llama desde coroutine)
        return try {
            epubPageCountDao.getByChapterId(chapterId)?.also {
                epubPageCountCache[chapterId] = it
            }
        } catch (_: Exception) { null }
    }
}

// --- Extension functions for entity mapping ---

private fun Series.toEntity(serverId: Long) = SeriesEntity(
    id = "${id}_${serverId}".hashCode().toLong(),
    remoteId = id,
    serverId = serverId,
    name = name,
    sortName = sortName,
    localizedName = localizedName,
    originalName = originalName,
    summary = summary,
    format = format.name,
    libraryId = libraryId,
    pagesRead = pagesRead,
    pages = pages,
    userRating = userRating,
    lastUpdated = System.currentTimeMillis(),
)

private fun SeriesEntity.toDomain(baseUrl: String, apiKey: String?) = Series(
    id = remoteId,
    name = name,
    sortName = sortName,
    localizedName = localizedName,
    originalName = originalName,
    coverImage = buildImageUrl(baseUrl, "series-cover", "seriesId=$remoteId", apiKey),
    summary = summary,
    format = try { MangaFormat.valueOf(format) } catch (_: Exception) { MangaFormat.UNKNOWN },
    libraryId = libraryId,
    pagesRead = pagesRead,
    pages = pages,
    userRating = userRating,
    serverId = serverId,
)

private fun buildImageUrl(baseUrl: String, endpoint: String, params: String, apiKey: String?): String {
    val keyParam = if (apiKey != null) "&apiKey=$apiKey" else ""
    return "$baseUrl/api/Image/$endpoint?$params$keyParam"
}
