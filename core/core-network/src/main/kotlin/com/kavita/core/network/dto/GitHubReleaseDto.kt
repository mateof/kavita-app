package com.kavita.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Respuesta de la API de GitHub para una release.
 * GET https://api.github.com/repos/{owner}/{repo}/releases/latest
 */
@Serializable
data class GitHubReleaseDto(
    val tag_name: String = "",
    val name: String? = null,
    val html_url: String = "",
    val body: String? = null,
    val prerelease: Boolean = false,
    val draft: Boolean = false,
    val assets: List<GitHubAssetDto> = emptyList(),
)

@Serializable
data class GitHubAssetDto(
    val name: String = "",
    val browser_download_url: String = "",
    val size: Long = 0,
    val content_type: String? = null,
)
