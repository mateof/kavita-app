package com.kavita.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "opds_feeds")
data class OpdsFeedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serverId: Long,
    val url: String,
    val title: String,
    val lastAccessed: Long = System.currentTimeMillis(),
)
