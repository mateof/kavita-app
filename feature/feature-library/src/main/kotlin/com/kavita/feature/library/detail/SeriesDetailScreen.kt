package com.kavita.feature.library.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kavita.core.model.Chapter
import com.kavita.core.model.Collection
import com.kavita.core.model.DownloadStatus
import com.kavita.core.model.DownloadTask
import com.kavita.core.model.MangaFormat
import com.kavita.core.model.ReadingList
import com.kavita.core.model.SeriesDetail
import com.kavita.core.model.SeriesMetadata
import com.kavita.core.model.Volume
import com.kavita.core.ui.components.CoverImage
import com.kavita.core.ui.components.DownloadBadge
import com.kavita.core.ui.components.ErrorScreen
import com.kavita.core.core.ui.R
import com.kavita.core.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailScreen(
    onNavigateBack: () -> Unit,
    onReadChapter: (chapterId: Int, seriesId: Int, volumeId: Int, format: String) -> Unit,
    viewModel: SeriesDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadedChapterIds by viewModel.downloadedChapterIds.collectAsStateWithLifecycle()
    val downloadTasks by viewModel.downloadTasks.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val readingLists by viewModel.readingLists.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    var showTopBarMenu by remember { mutableStateOf(false) }
    var showCollectionPicker by remember { mutableStateOf(false) }
    var showReadingListPicker by remember { mutableStateOf(false) }
    var showCreateCollectionDialog by remember { mutableStateOf(false) }
    var showCreateReadingListDialog by remember { mutableStateOf(false) }

    val savedMsg = stringResource(R.string.saved_to_downloads)
    val exportErrMsg = stringResource(R.string.export_error)
    val addedToCollectionMsg = stringResource(R.string.added_to_collection)
    val errCollectionMsg = stringResource(R.string.error_adding_to_collection)
    val addedToReadingListMsg = stringResource(R.string.added_to_reading_list)
    val errReadingListMsg = stringResource(R.string.error_adding_to_reading_list)

    LaunchedEffect(Unit) {
        viewModel.snackbar.collect { msg ->
            val text = when (msg) {
                "OK_EXPORT" -> savedMsg
                "ERR_EXPORT" -> exportErrMsg
                "OK_COLLECTION" -> addedToCollectionMsg
                "ERR_COLLECTION" -> errCollectionMsg
                "OK_READING_LIST" -> addedToReadingListMsg
                "ERR_READING_LIST" -> errReadingListMsg
                else -> msg
            }
            snackbarHostState.showSnackbar(text)
        }
    }

    // Refrescar datos al volver del lector (o de cualquier pantalla)
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.series?.name ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    androidx.compose.foundation.layout.Box {
                        IconButton(onClick = { showTopBarMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = showTopBarMenu,
                            onDismissRequest = { showTopBarMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.add_to_collection)) },
                                leadingIcon = { Icon(Icons.Filled.CollectionsBookmark, contentDescription = null) },
                                onClick = {
                                    showTopBarMenu = false
                                    viewModel.loadCollections()
                                    showCollectionPicker = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.add_to_reading_list)) },
                                leadingIcon = { Icon(Icons.Filled.PlaylistAdd, contentDescription = null) },
                                onClick = {
                                    showTopBarMenu = false
                                    viewModel.loadReadingLists()
                                    showReadingListPicker = true
                                },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.mark_as_read)) },
                                leadingIcon = { Icon(Icons.Filled.DoneAll, contentDescription = null) },
                                onClick = {
                                    showTopBarMenu = false
                                    viewModel.markAsRead()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.mark_as_unread)) },
                                leadingIcon = { Icon(Icons.Filled.Done, contentDescription = null) },
                                onClick = {
                                    showTopBarMenu = false
                                    viewModel.markAsUnread()
                                },
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingIndicator(modifier = Modifier.padding(innerPadding))
            uiState.error != null -> ErrorScreen(
                message = uiState.error!!,
                onRetry = viewModel::refresh,
                modifier = Modifier.padding(innerPadding),
            )
            else -> {
                val series = uiState.series!!
                val detail = uiState.detail
                val format = series.format

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    // Header
                    item {
                        SeriesHeader(
                            series = series,
                            onContinueReading = {
                                viewModel.getContinueReadingChapter()?.let { (chapterId, volumeId) ->
                                    onReadChapter(chapterId, viewModel.seriesId, volumeId, format.name)
                                }
                            },
                        )
                    }

                    // Metadata
                    if (uiState.metadata != null) {
                        item {
                            MetadataSection(metadata = uiState.metadata!!, format = format)
                        }
                    }

                    if (detail != null) {
                        // Volumes (numbered)
                        if (detail.volumes.isNotEmpty()) {
                            item {
                                SectionTitle(stringResource(R.string.volumes))
                            }
                            items(detail.volumes, key = { "vol-${it.id}" }) { volume ->
                                VolumeItem(
                                    volume = volume,
                                    format = format,
                                    downloadedChapterIds = downloadedChapterIds,
                                    downloadTasks = downloadTasks,
                                    onChapterClick = { chapter ->
                                        onReadChapter(chapter.id, viewModel.seriesId, volume.id, format.name)
                                    },
                                    onDownloadChapter = { chapter ->
                                        viewModel.downloadChapter(chapter.id, chapterFileName(chapter), format.name)
                                    },
                                    onDeleteDownload = { chapter ->
                                        viewModel.deleteChapterDownload(chapter.id)
                                    },
                                    onExportChapter = { chapter ->
                                        viewModel.exportChapter(
                                            chapter.id,
                                            chapterFileName(chapter),
                                            format.name,
                                            exportFileName(series.name, chapter, format),
                                        )
                                    },
                                )
                            }
                        }

                        // Loose chapters (numbered, not in volumes)
                        if (detail.chapters.isNotEmpty()) {
                            item {
                                SectionTitle(stringResource(R.string.chapters))
                            }
                            items(detail.chapters, key = { "ch-${it.id}" }) { chapter ->
                                ChapterCard(
                                    chapter = chapter,
                                    isDownloaded = chapter.id in downloadedChapterIds,
                                    downloadTask = downloadTasks.firstOrNull { it.chapterId == chapter.id },
                                    onClick = {
                                        onReadChapter(chapter.id, viewModel.seriesId, chapter.volumeId, format.name)
                                    },
                                    onDownload = {
                                        viewModel.downloadChapter(chapter.id, chapterFileName(chapter), format.name)
                                    },
                                    onDelete = {
                                        viewModel.deleteChapterDownload(chapter.id)
                                    },
                                    onExport = {
                                        viewModel.exportChapter(
                                            chapter.id,
                                            chapterFileName(chapter),
                                            format.name,
                                            exportFileName(series.name, chapter, format),
                                        )
                                    },
                                )
                            }
                        }

                        // Specials (individual books, magazines, etc.)
                        if (detail.specials.isNotEmpty()) {
                            item {
                                val title = when {
                                    detail.volumes.isEmpty() && detail.chapters.isEmpty() -> stringResource(R.string.content_label)
                                    else -> stringResource(R.string.specials)
                                }
                                SectionTitle(title)
                            }
                            items(detail.specials, key = { "sp-${it.id}" }) { chapter ->
                                ChapterCard(
                                    chapter = chapter,
                                    isDownloaded = chapter.id in downloadedChapterIds,
                                    downloadTask = downloadTasks.firstOrNull { it.chapterId == chapter.id },
                                    onClick = {
                                        onReadChapter(chapter.id, viewModel.seriesId, chapter.volumeId, format.name)
                                    },
                                    onDownload = {
                                        viewModel.downloadChapter(chapter.id, chapterFileName(chapter), format.name)
                                    },
                                    onDelete = {
                                        viewModel.deleteChapterDownload(chapter.id)
                                    },
                                    onExport = {
                                        viewModel.exportChapter(
                                            chapter.id,
                                            chapterFileName(chapter),
                                            format.name,
                                            exportFileName(series.name, chapter, format),
                                        )
                                    },
                                )
                            }
                        }
                    }

                    // Bottom spacing
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    // Bottom sheet: seleccionar coleccion
    if (showCollectionPicker) {
        CollectionPickerSheet(
            collections = collections,
            onDismiss = { showCollectionPicker = false },
            onSelect = { collectionId ->
                showCollectionPicker = false
                viewModel.addToCollection(collectionId)
            },
            onCreateNew = {
                showCollectionPicker = false
                showCreateCollectionDialog = true
            },
        )
    }

    // Bottom sheet: seleccionar lista de lectura
    if (showReadingListPicker) {
        ReadingListPickerSheet(
            readingLists = readingLists,
            onDismiss = { showReadingListPicker = false },
            onSelect = { readingListId ->
                showReadingListPicker = false
                viewModel.addToReadingList(readingListId)
            },
            onCreateNew = {
                showReadingListPicker = false
                showCreateReadingListDialog = true
            },
        )
    }

    // Dialogo: crear coleccion nueva
    if (showCreateCollectionDialog) {
        CreateNameDialog(
            title = stringResource(R.string.new_collection_title),
            onDismiss = { showCreateCollectionDialog = false },
            onConfirm = { name ->
                showCreateCollectionDialog = false
                viewModel.createCollectionAndAdd(name)
            },
        )
    }

    // Dialogo: crear lista de lectura nueva
    if (showCreateReadingListDialog) {
        CreateNameDialog(
            title = stringResource(R.string.new_reading_list_title),
            onDismiss = { showCreateReadingListDialog = false },
            onConfirm = { name ->
                showCreateReadingListDialog = false
                viewModel.createReadingListAndAdd(name)
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun SeriesHeader(
    series: com.kavita.core.model.Series,
    onContinueReading: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        CoverImage(
            imageUrl = series.coverImage,
            contentDescription = series.name,
            modifier = Modifier
                .width(130.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp)),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
        ) {
            Text(
                text = series.name,
                style = MaterialTheme.typography.titleLarge,
            )
            if (series.pages > 0) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { series.pagesRead.toFloat() / series.pages },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.pages_progress, series.pagesRead, series.pages),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Button(
                onClick = onContinueReading,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(if (series.pagesRead > 0) stringResource(R.string.continue_reading_btn) else stringResource(R.string.start_reading))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetadataSection(
    metadata: SeriesMetadata,
    format: MangaFormat,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        if (metadata.summary.isNotBlank()) {
            var expanded by remember { mutableStateOf(false) }
            Text(
                text = stringResource(R.string.synopsis),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = metadata.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .animateContentSize()
                    .clickable { expanded = !expanded },
            )
            Spacer(Modifier.height(12.dp))
        }

        val infoItems = buildList {
            if (metadata.writers.isNotEmpty()) add(stringResource(R.string.author) to metadata.writers.joinToString())
            if (metadata.publishers.isNotEmpty()) add(stringResource(R.string.publisher) to metadata.publishers.joinToString())
            add(stringResource(R.string.format) to formatLabel(format))
            if (metadata.releaseYear > 0) add(stringResource(R.string.year) to metadata.releaseYear.toString())
            if (metadata.language.isNotBlank()) add(stringResource(R.string.language) to languageLabel(metadata.language))
            if (metadata.ageRating.ordinal > 0) add(stringResource(R.string.rating) to metadata.ageRating.label)
            add(stringResource(R.string.status) to metadata.publicationStatus.label)
            if (metadata.totalCount > 0) add(stringResource(R.string.total) to "${metadata.totalCount}")
            if (metadata.pencillers.isNotEmpty()) add(stringResource(R.string.artist) to metadata.pencillers.joinToString())
            if (metadata.colorists.isNotEmpty()) add(stringResource(R.string.colorist) to metadata.colorists.joinToString())
            if (metadata.translators.isNotEmpty()) add(stringResource(R.string.translator) to metadata.translators.joinToString())
            if (metadata.coverArtists.isNotEmpty()) add(stringResource(R.string.cover_artist) to metadata.coverArtists.joinToString())
        }

        infoItems.forEach { (label, value) ->
            MetadataRow(label = label, value = value)
        }

        if (metadata.genres.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.genres), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                metadata.genres.forEach { genre ->
                    AssistChip(onClick = {}, label = { Text(genre) })
                }
            }
        }

        if (metadata.tags.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.tags), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                metadata.tags.forEach { tag ->
                    AssistChip(onClick = {}, label = { Text(tag) })
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(100.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ChapterCard(
    chapter: Chapter,
    isDownloaded: Boolean = false,
    downloadTask: DownloadTask? = null,
    onClick: () -> Unit,
    onDownload: () -> Unit = {},
    onDelete: () -> Unit = {},
    onExport: () -> Unit = {},
) {
    val isDownloading = downloadTask?.status == DownloadStatus.DOWNLOADING ||
        downloadTask?.status == DownloadStatus.PENDING

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (chapter.coverImage != null) {
                CoverImage(
                    imageUrl = chapter.coverImage,
                    contentDescription = chapterDisplayName(chapter),
                    modifier = Modifier
                        .width(50.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(4.dp)),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chapterDisplayName(chapter),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isDownloaded) {
                        Spacer(Modifier.width(6.dp))
                        DownloadBadge()
                    }
                }
                if (chapter.pages > 0) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { chapter.pagesRead.toFloat() / chapter.pages },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.pages_progress, chapter.pagesRead, chapter.pages),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            ChapterOverflowMenu(
                isDownloaded = isDownloaded,
                isDownloading = isDownloading,
                onDownload = onDownload,
                onExport = onExport,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun VolumeItem(
    volume: Volume,
    format: MangaFormat,
    downloadedChapterIds: Set<Int>,
    downloadTasks: List<DownloadTask>,
    onChapterClick: (Chapter) -> Unit,
    onDownloadChapter: (Chapter) -> Unit,
    onDeleteDownload: (Chapter) -> Unit,
    onExportChapter: (Chapter) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val hasDownloadedChapter = volume.chapters.any { it.id in downloadedChapterIds }
    // Si el volumen tiene un solo capitulo, tratarlo como ChapterCard
    val singleChapter = volume.chapters.singleOrNull()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .animateContentSize(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (singleChapter != null) {
                            onChapterClick(singleChapter)
                        } else {
                            expanded = !expanded
                        }
                    }
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (volume.coverImage != null) {
                    CoverImage(
                        imageUrl = volume.coverImage,
                        contentDescription = volumeDisplayName(volume),
                        modifier = Modifier
                            .width(50.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = volumeDisplayName(volume),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (hasDownloadedChapter) {
                            Spacer(Modifier.width(6.dp))
                            DownloadBadge()
                        }
                    }
                    if (volume.pages > 0) {
                        LinearProgressIndicator(
                            progress = { volume.pagesRead.toFloat() / volume.pages },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        )
                        Text(
                            text = stringResource(R.string.pages_progress, volume.pagesRead, volume.pages),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (singleChapter != null) {
                    // Mostrar menu para el unico capitulo del volumen
                    ChapterOverflowMenu(
                        isDownloaded = singleChapter.id in downloadedChapterIds,
                        isDownloading = downloadTasks.any {
                            it.chapterId == singleChapter.id &&
                                (it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING)
                        },
                        onDownload = { onDownloadChapter(singleChapter) },
                        onExport = { onExportChapter(singleChapter) },
                        onDelete = { onDeleteDownload(singleChapter) },
                    )
                } else if (volume.chapters.size > 1) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = stringResource(if (expanded) R.string.collapse else R.string.expand),
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }

            if (expanded && volume.chapters.size > 1) {
                volume.chapters.forEach { chapter ->
                    val isChDownloaded = chapter.id in downloadedChapterIds
                    val chTask = downloadTasks.firstOrNull { it.chapterId == chapter.id }
                    val isChDownloading = chTask?.status == DownloadStatus.DOWNLOADING ||
                        chTask?.status == DownloadStatus.PENDING

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChapterClick(chapter) }
                            .padding(start = 24.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = chapterDisplayName(chapter),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (isChDownloaded) {
                                    Spacer(Modifier.width(6.dp))
                                    DownloadBadge()
                                }
                            }
                            if (chapter.pages > 0) {
                                Text(
                                    text = stringResource(R.string.pages_progress, chapter.pagesRead, chapter.pages),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        ChapterOverflowMenu(
                            isDownloaded = isChDownloaded,
                            isDownloading = isChDownloading,
                            onDownload = { onDownloadChapter(chapter) },
                            onExport = { onExportChapter(chapter) },
                            onDelete = { onDeleteDownload(chapter) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterOverflowMenu(
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    androidx.compose.foundation.layout.Box {
        if (isDownloading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(12.dp)
                    .height(24.dp)
                    .width(24.dp),
                strokeWidth = 2.dp,
            )
        } else {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            if (!isDownloaded) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.download_offline)) },
                    leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null) },
                    onClick = { showMenu = false; onDownload() },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.save_to_downloads)) },
                leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null) },
                onClick = { showMenu = false; onExport() },
            )
            if (isDownloaded) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete_download)) },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    onClick = { showMenu = false; onDelete() },
                )
            }
        }
    }
}

