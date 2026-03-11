package com.kavita.feature.reader

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kavita.core.common.navigation.ReaderRoute
import com.kavita.core.model.Bookmark
import com.kavita.core.model.MangaFormat
import com.kavita.core.model.PageLayout
import com.kavita.core.model.PageScaleType
import com.kavita.core.model.PageTransition
import com.kavita.core.model.ReaderTheme
import com.kavita.core.model.ReadingProgress
import com.kavita.core.model.TapNavigation
import com.kavita.core.model.UserPreferences
import com.kavita.core.model.ReadingDirection
import com.kavita.core.model.repository.DownloadRepository
import com.kavita.core.model.repository.ReaderRepository
import com.kavita.core.model.repository.SeriesRepository
import com.kavita.core.model.repository.StatsRepository
import com.kavita.core.data.preferences.UserPreferencesDataStore
import com.kavita.core.network.ActiveServerProvider
import com.kavita.core.readium.ReadiumManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication
import java.io.File
import javax.inject.Inject

data class TocEntry(
    val title: String,
    val href: String,
    val level: Int = 0,
)

data class ReaderUiState(
    val chapterId: Int = 0,
    val seriesId: Int = 0,
    val volumeId: Int = 0,
    val libraryId: Int = 0,
    val format: MangaFormat = MangaFormat.UNKNOWN,
    val chapterTitle: String = "",
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    /** Total de paginas segun Kavita (para enviar progreso al servidor). */
    val kavitaTotalPages: Int = 0,
    val isLoading: Boolean = true,
    val showControls: Boolean = false,
    val isBookmarked: Boolean = false,
    val brightness: Float = 1f,
    val error: String? = null,
    val nextChapterId: Int? = null,
    val prevChapterId: Int? = null,
    val pdfFile: File? = null,
    val publicationReady: Boolean = false,
    val tableOfContents: List<TocEntry> = emptyList(),
    /** URLs precomputadas para cada pagina del capitulo (solo comic/manga). */
    val pageUrls: List<String> = emptyList(),
    /** Progreso de descarga del fichero (PDF/EPUB). */
    val downloadBytesDownloaded: Long = 0,
    val downloadTotalBytes: Long = 0,
    val isDownloading: Boolean = false,
)

sealed interface ReaderEvent {
    data class NavigateToChapter(val chapterId: Int, val seriesId: Int, val volumeId: Int, val format: String) : ReaderEvent
    data object CloseReader : ReaderEvent
    data class NavigateToToc(val href: String) : ReaderEvent
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val readerRepository: ReaderRepository,
    private val downloadRepository: DownloadRepository,
    private val seriesRepository: SeriesRepository,
    private val statsRepository: StatsRepository,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val activeServerProvider: ActiveServerProvider,
    private val readiumManager: ReadiumManager,
    private val application: Application,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ReaderRoute>()

    private val _uiState = MutableStateFlow(
        ReaderUiState(
            chapterId = route.chapterId,
            seriesId = route.seriesId,
            volumeId = route.volumeId,
            format = try { MangaFormat.valueOf(route.format) } catch (_: Exception) { MangaFormat.UNKNOWN },
        )
    )
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    val preferences: StateFlow<UserPreferences> = preferencesDataStore.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    private val _events = MutableSharedFlow<ReaderEvent>()
    val events = _events.asSharedFlow()

    private var autoSaveJob: Job? = null
    private var debouncedSaveJob: Job? = null

    // Readium publication, kept as class property (not in StateFlow, not serializable)
    var publication: Publication? = null
        private set

    init {
        loadChapter()
        startAutoSave()
    }

    private fun loadChapter() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            var chapterInfo: com.kavita.core.model.repository.ChapterInfo? = null
            try {
                val info = readerRepository.getChapterInfo(route.chapterId)
                chapterInfo = info
                // Precomputar todas las URLs de paginas para evitar recalcularlas en cada recomposicion
                val precomputedUrls = (0 until info.pages).map { page ->
                    readerRepository.getPageImageUrl(route.chapterId, page)
                }
                _uiState.update {
                    it.copy(
                        chapterTitle = info.chapterTitle.ifBlank { info.seriesName },
                        totalPages = info.pages,
                        kavitaTotalPages = info.pages,
                        libraryId = info.libraryId,
                        pageUrls = precomputedUrls,
                    )
                }
            } catch (_: Exception) { }

            try {
                val progress = readerRepository.getProgress(route.chapterId)
                if (progress != null) {
                    _uiState.update {
                        // Para EPUB: bookScrollId contiene la posicion real de Readium
                        val restoredPage = if (it.format == MangaFormat.EPUB) {
                            progress.bookScrollId?.toIntOrNull() ?: progress.pageNum
                        } else {
                            progress.pageNum
                        }
                        val maxPage = (it.totalPages - 1).coerceAtLeast(0)
                        it.copy(currentPage = restoredPage.coerceIn(0, maxPage))
                    }
                }
            } catch (_: Exception) { }

