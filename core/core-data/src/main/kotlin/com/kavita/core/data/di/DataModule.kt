package com.kavita.core.data.di

import com.kavita.core.data.repository.AdminRepositoryImpl
import com.kavita.core.data.repository.AuthRepositoryImpl
import com.kavita.core.data.repository.CollectionRepositoryImpl
import com.kavita.core.data.repository.DownloadRepositoryImpl
import com.kavita.core.data.repository.LibraryRepositoryImpl
import com.kavita.core.data.repository.OpdsRepositoryImpl
import com.kavita.core.data.repository.ReaderRepositoryImpl
import com.kavita.core.data.repository.ReadingListRepositoryImpl
import com.kavita.core.data.repository.SeriesRepositoryImpl
import com.kavita.core.data.repository.ServerRepositoryImpl
import com.kavita.core.data.repository.StatsRepositoryImpl
import com.kavita.core.model.repository.AdminRepository
import com.kavita.core.model.repository.AuthRepository
import com.kavita.core.model.repository.CollectionRepository
import com.kavita.core.model.repository.DownloadRepository
import com.kavita.core.model.repository.LibraryRepository
import com.kavita.core.model.repository.OpdsRepository
import com.kavita.core.model.repository.ReaderRepository
import com.kavita.core.model.repository.ReadingListRepository
import com.kavita.core.model.repository.SeriesRepository
import com.kavita.core.model.repository.ServerRepository
import com.kavita.core.model.repository.StatsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindServerRepository(impl: ServerRepositoryImpl): ServerRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSeriesRepository(impl: SeriesRepositoryImpl): SeriesRepository

    @Binds
    @Singleton
    abstract fun bindReaderRepository(impl: ReaderRepositoryImpl): ReaderRepository

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository

    @Binds
    @Singleton
    abstract fun bindStatsRepository(impl: StatsRepositoryImpl): StatsRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindOpdsRepository(impl: OpdsRepositoryImpl): OpdsRepository

    @Binds
    @Singleton
    abstract fun bindAdminRepository(impl: AdminRepositoryImpl): AdminRepository

    @Binds
    @Singleton
    abstract fun bindCollectionRepository(impl: CollectionRepositoryImpl): CollectionRepository

    @Binds
    @Singleton
    abstract fun bindReadingListRepository(impl: ReadingListRepositoryImpl): ReadingListRepository
}
