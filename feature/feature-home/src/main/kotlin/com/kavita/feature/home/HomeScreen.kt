package com.kavita.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kavita.core.model.ContinueReadingItem
import com.kavita.core.model.RecentlyUpdatedSeries
import com.kavita.core.model.Series
import com.kavita.core.core.ui.R
import com.kavita.core.ui.components.CoverImage
import com.kavita.core.ui.components.EmptyState
import com.kavita.core.ui.components.ErrorScreen
import com.kavita.core.ui.components.LoadingIndicator
import com.kavita.core.ui.components.SeriesCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSeriesClick: (seriesId: Int, serverId: Long) -> Unit,
    onOpenReader: (chapterId: Int, seriesId: Int, volumeId: Int, format: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadedSeriesIds by viewModel.downloadedSeriesIds.collectAsStateWithLifecycle()

    // Refresh when returning from reader so progress is up to date
    LifecycleResumeEffect(Unit) {
        viewModel.refreshContinueReading()
        onPauseOrDispose { }
    }

    when {
        uiState.isLoading -> LoadingIndicator()
        uiState.error != null && uiState.continueReading.isEmpty() -> {
            ErrorScreen(
                message = uiState.error!!,
                onRetry = viewModel::refresh,
            )
        }
        else -> {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
            ) {
                if (uiState.continueReading.isEmpty() && uiState.recentlyUpdated.isEmpty() && uiState.recentlyAdded.isEmpty() && uiState.onDeck.isEmpty()) {
                    EmptyState(
                        message = stringResource(R.string.library_empty),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                    )
                } else {
                    HomeContent(
                        uiState = uiState,
                        downloadedSeriesIds = downloadedSeriesIds,
                        onSeriesClick = onSeriesClick,
                        onOpenReader = onOpenReader,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    downloadedSeriesIds: Set<Int>,
    onSeriesClick: (seriesId: Int, serverId: Long) -> Unit,
    onOpenReader: (chapterId: Int, seriesId: Int, volumeId: Int, format: String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        if (uiState.continueReading.isNotEmpty()) {
            ContinueReadingSection(
                title = stringResource(R.string.continue_reading),
                items = uiState.continueReading,
                onItemClick = { item ->
                    onOpenReader(item.chapterId, item.seriesId, item.volumeId, item.format.name)
                },
                onSeriesClick = { item ->
                    onSeriesClick(item.seriesId, item.serverId)
                },
            )
        }
        if (uiState.onDeck.isNotEmpty()) {
            ContinueReadingSection(
                title = stringResource(R.string.on_deck),
                items = uiState.onDeck,
                onItemClick = { item ->
                    if (item.chapterId > 0) {
                        onOpenReader(item.chapterId, item.seriesId, item.volumeId, item.format.name)
                    } else {
                        onSeriesClick(item.seriesId, item.serverId)
                    }
                },
                onSeriesClick = { item ->
                    onSeriesClick(item.seriesId, item.serverId)
                },
            )
        }
        if (uiState.recentlyUpdated.isNotEmpty()) {
            RecentlyUpdatedSection(
                title = stringResource(R.string.recently_updated),
                items = uiState.recentlyUpdated,
                downloadedSeriesIds = downloadedSeriesIds,
                onSeriesClick = onSeriesClick,
            )
        }
        if (uiState.recentlyAdded.isNotEmpty()) {
            SeriesSection(
                title = stringResource(R.string.recently_added),
                series = uiState.recentlyAdded,
                downloadedSeriesIds = downloadedSeriesIds,
                onSeriesClick = onSeriesClick,
            )
        }
    }
}

@Composable
private fun ContinueReadingSection(
    title: String = "",
    items: List<ContinueReadingItem>,
    onItemClick: (ContinueReadingItem) -> Unit,
    onSeriesClick: (ContinueReadingItem) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items, key = { "${it.serverId}-${it.seriesId}-${it.chapterId}" }) { item ->
                ContinueReadingCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    onSeriesClick = { onSeriesClick(item) },
                )
            }
        }
    }
}

@Composable
private fun ContinueReadingCard(
    item: ContinueReadingItem,
    onClick: () -> Unit,
    onSeriesClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
    ) {
        Box {
            CoverImage(
                imageUrl = item.coverUrl,
                contentDescription = item.seriesName,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (item.totalPages > 0) {
            LinearProgressIndicator(
                progress = { item.pagesRead.toFloat() / item.totalPages },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
            )
        }
        Text(
            text = item.seriesName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable(onClick = onSeriesClick),
        )
        if (item.chapterTitle.isNotBlank()) {
            Text(
                text = item.chapterTitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SeriesSection(
    title: String,
    series: List<Series>,
    downloadedSeriesIds: Set<Int>,
    onSeriesClick: (seriesId: Int, serverId: Long) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(series, key = { "${it.serverId}-${it.id}" }) { s ->
                SeriesCard(
                    name = s.name,
                    coverUrl = s.coverImage,
                    pagesRead = s.pagesRead,
                    totalPages = s.pages,
                    onClick = { onSeriesClick(s.id, s.serverId) },
                    isDownloaded = s.id in downloadedSeriesIds,
                )
            }
        }
    }
}

@Composable
private fun RecentlyUpdatedSection(
    title: String,
    items: List<RecentlyUpdatedSeries>,
    downloadedSeriesIds: Set<Int>,
    onSeriesClick: (seriesId: Int, serverId: Long) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(items, key = { "${it.serverId}-${it.seriesId}" }) { s ->
                SeriesCard(
                    name = s.seriesName,
                    coverUrl = s.coverImage,
                    pagesRead = 0,
                    totalPages = 0,
                    onClick = { onSeriesClick(s.seriesId, s.serverId) },
                    isDownloaded = s.seriesId in downloadedSeriesIds,
                )
            }
        }
    }
}