            // Descargar fichero para PDF y EPUB
            val format = _uiState.value.format
            if (format == MangaFormat.PDF || format == MangaFormat.EPUB) {
                launch {
                    try {
                        val ext = if (format == MangaFormat.PDF) "pdf" else "epub"
                        val serverId = activeServerProvider.requireId()
                        val downloadDir = File(application.filesDir, "downloads/$serverId/${route.seriesId}")
                        downloadDir.mkdirs()

                        _uiState.update { it.copy(isDownloading = true) }
                        val file = readerRepository.downloadChapterFile(
                            chapterId = route.chapterId,
                            cacheDir = downloadDir,
                        ) { bytesDownloaded, totalBytes ->
                            _uiState.update {
                                it.copy(
                                    downloadBytesDownloaded = bytesDownloaded,
                                    downloadTotalBytes = totalBytes,
                                )
                            }
                        }
                        _uiState.update { it.copy(isDownloading = false) }

                        // Renombrar con extension correcta si es necesario
                        val targetFile = File(downloadDir, "chapter_${route.chapterId}.$ext")
                        if (file.absolutePath != targetFile.absolutePath) {
                            if (!file.renameTo(targetFile)) {
                                file.copyTo(targetFile, overwrite = true)
                                file.delete()
                            }
                        }

                        // Registrar descarga en la BD
                        val info = chapterInfo
                        downloadRepository.registerCompletedDownload(
                            chapterId = route.chapterId,
                            seriesId = route.seriesId,
                            serverId = serverId,
                            seriesName = info?.seriesName ?: "",
                            chapterName = info?.chapterTitle ?: "",
                            format = format.name,
                            filePath = targetFile.absolutePath,
                        )

                        if (format == MangaFormat.EPUB) {
                            val pub = readiumManager.openPublication(targetFile)
                            publication = pub
                            val toc = extractToc(pub)
                            _uiState.update { it.copy(pdfFile = targetFile, publicationReady = true, tableOfContents = toc) }
                        } else {
                            val pub = readiumManager.openPublication(targetFile)
                            publication = pub
                            val toc = extractToc(pub)
                            _uiState.update { it.copy(pdfFile = targetFile, tableOfContents = toc) }
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(isDownloading = false, error = "Error al abrir el documento: ${e.message}") }
                    }
                }
            }

            // Cargar siguiente/anterior capitulo
            launch {
                try {
                    val nextId = readerRepository.getNextChapterId(route.seriesId, route.volumeId, route.chapterId)
                    _uiState.update { it.copy(nextChapterId = if (nextId != null && nextId > 0) nextId else null) }
                } catch (_: Exception) { }
            }
            launch {
                try {
                    val prevId = readerRepository.getPrevChapterId(route.seriesId, route.volumeId, route.chapterId)
                    _uiState.update { it.copy(prevChapterId = if (prevId != null && prevId > 0) prevId else null) }
                } catch (_: Exception) { }
            }

            // Verificar bookmark
            launch {
                try {
                    val bookmarks = readerRepository.getBookmarks(route.chapterId)
                    _uiState.update { it.copy(isBookmarked = bookmarks.isNotEmpty()) }
                } catch (_: Exception) { }
            }

            _uiState.update { it.copy(isLoading = false) }

