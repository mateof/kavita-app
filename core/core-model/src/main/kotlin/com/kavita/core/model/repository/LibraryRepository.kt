package com.kavita.core.model.repository

import com.kavita.core.model.Library
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeLibraries(serverId: Long): Flow<List<Library>>
    suspend fun getLibraries(serverId: Long): List<Library>
    suspend fun refreshLibraries(serverId: Long)
    suspend fun scanLibrary(libraryId: Int)
    suspend fun refreshLibraryMetadata(libraryId: Int)
}
