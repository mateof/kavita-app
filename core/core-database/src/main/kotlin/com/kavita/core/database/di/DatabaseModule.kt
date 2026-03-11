package com.kavita.core.database.di

import android.content.Context
import androidx.room.Room
import com.kavita.core.database.KavitaDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KavitaDatabase =
        Room.databaseBuilder(
            context,
            KavitaDatabase::class.java,
            "kavita.db",
        ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideServerDao(db: KavitaDatabase): ServerDao = db.serverDao()

    @Provides
    fun provideReadingProgressDao(db: KavitaDatabase): ReadingProgressDao = db.readingProgressDao()

    @Provides
    fun provideBookmarkDao(db: KavitaDatabase): BookmarkDao = db.bookmarkDao()

    @Provides
    fun provideDownloadDao(db: KavitaDatabase): DownloadDao = db.downloadDao()

    @Provides
    fun provideSeriesDao(db: KavitaDatabase): SeriesDao = db.seriesDao()

    @Provides
    fun provideChapterDao(db: KavitaDatabase): ChapterDao = db.chapterDao()

    @Provides
    fun provideReadingSessionDao(db: KavitaDatabase): ReadingSessionDao = db.readingSessionDao()

    @Provides
    fun provideOpdsFeedDao(db: KavitaDatabase): OpdsFeedDao = db.opdsFeedDao()

    @Provides
    fun provideLibraryDao(db: KavitaDatabase): LibraryDao = db.libraryDao()

    @Provides
    fun provideEpubPageCountDao(db: KavitaDatabase): EpubPageCountDao = db.epubPageCountDao()
}
