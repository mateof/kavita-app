package com.kavita.core.data.repository

import com.kavita.core.model.Collection
import com.kavita.core.model.Series
import com.kavita.core.model.repository.CollectionRepository
import com.kavita.core.network.ActiveServerProvider
import com.kavita.core.network.RetrofitFactory
import com.kavita.core.network.api.KavitaCollectionApi
import com.kavita.core.network.api.KavitaSeriesApi
import com.kavita.core.network.dto.CollectionTagBulkAddDto
import com.kavita.core.network.dto.toDomain
import javax.inject.Inject

class CollectionRepositoryImpl @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
    private val activeServerProvider: ActiveServerProvider,
) : CollectionRepository {

    override suspend fun getCollections(): List<Collection> {
        val url = activeServerProvider.requireUrl()
        val apiKey = activeServerProvider.getApiKey()
        val serverId = activeServerProvider.requireId()
        val api = retrofitFactory.createApi<KavitaCollectionApi>(url)
        return api.getCollections().map { it.toDomain(url, apiKey, serverId) }
    }

    override suspend fun getCollectionSeries(collectionId: Int, page: Int, pageSize: Int): List<Series> {
        val url = activeServerProvider.requireUrl()
        val apiKey = activeServerProvider.getApiKey()
        val serverId = activeServerProvider.requireId()
        val api = retrofitFactory.createApi<KavitaSeriesApi>(url)
        val dtos = api.getSeriesByCollection(collectionId, page, pageSize)
        return dtos.map { it.toDomain(serverId, url, apiKey) }
    }

    override suspend fun addSeriesToCollection(collectionId: Int, seriesIds: List<Int>) {
        val url = activeServerProvider.requireUrl()
        val api = retrofitFactory.createApi<KavitaCollectionApi>(url)
        api.addSeriesToCollection(
            CollectionTagBulkAddDto(
                collectionTagId = collectionId,
                seriesIds = seriesIds,
            )
        )
    }

    override suspend fun createCollectionWithSeries(title: String, seriesIds: List<Int>) {
        val url = activeServerProvider.requireUrl()
        val api = retrofitFactory.createApi<KavitaCollectionApi>(url)
        // collectionTagId = 0 indica al servidor que cree una coleccion nueva
        api.addSeriesToCollection(
            CollectionTagBulkAddDto(
                collectionTagId = 0,
                collectionTagTitle = title,
                seriesIds = seriesIds,
            )
        )
    }
}
