package com.kavita.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "series")
data class SeriesEntity(
    @PrimaryKey val id: Long = 0,
    val remoteId: Int,
    val serverId: Long,
    val name: String,
    val sortName: String = "",
    val localizedName: String? = null,
    val originalName: String? = null,
    val summary: String? = null,
    val format: String = "UNKNOWN",
    val libraryId: Int = 0,
    val libraryName: String? = null,
    val pagesRead: Int = 0,
    val pages: Int = 0,
    val userRating: Float = 0f,
    val lastChapterAdded: Long? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
)