private const val SPECIAL_NUMBER = -100000

@Composable
private fun volumeDisplayName(volume: Volume): String {
    if (volume.number == SPECIAL_NUMBER) {
        val chapterTitle = volume.chapters.firstOrNull()?.let { chapterDisplayName(it) }
        return if (volume.chapters.size == 1 && chapterTitle != null) chapterTitle else stringResource(R.string.specials)
    }
    val name = volume.name.takeIf { it.isNotBlank() && it != volume.number.toString() && it != "100000" }
    return name ?: stringResource(R.string.volume_number, volume.number.toString())
}

@Composable
private fun chapterDisplayName(chapter: Chapter): String {
    val title = chapter.title?.takeIf { it.isNotBlank() }
    val range = chapter.range?.takeIf { it.isNotBlank() }
    if (title != null) return title
    if (range != null) return range
    if (chapter.number == SPECIAL_NUMBER.toString() || chapter.number == "-100000") return stringResource(R.string.special_label)
    val num = chapter.number.toFloatOrNull()
    return if (num != null && num > 0) stringResource(R.string.chapter_number, chapter.number) else stringResource(R.string.chapter_label)
}

@Composable
private fun formatLabel(format: MangaFormat): String = when (format) {
    MangaFormat.IMAGE -> stringResource(R.string.format_image)
    MangaFormat.ARCHIVE -> stringResource(R.string.format_archive)
    MangaFormat.EPUB -> stringResource(R.string.format_epub)
    MangaFormat.PDF -> stringResource(R.string.format_pdf)
    MangaFormat.UNKNOWN -> stringResource(R.string.format_unknown)
}

