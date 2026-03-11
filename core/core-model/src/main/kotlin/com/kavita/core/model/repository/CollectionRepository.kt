package com.kavita.core.model.repository

import com.kavita.core.model.Collection
import com.kavita.core.model.Series

interface CollectionRepository {
    suspend fun getCollections(): List<Collection>
    suspend fun getCollectionSeries(collectionId: Int, page: Int = 1, pageSize: Int = 30): List<Series>
    suspend fun addSeriesToCollection(collectionId: Int, seriesIds: List<Int>)
    /** Crea una coleccion nueva y anade las series indicadas */
    suspend fun createCollectionWithSeries(title: String, seriesIds: List<Int>)
}
