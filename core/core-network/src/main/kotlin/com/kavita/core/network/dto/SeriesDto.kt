package com.kavita.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class SeriesDto(
    val id: Int = 0,
    val name: String = "",
    val sortName: String = "",
    val localizedName: String = "",
    val originalName: String = "",
    val coverImage: String = "",
    val summary: String = "",
    val format: Int = 0,
    val libraryId: Int = 0,
    val libraryName: String = "",
    val pagesRead: Int = 0,
    val pages: Int = 0,
    val userRating: Float = 0f,
    val created: String = "",
    val lastChapterAdded: String = "",
    val wordCount: Long = 0,
)

@Serializable
data class SeriesDetailDto(
    val specials: List<ChapterDto> = emptyList(),
    val chapters: List<ChapterDto> = emptyList(),
    val volumes: List<VolumeDto> = emptyList(),
    val storylineChapters: List<ChapterDto> = emptyList(),
    val totalCount: Int = 0,
    val unreadCount: Int = 0,
)

@Serializable
data class SeriesMetadataDto(
    val id: Int = 0,
    val seriesId: Int = 0,
    val summary: String = "",
    val genres: List<GenreDto> = emptyList(),
    val tags: List<TagDto> = emptyList(),
    val writers: List<PersonDto> = emptyList(),
    val coverArtists: List<PersonDto> = emptyList(),
    val publishers: List<PersonDto> = emptyList(),
    val characters: List<PersonDto> = emptyList(),
    val pencillers: List<PersonDto> = emptyList(),
    val inkers: List<PersonDto> = emptyList(),
    val colorists: List<PersonDto> = emptyList(),
    val letterers: List<PersonDto> = emptyList(),
    val editors: List<PersonDto> = emptyList(),
    val translators: List<PersonDto> = emptyList(),
    val ageRating: Int = 0,
    val releaseYear: Int = 0,
    val language: String = "",
    val publicationStatus: Int = 0,
    val maxCount: Int = 0,
    val totalCount: Int = 0,
    val webLinks: String = "",
)

@Serializable
data class GenreDto(
    val id: Int = 0,
    val title: String = "",
)

@Serializable
data class TagDto(
    val id: Int = 0,
    val title: String = "",
)

@Serializable
data class PersonDto(
    val id: Int = 0,
    val name: String = "",
    val role: Int = 0,
)

@Serializable
data class FilterV2Dto(
    val id: Int = 0,
    val name: String? = null,
    val statements: List<FilterStatementDto> = emptyList(),
    val combination: Int = 0,
    val sortOptions: SortOptionsDto? = null,
    val limitTo: Int = 0,
)

@Serializable
data class FilterStatementDto(
    val comparison: Int = 0,
    val field: Int = 0,
    val value: String = "",
)

@Serializable
data class SortOptionsDto(
    val sortField: Int = 0,
    val isAscending: Boolean = true,
)

@Serializable
data class GroupedSeriesDto(
    val seriesName: String = "",
    val seriesId: Int = 0,
    val libraryId: Int = 0,
    val libraryType: Int = 0,
    val created: String = "",
    val chapterId: Int = 0,
    val volumeId: Int = 0,
    val id: Int = 0,
    val format: Int = 0,
    val count: Int = 0,
)

@Serializable
data class PagedResultDto<T>(
    val result: List<T> = emptyList(),
    val pagination: PaginationDto = PaginationDto(),
)

@Serializable
data class PaginationDto(
    val currentPage: Int = 1,
    val itemsPerPage: Int = 20,
    val totalItems: Int = 0,
    val totalPages: Int = 0,
)
