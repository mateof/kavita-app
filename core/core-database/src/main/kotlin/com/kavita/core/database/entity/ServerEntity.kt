package com.kavita.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val type: String,
    val username: String? = null,
    val apiKey: String? = null,
    val token: String? = null,
    val refreshToken: String? = null,
    val isActive: Boolean = false,
)
