package com.kavita.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class RefreshTokenRequest(
    val token: String,
    val refreshToken: String,
)

@Serializable
data class TokenResponse(
    val token: String = "",
    val refreshToken: String = "",
)
