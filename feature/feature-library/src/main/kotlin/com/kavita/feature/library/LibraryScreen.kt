package com.kavita.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kavita.core.model.Series
import com.kavita.core.core.ui.R
import com.kavita.core.ui.components.EmptyState
import com.kavita.core.ui.components.ErrorScreen
import com.kavita.core.ui.components.LoadingIndicator
import com.kavita.core.ui.components.SeriesCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onSeriesClick: (seriesId: Int, serverId: Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadedSeriesIds by viewModel.downloadedSeriesIds.collectAsStateWithLifecycle()
    val downloadedSeries by viewModel.downloadedSeries.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> LoadingIndicator()
        uiState.error != null && uiState.series.isEmpty() -> {
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
                Column(modifier = Modifier.fillMaxSize()) {
                    // Filtros de biblioteca
                    LibraryFilterRow(
                        libraries = uiState.libraries,
                        selectedId = uiState.selectedLibraryId,
                        showDownloadedOnly = uiState.showDownloadedOnly,
                        onSelect = viewModel::selectLibrary,
                        onShowDownloaded = viewModel::showDownloaded,
                    )

                    if (uiState.showDownloadedOnly) {
                        if (downloadedSeries.isEmpty()) {
                            EmptyState(message = stringResource(R.string.no_downloaded_series))
                        } else {
                            SeriesGrid(
                                series = downloadedSeries,
                                downloadedSeriesIds = downloadedSeriesIds,
                                onSeriesClick = onSeriesClick,
                                onLoadMore = {},
                                hasMore = false,
                            )
                        }
                    } else if (uiState.series.isEmpty()) {
                        EmptyState(message = stringResource(R.string.no_series_in_library))
                    } else {
                        SeriesGrid(
                            series = uiState.series,
                            downloadedSeriesIds = downloadedSeriesIds,
                            onSeriesClick = onSeriesClick,
                            onLoadMore = viewModel::loadMore,
                            hasMore = uiState.hasMore,
                            scrollToIndex = uiState.scrollToIndex,
                            onScrolled = viewModel::clearScrollToIndex,
                            onLetterSelected = viewModel::jumpToLetter,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryFilterRow(
    libraries: List<com.kavita.core.model.Library>,
    selectedId: Int?,
    showDownloadedOnly: Boolean,
    onSelect: (Int?) -> Unit,
    onShowDownloaded: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selectedId == null && !showDownloadedOnly,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.all_libraries)) },
            )
        }
        items(libraries) { lib ->
            FilterChip(
                selected = selectedId == lib.id && !showDownloadedOnly,
                onClick = { onSelect(lib.id) },
                label = { Text(lib.name) },
            )
        }
        item {
            FilterChip(
                selected = showDownloadedOnly,
                onClick = onShowDownloaded,
                label = { Text(stringResource(R.string.downloaded)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.DownloadDone,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }
}

private val ALPHABET = ('#' + "ABCDEFGHIJKLMNOPQRSTUVWXYZ").toList()

@Composable
private fun SeriesGrid(
    series: List<Series>,
    downloadedSeriesIds: Set<Int>,
    onSeriesClick: (seriesId: Int, serverId: Long) -> Unit,
    onLoadMore: () -> Unit,
    hasMore: Boolean,
    scrollToIndex: Int? = null,
    onScrolled: () -> Unit = {},
    onLetterSelected: (Char) -> Unit = {},
) {
    val gridState = rememberLazyGridState()

    val reachedEnd by remember {
        derivedStateOf {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = gridState.layoutInfo.totalItemsCount
            totalItems > 0 && lastVisibleItem >= totalItems - 6
        }
    }

    LaunchedEffect(reachedEnd, hasMore, series.size) {
        if (reachedEnd && hasMore) onLoadMore()
    }

    LaunchedEffect(scrollToIndex) {
        if (scrollToIndex != null) {
            gridState.scrollToItem(scrollToIndex)
            onScrolled()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            state = gridState,
            contentPadding = PaddingValues(start = 8.dp, end = 28.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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

        AlphabetSidebar(
            letters = ALPHABET,
            onLetterSelected = onLetterSelected,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun AlphabetSidebar(
    letters: List<Char>,
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedLetter by remember { mutableStateOf<Char?>(null) }
    var barHeightPx by remember { mutableStateOf(0) }

    fun letterAtY(y: Float): Char? {
        if (barHeightPx <= 0) return null
        val index = ((y / barHeightPx) * letters.size).toInt().coerceIn(0, letters.size - 1)
        return letters[index]
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(24.dp)
            .padding(vertical = 8.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                RoundedCornerShape(12.dp),
            )
            .onSizeChanged { barHeightPx = it.height }
            .pointerInput(letters) {
                detectTapGestures { offset ->
                    letterAtY(offset.y)?.let { letter ->
                        selectedLetter = letter
                        onLetterSelected(letter)
                    }
                }
            }
            .pointerInput(letters) {
                detectVerticalDragGestures(
                    onDragEnd = { selectedLetter = null },
                    onDragCancel = { selectedLetter = null },
                ) { change, _ ->
                    change.consume()
                    letterAtY(change.position.y)?.let { letter ->
                        if (letter != selectedLetter) {
                            selectedLetter = letter
                            onLetterSelected(letter)
                        }
                    }
                }
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            val isSelected = letter == selectedLetter
            Text(
                text = letter.toString(),
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center,
            )
        }
    }
}
