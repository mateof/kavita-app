package com.kavita.core.network.api

import com.kavita.core.network.dto.GitHubReleaseDto
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * API pública de GitHub (sin autenticación). Se usa para comprobar si hay una nueva
 * release de la app publicada.
 */
interface GitHubApi {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): GitHubReleaseDto
}
