package com.kavita.core.data.repository

import com.kavita.core.model.ReadingList
import com.kavita.core.model.ReadingListItem
import com.kavita.core.model.repository.ReadingListRepository
import com.kavita.core.network.ActiveServerProvider
import com.kavita.core.network.RetrofitFactory
import com.kavita.core.network.api.KavitaReadingListApi
import com.kavita.core.network.dto.CreateReadingListRequest
import com.kavita.core.network.dto.UpdateReadingListByMultipleSeriesDto
import com.kavita.core.network.dto.toDomain
import javax.inject.Inject

class ReadingListRepositoryImpl @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
    private val activeServerProvider: ActiveServerProvider,
) : ReadingListRepository {

    override suspend fun getReadingLists(): List<ReadingList> {
        val url = activeServerProvider.requireUrl()
        val apiKey = activeServerProvider.getApiKey()
        val serverId = activeServerProvider.requireId()
        val api = retrofitFactory.createApi<KavitaReadingListApi>(url)
        return api.getReadingLists().map { it.toDomain(url, apiKey, serverId) }
    }

    override suspend fun getReadingListItems(readingListId: Int): List<ReadingListItem> {
        val url = activeServerProvider.requireUrl()
        val apiKey = activeServerProvider.getApiKey()
        val api = retrofitFactory.createApi<KavitaReadingListApi>(url)
        return api.getReadingListItems(readingListId).map { it.toDomain(url, apiKey) }
    }

    override suspend fun createReadingList(title: String): ReadingList {
        val url = activeServerProvider.requireUrl()
        val apiKey = activeServerProvider.getApiKey()
        val serverId = activeServerProvider.requireId()
        val api = retrofitFactory.createApi<KavitaReadingListApi>(url)
        val dto = api.createReadingList(CreateReadingListRequest(title))
        return dto.toDomain(url, apiKey, serverId)
    }

    override suspend fun addSeriesToReadingList(readingListId: Int, seriesIds: List<Int>) {
        val url = activeServerProvider.requireUrl()
        val api = retrofitFactory.createApi<KavitaReadingListApi>(url)
        api.addSeriesToReadingList(
            UpdateReadingListByMultipleSeriesDto(readingListId, seriesIds)
        )
    }
}
