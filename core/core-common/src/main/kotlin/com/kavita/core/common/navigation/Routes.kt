package com.kavita.core.common.navigation

import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object LibraryRoute
@Serializable data object SearchRoute
@Serializable data object DownloadsRoute
@Serializable data object MoreRoute
@Serializable data class SeriesDetailRoute(val seriesId: Int, val serverId: Long)
@Serializable data class ReaderRoute(
    val chapterId: Int,
    val seriesId: Int,
    val volumeId: Int,
    val format: String,
)
@Serializable data object SettingsRoute
@Serializable data object AdminRoute
@Serializable data object LoginRoute
@Serializable data object ServerManagementRoute
@Serializable data object StatsRoute
@Serializable data class OpdsBrowseRoute(val serverUrl: String)
@Serializable data object CollectionsRoute
@Serializable data class CollectionDetailRoute(val collectionId: Int, val serverId: Long)
@Serializable data object ReadingListsRoute
@Serializable data class ReadingListDetailRoute(val readingListId: Int, val serverId: Long)
