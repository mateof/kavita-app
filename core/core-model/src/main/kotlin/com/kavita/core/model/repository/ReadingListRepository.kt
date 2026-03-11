package com.kavita.core.model.repository

import com.kavita.core.model.ReadingList
import com.kavita.core.model.ReadingListItem

interface ReadingListRepository {
    suspend fun getReadingLists(): List<ReadingList>
    suspend fun getReadingListItems(readingListId: Int): List<ReadingListItem>
    suspend fun createReadingList(title: String): ReadingList
    suspend fun addSeriesToReadingList(readingListId: Int, seriesIds: List<Int>)
}