            // Iniciar sesion de lectura para estadisticas
            try {
                statsRepository.startSession(
                    chapterId = route.chapterId,
                    seriesId = route.seriesId,
                    serverId = activeServerProvider.requireId(),
                    format = _uiState.value.format.name,
                )
            } catch (_: Exception) { }
        }
    }

    fun onPageChanged(page: Int) {
        _uiState.update {
            val maxPage = (it.totalPages - 1).coerceAtLeast(0)
            it.copy(currentPage = page.coerceIn(0, maxPage))
        }
        // Registrar pagina leida para estadisticas
        viewModelScope.launch {
            try { statsRepository.recordPageRead() } catch (_: Exception) { }
        }
        // Guardar progreso con debounce (5s tras el ultimo cambio de pagina)
        debouncedSaveJob?.cancel()
        debouncedSaveJob = viewModelScope.launch {
            delay(5_000)
            saveProgress()
        }
    }

    fun onTotalPagesResolved(total: Int) {
        _uiState.update { it.copy(totalPages = total) }
        seriesRepository.setEpubPageCount(route.chapterId, total)
    }

    fun toggleControls() {
        _uiState.update { it.copy(showControls = !it.showControls) }
    }

    fun setBrightness(brightness: Float) {
        _uiState.update { it.copy(brightness = brightness) }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val state = _uiState.value
            val bookmark = Bookmark(
                chapterId = state.chapterId,
                seriesId = state.seriesId,
                volumeId = state.volumeId,
                serverId = activeServerProvider.requireId(),
                page = state.currentPage,
            )
            if (state.isBookmarked) {
                readerRepository.removeBookmark(bookmark)
            } else {
                readerRepository.addBookmark(bookmark)
            }
            _uiState.update { it.copy(isBookmarked = !it.isBookmarked) }
        }
    }

    fun goToNextChapter() {
        val state = _uiState.value
        val nextId = state.nextChapterId ?: return
        viewModelScope.launch {
            withContext(NonCancellable) { saveProgress() }
            _events.emit(
                ReaderEvent.NavigateToChapter(nextId, state.seriesId, state.volumeId, state.format.name)
            )
        }
    }

    fun goToPrevChapter() {
        val state = _uiState.value
        val prevId = state.prevChapterId ?: return
        viewModelScope.launch {
            withContext(NonCancellable) { saveProgress() }
            _events.emit(
                ReaderEvent.NavigateToChapter(prevId, state.seriesId, state.volumeId, state.format.name)
            )
        }
    }

    fun getPageImageUrl(page: Int): String {
        val urls = _uiState.value.pageUrls
        return if (page in urls.indices) urls[page]
        else readerRepository.getPageImageUrl(_uiState.value.chapterId, page)
    }

    fun getBookPageUrl(page: Int): String {
        return readerRepository.getBookPageUrl(_uiState.value.chapterId, page)
    }

    suspend fun getBookPageHtml(page: Int): String {
        return readerRepository.getBookPageHtml(_uiState.value.chapterId, page)
    }

    fun getBaseUrl(): String = readerRepository.getBaseUrl()

    fun updateReadingDirection(direction: ReadingDirection) {
        viewModelScope.launch {
            preferencesDataStore.updateReadingDirection(direction)
        }
    }

    fun updatePageLayout(layout: PageLayout) {
        viewModelScope.launch {
            preferencesDataStore.updatePageLayout(layout)
        }
    }

    fun updatePageScaleType(scaleType: PageScaleType) {
        viewModelScope.launch {
            preferencesDataStore.updatePageScaleType(scaleType)
        }
    }

    fun updatePageTransition(transition: PageTransition) {
        viewModelScope.launch {
            preferencesDataStore.updatePageTransition(transition)
        }
    }

    fun updateTapNavigation(tapNavigation: TapNavigation) {
        viewModelScope.launch {
            preferencesDataStore.updateTapNavigation(tapNavigation)
        }
    }

    fun updatePdfNightMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.updatePdfNightMode(enabled)
        }
    }

    fun updateEpubFontSize(size: Float) {
        viewModelScope.launch {
            preferencesDataStore.updateEpubFontSize(size)
        }
    }

    fun updateEpubFontFamily(family: String) {
        viewModelScope.launch {
            preferencesDataStore.updateEpubFontFamily(family)
        }
    }

    fun updateEpubLineSpacing(spacing: Float) {
        viewModelScope.launch {
            preferencesDataStore.updateEpubLineSpacing(spacing)
        }
    }

    fun updateEpubTheme(theme: ReaderTheme) {
        viewModelScope.launch {
            preferencesDataStore.updateEpubTheme(theme)
        }
    }

    private fun extractToc(pub: Publication): List<TocEntry> {
        val result = mutableListOf<TocEntry>()
        fun flatten(links: List<org.readium.r2.shared.publication.Link>, level: Int) {
            for (link in links) {
                result.add(TocEntry(
                    title = link.title ?: link.href.toString(),
                    href = link.href.toString(),
                    level = level,
                ))
                flatten(link.children, level + 1)
            }
        }
        flatten(pub.tableOfContents, 0)
        return result
    }

    fun navigateToTocEntry(entry: TocEntry) {
        viewModelScope.launch {
            _uiState.update { it.copy(showControls = false) }
            _events.emit(ReaderEvent.NavigateToToc(entry.href))
        }
    }

    private fun startAutoSave() {
        autoSaveJob = viewModelScope.launch {
            while (true) {
                delay(30_000)
                saveProgress()
            }
        }
    }

    private suspend fun saveProgress() {
        val state = _uiState.value
        if (state.totalPages == 0) return

        // Para EPUB: convertir posicion Readium al rango de paginas de Kavita
        val pageForServer = if (state.format == MangaFormat.EPUB &&
            state.kavitaTotalPages > 0 && state.totalPages > 0 &&
            state.kavitaTotalPages != state.totalPages
        ) {
            (state.currentPage.toLong() * state.kavitaTotalPages / state.totalPages)
                .toInt().coerceIn(0, state.kavitaTotalPages - 1)
        } else {
            state.currentPage
        }

        val progress = ReadingProgress(
            chapterId = state.chapterId,
            seriesId = state.seriesId,
            volumeId = state.volumeId,
            libraryId = state.libraryId,
            serverId = activeServerProvider.requireId(),
            pageNum = pageForServer,
            bookScrollId = if (state.format == MangaFormat.EPUB) state.currentPage.toString() else null,
        )
        readerRepository.saveProgress(progress)
        readerRepository.syncProgressToServer(progress.chapterId)
    }

    fun closeReader() {
        viewModelScope.launch {
            withContext(NonCancellable) {
                saveProgress()
                try { statsRepository.endSession() } catch (_: Exception) { }
            }
            publication?.close()
            _events.emit(ReaderEvent.CloseReader)
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoSaveJob?.cancel()
        debouncedSaveJob?.cancel()
        // Guardar progreso y cerrar sesion de forma no cancelable como red de seguridad
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + NonCancellable).launch {
            try { saveProgress() } catch (_: Exception) { }
            try { statsRepository.endSession() } catch (_: Exception) { }
            publication?.close()
        }
    }
}
