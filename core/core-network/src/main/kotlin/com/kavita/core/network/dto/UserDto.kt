package com.kavita.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Int = 0,
    val username: String = "",
    val email: String? = null,
    val roles: List<String> = emptyList(),
    val lastActiveUtc: String? = null,
    val isPending: Boolean = false,
)

@Serializable
data class InviteUserRequest(
    val email: String,
    val roles: List<String> = listOf("Pleb"),
)
