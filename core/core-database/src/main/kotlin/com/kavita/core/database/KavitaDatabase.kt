package com.kavita.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kavita.core.database.dao.BookmarkDao
import com.kavita.core.database.dao.DownloadDao
import com.kavita.core.database.dao.EpubPageCountDao
import com.kavita.core.database.dao.LibraryDao
import com.kavita.core.database.dao.ReadingProgressDao
import com.kavita.core.database.dao.ReadingSessionDao
import com.kavita.core.database.dao.ChapterDao
import com.kavita.core.database.dao.OpdsFeedDao
import com.kavita.core.database.dao.SeriesDao
import com.kavita.core.database.dao.ServerDao
import com.kavita.core.database.entity.BookmarkEntity
import com.kavita.core.database.entity.ChapterEntity
import com.kavita.core.database.entity.DownloadEntity
import com.kavita.core.database.entity.EpubPageCountEntity
import com.kavita.core.database.entity.LibraryEntity
import com.kavita.core.database.entity.OpdsFeedEntity
import com.kavita.core.database.entity.ReadingProgressEntity
import com.kavita.core.database.entity.ReadingSessionEntity
import com.kavita.core.database.entity.SeriesEntity
import com.kavita.core.database.entity.ServerEntity

@Database(
    entities = [
        ServerEntity::class,
        SeriesEntity::class,
        ChapterEntity::class,
        ReadingProgressEntity::class,
        BookmarkEntity::class,
        DownloadEntity::class,
        ReadingSessionEntity::class,
        OpdsFeedEntity::class,
        LibraryEntity::class,
        EpubPageCountEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class KavitaDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun seriesDao(): SeriesDao
    abstract fun chapterDao(): ChapterDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun downloadDao(): DownloadDao
    abstract fun readingSessionDao(): ReadingSessionDao
    abstract fun opdsFeedDao(): OpdsFeedDao
    abstract fun libraryDao(): LibraryDao
    abstract fun epubPageCountDao(): EpubPageCountDao
}
