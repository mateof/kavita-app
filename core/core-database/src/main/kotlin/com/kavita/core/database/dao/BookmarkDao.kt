package com.kavita.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kavita.core.database.entity.BookmarkEntity

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE chapterId = :chapterId AND serverId = :serverId")
    suspend fun getByChapter(chapterId: Int, serverId: Long): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE chapterId = :chapterId AND page = :page AND serverId = :serverId")
    suspend fun deleteByPage(chapterId: Int, page: Int, serverId: Long)
}