@Composable
private fun languageLabel(code: String): String = when (code.lowercase()) {
    "es" -> stringResource(R.string.lang_spanish)
    "en" -> stringResource(R.string.lang_english)
    "fr" -> stringResource(R.string.lang_french)
    "de" -> stringResource(R.string.lang_german)
    "it" -> stringResource(R.string.lang_italian)
    "pt" -> stringResource(R.string.lang_portuguese)
    "ja" -> stringResource(R.string.lang_japanese)
    "ko" -> stringResource(R.string.lang_korean)
    "zh" -> stringResource(R.string.lang_chinese)
    "ca" -> stringResource(R.string.lang_catalan)
    "gl" -> stringResource(R.string.lang_galician)
    "eu" -> stringResource(R.string.lang_basque)
    else -> code.uppercase()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionPickerSheet(
    collections: List<Collection>,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    onCreateNew: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Text(
            text = stringResource(R.string.add_to_collection),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.create_new)) },
            leadingContent = { Icon(Icons.Filled.Add, contentDescription = null) },
            modifier = Modifier.clickable { onCreateNew() },
        )
        HorizontalDivider()
        collections.forEach { collection ->
            ListItem(
                headlineContent = { Text(collection.title) },
                leadingContent = { Icon(Icons.Filled.CollectionsBookmark, contentDescription = null) },
                modifier = Modifier.clickable { onSelect(collection.id) },
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingListPickerSheet(
    readingLists: List<ReadingList>,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    onCreateNew: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Text(
            text = stringResource(R.string.add_to_reading_list),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.create_new)) },
            leadingContent = { Icon(Icons.Filled.Add, contentDescription = null) },
            modifier = Modifier.clickable { onCreateNew() },
        )
        HorizontalDivider()
        readingLists.forEach { list ->
            ListItem(
                headlineContent = { Text(list.title) },
                leadingContent = { Icon(Icons.Filled.PlaylistAdd, contentDescription = null) },
                modifier = Modifier.clickable { onSelect(list.id) },
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun CreateNameDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.enter_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

private fun chapterFileName(chapter: Chapter): String {
    return chapter.title?.takeIf { it.isNotBlank() }
        ?: chapter.range?.takeIf { it.isNotBlank() }
        ?: "Chapter ${chapter.number}"
}

private fun exportFileName(seriesName: String, chapter: Chapter, format: MangaFormat): String {
    val ext = when (format) {
        MangaFormat.EPUB -> "epub"
        MangaFormat.PDF -> "pdf"
        MangaFormat.ARCHIVE -> "cbz"
        else -> "bin"
    }
    val chName = chapterFileName(chapter).replace(Regex("[/\\\\:*?\"<>|]"), "_")
    val safeSeries = seriesName.replace(Regex("[/\\\\:*?\"<>|]"), "_")
    return "$safeSeries - $chName.$ext"
}
