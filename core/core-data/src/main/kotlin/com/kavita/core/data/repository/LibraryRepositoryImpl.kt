@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.kavita.core.data.repository

import com.kavita.core.database.dao.LibraryDao
import com.kavita.core.database.entity.LibraryEntity
import com.kavita.core.model.Library
import com.kavita.core.model.LibraryType
import com.kavita.core.model.repository.LibraryRepository
import com.kavita.core.network.ActiveServerProvider
import com.kavita.core.network.RetrofitFactory
import com.kavita.core.network.api.KavitaLibraryApi
import com.kavita.core.network.dto.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class LibraryRepositoryImpl @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
    private val activeServerProvider: ActiveServerProvider,
    private val libraryDao: LibraryDao,
) : LibraryRepository {

    override fun observeLibraries(serverId: Long): Flow<List<Library>> {
        return flowOf(emptyList())
    }

    override suspend fun getLibraries(serverId: Long): List<Library> {
        return try {
            val url = activeServerProvider.requireUrl()
            val api = retrofitFactory.createApi<KavitaLibraryApi>(url)
            val libraries = api.getLibraries().map { it.toDomain(serverId, url, activeServerProvider.getApiKey()) }
            // Cache en Room
            libraryDao.upsertAll(libraries.map { it.toEntity(serverId) })
            libraries
        } catch (e: Exception) {
            // Fallback: datos de Room
            val cached = libraryDao.getByServer(serverId)
            if (cached.isNotEmpty()) {
                cached.map { it.toDomain(activeServerProvider.requireUrl(), activeServerProvider.getApiKey()) }
            } else throw e
        }
    }

    override suspend fun refreshLibraries(serverId: Long) {
        getLibraries(serverId)
    }

    override suspend fun scanLibrary(libraryId: Int) {
        val url = activeServerProvider.requireUrl()
        val api = retrofitFactory.createApi<KavitaLibraryApi>(url)
        api.scanLibrary(libraryId)
    }

    override suspend fun refreshLibraryMetadata(libraryId: Int) {
        val url = activeServerProvider.requireUrl()
        val api = retrofitFactory.createApi<KavitaLibraryApi>(url)
        api.refreshMetadata(libraryId)
    }
}

private fun Library.toEntity(serverId: Long) = LibraryEntity(
    id = "${id}_${serverId}".hashCode().toLong(),
    remoteId = id,
    serverId = serverId,
    name = name,
    type = type.name,
    coverImage = coverImage,
    lastScanned = lastScanned?.toEpochMilliseconds(),
)

private fun LibraryEntity.toDomain(baseUrl: String, apiKey: String?) = Library(
    id = remoteId,
    name = name,
    type = try { LibraryType.valueOf(type) } catch (_: Exception) { LibraryType.MANGA },
    coverImage = coverImage,
    lastScanned = null,
    folders = emptyList(),
    serverId = serverId,
)
