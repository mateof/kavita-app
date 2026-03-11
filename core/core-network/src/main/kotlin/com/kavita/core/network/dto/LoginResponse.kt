package com.kavita.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val username: String = "",
    val email: String = "",
    val token: String = "",
    val refreshToken: String = "",
    val apiKey: String = "",
    val kavitaVersion: String = "",
)
