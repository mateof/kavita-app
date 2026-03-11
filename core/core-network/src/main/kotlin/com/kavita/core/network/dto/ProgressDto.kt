package com.kavita.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProgressDto(
    val chapterId: Int = 0,
    val pageNum: Int = 0,
    val seriesId: Int = 0,
    val volumeId: Int = 0,
    val libraryId: Int = 0,
    val bookScrollId: String? = null,
    val lastModified: String = "",
    val lastModifiedUtc: String = "",
)

/** DTO para enviar progreso al servidor (sin campos de fecha que el servidor gestiona). */
@Serializable
data class SaveProgressDto(
    val chapterId: Int = 0,
    val pageNum: Int = 0,
    val seriesId: Int = 0,
    val volumeId: Int = 0,
    val libraryId: Int = 0,
    val bookScrollId: String? = null,
)
