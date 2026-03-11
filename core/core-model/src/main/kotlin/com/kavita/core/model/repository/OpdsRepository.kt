package com.kavita.core.model.repository

import com.kavita.core.model.OpdsFeed

interface OpdsRepository {
    suspend fun fetchFeed(url: String, username: String? = null, password: String? = null): Result<OpdsFeed>
    suspend fun searchCatalog(searchUrl: String, query: String): Result<OpdsFeed>
}
