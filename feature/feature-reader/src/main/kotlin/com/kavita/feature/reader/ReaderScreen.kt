package com.kavita.feature.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url
import com.kavita.core.model.MangaFormat
import com.kavita.core.ui.components.LoadingIndicator
import com.kavita.core.ui.components.ErrorScreen
import com.kavita.feature.reader.comic.ComicReader
import com.kavita.feature.reader.pdf.PdfPageReader
import com.kavita.feature.reader.readium.ReadiumReader
import com.kavita.feature.reader.overlay.BrightnessOverlay
import com.kavita.feature.reader.overlay.ReaderControls
import com.kavita.feature.reader.overlay.ReaderSettingsSheet
import com.kavita.feature.reader.overlay.TocSheet

@Composable
fun ReaderScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChapter: (chapterId: Int, seriesId: Int, volumeId: Int, format: String) -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }
    var showToc by remember { mutableStateOf(false) }
    val activity = LocalContext.current as FragmentActivity

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ReaderEvent.NavigateToChapter -> {
                    onNavigateToChapter(event.chapterId, event.seriesId, event.volumeId, event.format)
                }
                ReaderEvent.CloseReader -> onNavigateBack()
                is ReaderEvent.NavigateToToc -> {
                    val fragment = activity.supportFragmentManager.findFragmentByTag("readium_nav")
                    if (fragment is EpubNavigatorFragment) {
                        val url = Url(event.href)
                        if (url != null) {
                            val locator = Locator(href = url, mediaType = org.readium.r2.shared.util.mediatype.MediaType.XHTML)
                            fragment.go(locator, animated = true)
                        }
                    }
                }
            }
        }
    }

    when {
        uiState.isLoading -> LoadingIndicator()
        uiState.error != null -> ErrorScreen(message = uiState.error!!)
        else -> {
            Box(modifier = Modifier.fillMaxSize()) {
                when (uiState.format) {
                    MangaFormat.PDF -> {
                        val pdfFile = uiState.pdfFile
                        if (pdfFile != null) {
                            PdfPageReader(
                                file = pdfFile,
                                currentPage = uiState.currentPage,
                                pageLayout = preferences.pageLayout,
                                pageScaleType = preferences.pageScaleType,
                                pageTransition = preferences.pageTransition,
                                tapNavigation = preferences.tapNavigation,
                                readingDirection = preferences.defaultReadingDirection,
                                nightMode = preferences.pdfNightMode,
                                onPageChanged = viewModel::onPageChanged,
                                onTapCenter = viewModel::toggleControls,
                            )
                        } else {
                            DownloadProgressIndicator(uiState)
                        }
                    }
                    MangaFormat.EPUB -> {
                        val publication = viewModel.publication
                        if (publication != null && uiState.publicationReady) {
                            ReadiumReader(
                                publication = publication,
                                format = uiState.format,
                                currentPage = uiState.currentPage,
                                pageLayout = preferences.pageLayout,
                                pageScaleType = preferences.pageScaleType,
                                readingDirection = preferences.defaultReadingDirection,
                                tapNavigation = preferences.tapNavigation,
                                epubFontSize = preferences.epubFontSize,
                                epubFontFamily = preferences.epubFontFamily,
                                epubLineSpacing = preferences.epubLineSpacing,
                                epubTheme = preferences.epubTheme,
                                onPageChanged = viewModel::onPageChanged,
                                onTotalPagesResolved = viewModel::onTotalPagesResolved,
                                onTapCenter = viewModel::toggleControls,
                            )
                        } else {
                            DownloadProgressIndicator(uiState)
                        }
                    }
                    else -> {
                        ComicReader(
                            totalPages = uiState.totalPages,
                            currentPage = uiState.currentPage,
                            readingDirection = preferences.defaultReadingDirection,
                            pageLayout = preferences.pageLayout,
                            pageScaleType = preferences.pageScaleType,
                            pageTransition = preferences.pageTransition,
                            tapNavigation = preferences.tapNavigation,
                            getPageImageUrl = viewModel::getPageImageUrl,
                            onPageChanged = viewModel::onPageChanged,
                            onTapCenter = viewModel::toggleControls,
                            pageUrls = uiState.pageUrls,
                        )
                    }
                }

                BrightnessOverlay(brightness = uiState.brightness)

                ReaderControls(
                    visible = uiState.showControls,
                    title = uiState.chapterTitle,
                    currentPage = uiState.currentPage,
                    totalPages = uiState.totalPages,
                    isBookmarked = uiState.isBookmarked,
                    brightness = uiState.brightness,
                    hasPrevChapter = uiState.prevChapterId != null,
                    hasNextChapter = uiState.nextChapterId != null,
                    hasToc = uiState.tableOfContents.isNotEmpty(),
                    onClose = viewModel::closeReader,
                    onBookmarkToggle = viewModel::toggleBookmark,
                    onPageSliderChange = viewModel::onPageChanged,
                    onBrightnessChange = viewModel::setBrightness,
                    onPrevChapter = viewModel::goToPrevChapter,
                    onNextChapter = viewModel::goToNextChapter,
                    onOpenSettings = { showSettings = true },
                    onOpenToc = { showToc = true },
                )
            }

            if (showToc) {
                TocSheet(
                    entries = uiState.tableOfContents,
                    onEntryClick = { entry ->
                        viewModel.navigateToTocEntry(entry)
                    },
                    onDismiss = { showToc = false },
                )
            }

            if (showSettings) {
                ReaderSettingsSheet(
                    readingDirection = preferences.defaultReadingDirection,
                    pageLayout = preferences.pageLayout,
                    pageScaleType = preferences.pageScaleType,
                    pageTransition = preferences.pageTransition,
                    tapNavigation = preferences.tapNavigation,
                    format = uiState.format,
                    pdfNightMode = preferences.pdfNightMode,
                    epubFontSize = preferences.epubFontSize,
                    epubFontFamily = preferences.epubFontFamily,
                    epubLineSpacing = preferences.epubLineSpacing,
                    epubTheme = preferences.epubTheme,
                    onReadingDirectionChange = viewModel::updateReadingDirection,
                    onPageLayoutChange = viewModel::updatePageLayout,
                    onPageScaleTypeChange = viewModel::updatePageScaleType,
                    onPageTransitionChange = viewModel::updatePageTransition,
                    onTapNavigationChange = viewModel::updateTapNavigation,
                    onPdfNightModeChange = viewModel::updatePdfNightMode,
                    onEpubFontSizeChange = viewModel::updateEpubFontSize,
                    onEpubFontFamilyChange = viewModel::updateEpubFontFamily,
                    onEpubLineSpacingChange = viewModel::updateEpubLineSpacing,
                    onEpubThemeChange = viewModel::updateEpubTheme,
                    onDismiss = { showSettings = false },
                )
            }
        }
    }
}

@Composable
private fun DownloadProgressIndicator(uiState: ReaderUiState) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (uiState.isDownloading && uiState.downloadTotalBytes > 0) {
            val progress = uiState.downloadBytesDownloaded.toFloat() / uiState.downloadTotalBytes
            val downloadedMb = uiState.downloadBytesDownloaded / (1024f * 1024f)
            val totalMb = uiState.downloadTotalBytes / (1024f * 1024f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 48.dp),
            ) {
                Text(
                    text = uiState.chapterTitle.ifBlank { "Descargando..." },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "%.1f MB / %.1f MB".format(downloadedMb, totalMb),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            CircularProgressIndicator()
        }
    }
}
