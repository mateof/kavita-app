package com.kavita.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "libraries")
data class LibraryEntity(
    @PrimaryKey val id: Long = 0,
    val remoteId: Int,
    val serverId: Long,
    val name: String,
    val type: String = "MANGA",
    val coverImage: String? = null,
    val lastScanned: Long? = null,
)
