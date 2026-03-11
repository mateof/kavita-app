package com.kavita.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kavita.core.database.entity.EpubPageCountEntity

@Dao
interface EpubPageCountDao {

    @Query("SELECT totalPages FROM epub_page_count WHERE chapterId = :chapterId")
    fun getByChapterId(chapterId: Int): Int?

    @Query("SELECT * FROM epub_page_count")
    suspend fun getAll(): List<EpubPageCountEntity>

    @Upsert
    suspend fun upsert(entity: EpubPageCountEntity)
}
