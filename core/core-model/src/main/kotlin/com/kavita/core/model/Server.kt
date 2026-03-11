package com.kavita.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Server(
    val id: Long = 0,
    val name: String,
    val url: String,
    val type: ServerType,
    val username: String? = null,
    val apiKey: String? = null,
    val token: String? = null,
    val refreshToken: String? = null,
    val isActive: Boolean = false,
)

@Serializable
enum class ServerType {
    KAVITA,
    OPDS,
}
