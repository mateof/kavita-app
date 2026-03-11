@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.kavita.core.network.dto

import com.kavita.core.model.AgeRating
import com.kavita.core.model.Chapter
import com.kavita.core.model.Collection
import com.kavita.core.model.Library
import com.kavita.core.model.LibraryType
import com.kavita.core.model.MangaFormat
import com.kavita.core.model.PublicationStatus
import com.kavita.core.model.ReadingList
import com.kavita.core.model.ReadingListItem
import com.kavita.core.model.RecentlyUpdatedSeries
import com.kavita.core.model.Series
import com.kavita.core.model.SeriesDetail
import com.kavita.core.model.SeriesMetadata
import com.kavita.core.model.Volume
import com.kavita.core.model.repository.UserInfo
import kotlinx.datetime.Instant

fun SeriesDto.toDomain(serverId: Long, baseUrl: String, apiKey: String? = null): Series = Series(
    id = id,
    name = name,
    sortName = sortName,
    localizedName = localizedName,
    originalName = originalName,
    coverImage = buildImageUrl(baseUrl, "series-cover", "seriesId=$id", apiKey),
    summary = summary,
    format = MangaFormat.entries.getOrElse(format) { MangaFormat.UNKNOWN },
    libraryId = libraryId,
    pagesRead = pagesRead,
    pages = pages,
    userRating = userRating,
    serverId = serverId,
)

fun VolumeDto.toDomain(serverId: Long, seriesId: Int, baseUrl: String, apiKey: String? = null): Volume = Volume(
    id = id,
    number = number,
    name = name,
    pages = pages,
    pagesRead = pagesRead,
    coverImage = buildImageUrl(baseUrl, "volume-cover", "volumeId=$id", apiKey),
    chapters = chapters.map { it.toDomain(serverId, seriesId, baseUrl, apiKey) },
    seriesId = seriesId,
    serverId = serverId,
)

fun ChapterDto.toDomain(serverId: Long, seriesId: Int, baseUrl: String, apiKey: String? = null): Chapter = Chapter(
    id = id,
    range = range,
    number = number,
    title = title,
    pages = pages,
    pagesRead = pagesRead,
    coverImage = buildImageUrl(baseUrl, "chapter-cover", "chapterId=$id", apiKey),
    isSpecial = isSpecial,
    volumeId = volumeId,
    seriesId = seriesId,
    serverId = serverId,
    releaseDate = try { Instant.parse(releaseDate) } catch (_: Exception) { null },
)

fun LibraryDto.toDomain(serverId: Long, baseUrl: String, apiKey: String? = null): Library = Library(
    id = id,
    name = name,
    type = LibraryType.entries.getOrElse(type) { LibraryType.MANGA },
    coverImage = coverImage?.let { buildImageUrl(baseUrl, "library-cover", "libraryId=$id", apiKey) },
    lastScanned = try { Instant.parse(lastScanned) } catch (_: Exception) { null },
    folders = folders,
    serverId = serverId,
)

fun GroupedSeriesDto.toDomain(serverId: Long, baseUrl: String, apiKey: String? = null): RecentlyUpdatedSeries = RecentlyUpdatedSeries(
    seriesId = seriesId,
    seriesName = seriesName,
    coverImage = buildImageUrl(baseUrl, "series-cover", "seriesId=$seriesId", apiKey),
    libraryId = libraryId,
    format = MangaFormat.entries.getOrElse(format) { MangaFormat.UNKNOWN },
    count = count,
    serverId = serverId,
)

fun SeriesDetailDto.toDomain(serverId: Long, seriesId: Int, baseUrl: String, apiKey: String? = null): SeriesDetail = SeriesDetail(
    specials = specials.map { it.toDomain(serverId, seriesId, baseUrl, apiKey) },
    chapters = chapters.map { it.toDomain(serverId, seriesId, baseUrl, apiKey) },
    volumes = volumes.map { it.toDomain(serverId, seriesId, baseUrl, apiKey) },
    storylineChapters = storylineChapters.map { it.toDomain(serverId, seriesId, baseUrl, apiKey) },
    totalCount = totalCount,
    unreadCount = unreadCount,
)

fun SeriesMetadataDto.toDomain(): SeriesMetadata = SeriesMetadata(
    summary = summary,
    genres = genres.map { it.title },
    tags = tags.map { it.title },
    writers = writers.map { it.name },
    coverArtists = coverArtists.map { it.name },
    publishers = publishers.map { it.name },
    pencillers = pencillers.map { it.name },
    colorists = colorists.map { it.name },
    letterers = letterers.map { it.name },
    editors = editors.map { it.name },
    translators = translators.map { it.name },
    characters = characters.map { it.name },
    ageRating = AgeRating.entries.getOrElse(ageRating) { AgeRating.UNKNOWN },
    releaseYear = releaseYear,
    language = language,
    publicationStatus = PublicationStatus.entries.getOrElse(publicationStatus) { PublicationStatus.ONGOING },
    totalCount = totalCount,
    maxCount = maxCount,
    webLinks = webLinks,
)

fun UserDto.toDomain() = UserInfo(
    id = id,
    username = username,
    email = email,
    roles = roles,
    lastActiveUtc = lastActiveUtc,
)

fun CollectionTagDto.toDomain(baseUrl: String, apiKey: String?, serverId: Long) = Collection(
    id = id,
    title = title,
    summary = summary,
    coverImage = buildImageUrl(baseUrl, "collection-cover", "collectionTagId=$id", apiKey),
    promoted = promoted,
    serverId = serverId,
)

fun ReadingListDto.toDomain(baseUrl: String, apiKey: String?, serverId: Long) = ReadingList(
    id = id,
    title = title,
    summary = summary,
    coverImage = buildImageUrl(baseUrl, "reading-list-cover", "readingListId=$id", apiKey),
    promoted = promoted,
    itemCount = items.size,
    serverId = serverId,
)

fun ReadingListItemDto.toDomain(baseUrl: String, apiKey: String?) = ReadingListItem(
    id = id,
    seriesId = seriesId,
    seriesName = seriesName,
    chapterId = chapterId,
    chapterNumber = chapterNumber,
    order = order,
    coverImage = buildImageUrl(baseUrl, "series-cover", "seriesId=$seriesId", apiKey),
)

private fun buildImageUrl(baseUrl: String, endpoint: String, params: String, apiKey: String?): String {
    val keyParam = if (apiKey != null) "&apiKey=$apiKey" else ""
    return "$baseUrl/api/Image/$endpoint?$params$keyParam"
}
