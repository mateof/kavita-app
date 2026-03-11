package com.kavita.feature.library.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kavita.core.common.navigation.SeriesDetailRoute
import com.kavita.core.model.Collection
import com.kavita.core.model.DownloadStatus
import com.kavita.core.model.DownloadTask
import com.kavita.core.model.ReadingList
import com.kavita.core.model.Series
import com.kavita.core.model.SeriesDetail
import com.kavita.core.model.SeriesMetadata
import com.kavita.core.model.repository.CollectionRepository
import com.kavita.core.model.repository.DownloadRepository
import com.kavita.core.model.repository.ReadingListRepository
import com.kavita.core.model.repository.ReaderRepository
import com.kavita.core.model.repository.SeriesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SeriesDetailUiState(
    val series: Series? = null,
    val detail: SeriesDetail? = null,
    val metadata: SeriesMetadata? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class SeriesDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val seriesRepository: SeriesRepository,
    private val downloadRepository: DownloadRepository,
    private val readerRepository: ReaderRepository,
    private val collectionRepository: CollectionRepository,
    private val readingListRepository: ReadingListRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<SeriesDetailRoute>()
    val seriesId = route.seriesId
    val serverId = route.serverId

    private val _uiState = MutableStateFlow(SeriesDetailUiState())
    val uiState: StateFlow<SeriesDetailUiState> = _uiState.asStateFlow()

    val downloadedChapterIds: StateFlow<Set<Int>> =
        downloadRepository.observeDownloadedChapterIds(seriesId, serverId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val downloadTasks: StateFlow<List<DownloadTask>> =
        downloadRepository.observeDownloads()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _snackbar = MutableSharedFlow<String>()
    val snackbar: SharedFlow<String> = _snackbar.asSharedFlow()

    init {
        loadDetail()
    }

    fun refresh() {
        loadDetail()
    }

    private fun loadDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val series = seriesRepository.getSeriesDetail(seriesId)
                _uiState.update { it.copy(series = series) }
            } catch (_: Exception) { }

            try {
                val detail = seriesRepository.getSeriesDetailInfo(seriesId)
                // Aplicar conteo real de paginas EPUB si esta disponible
                val adjustedDetail = applyEpubPageCounts(detail)
                _uiState.update { it.copy(detail = adjustedDetail) }
            } catch (_: Exception) { }

            try {
                val meta = seriesRepository.getSeriesMetadata(seriesId)
                _uiState.update { it.copy(metadata = meta) }
            } catch (_: Exception) { }

            // Actualizar pages de la serie con totales reales EPUB
            adjustSeriesTotalPages()

            if (_uiState.value.series == null && _uiState.value.detail == null) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Error al cargar los detalles")
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun applyEpubPageCounts(detail: SeriesDetail): SeriesDetail {
        suspend fun adjustChapter(ch: com.kavita.core.model.Chapter): com.kavita.core.model.Chapter {
            val realPages = seriesRepository.getEpubPageCount(ch.id) ?: return ch
            if (realPages == ch.pages) return ch
            // Leer posicion real de Readium desde progreso local (bookScrollId)
            val localProgress = try {
                readerRepository.getProgress(ch.id)
            } catch (_: Exception) { null }
            val adjustedRead = localProgress?.bookScrollId?.toIntOrNull()
                ?: if (ch.pages > 0) {
                    (ch.pagesRead.toLong() * realPages / ch.pages).toInt().coerceIn(0, realPages)
                } else ch.pagesRead
            return ch.copy(pages = realPages, pagesRead = adjustedRead.coerceIn(0, realPages))
        }

        return detail.copy(
            specials = detail.specials.map { adjustChapter(it) },
            chapters = detail.chapters.map { adjustChapter(it) },
            storylineChapters = detail.storylineChapters.map { adjustChapter(it) },
            volumes = detail.volumes.map { vol ->
                val adjustedChapters = vol.chapters.map { adjustChapter(it) }
                vol.copy(
                    chapters = adjustedChapters,
                    pages = adjustedChapters.sumOf { it.pages },
                    pagesRead = adjustedChapters.sumOf { it.pagesRead },
                )
            },
        )
    }

    private fun adjustSeriesTotalPages() {
        val detail = _uiState.value.detail ?: return
        val series = _uiState.value.series ?: return
        val allChapters = detail.storylineChapters + detail.chapters + detail.specials +
            detail.volumes.flatMap { it.chapters }
        val totalPages = allChapters.sumOf { it.pages }
        val totalRead = allChapters.sumOf { it.pagesRead }
        if (totalPages != series.pages || totalRead != series.pagesRead) {
            _uiState.update {
                it.copy(series = series.copy(pages = totalPages, pagesRead = totalRead))
            }
        }
    }

    fun getContinueReadingChapter(): Pair<Int, Int>? {
        val detail = _uiState.value.detail ?: return null
        // Check storyline chapters first (reading order)
        for (chapter in detail.storylineChapters) {
            if (chapter.pagesRead < chapter.pages) {
                return chapter.id to chapter.volumeId
            }
        }
        // Then volumes
        for (volume in detail.volumes) {
            for (chapter in volume.chapters) {
                if (chapter.pagesRead < chapter.pages) {
                    return chapter.id to volume.id
                }
            }
        }
        // Then loose chapters
        for (chapter in detail.chapters) {
            if (chapter.pagesRead < chapter.pages) {
                return chapter.id to chapter.volumeId
            }
        }
        // Then specials
        for (chapter in detail.specials) {
            if (chapter.pagesRead < chapter.pages) {
                return chapter.id to chapter.volumeId
            }
        }
        // Fallback: first available chapter
        val first = detail.storylineChapters.firstOrNull()
            ?: detail.volumes.firstOrNull()?.chapters?.firstOrNull()
            ?: detail.chapters.firstOrNull()
            ?: detail.specials.firstOrNull()
            ?: return null
        return first.id to first.volumeId
    }

    fun markAsRead() {
        viewModelScope.launch {
            seriesRepository.markSeriesAsRead(seriesId)
            loadDetail()
        }
    }

    fun markAsUnread() {
        viewModelScope.launch {
            seriesRepository.markSeriesAsUnread(seriesId)
            loadDetail()
        }
    }

    fun downloadChapter(chapterId: Int, chapterName: String, format: String) {
        val series = _uiState.value.series ?: return
        viewModelScope.launch {
            try {
                downloadRepository.enqueueDownload(
                    chapterId = chapterId,
                    seriesId = seriesId,
                    serverId = serverId,
                    seriesName = series.name,
                    chapterName = chapterName,
                    format = format,
                )
            } catch (_: Exception) {
                _snackbar.emit("Error")
            }
        }
    }

    fun deleteChapterDownload(chapterId: Int) {
        viewModelScope.launch {
            val tasks = downloadTasks.value
            val task = tasks.firstOrNull { it.chapterId == chapterId && it.serverId == serverId }
            if (task != null) {
                downloadRepository.deleteDownload(task.id)
            }
        }
    }

    fun exportChapter(chapterId: Int, chapterName: String, format: String, fileName: String) {
        val series = _uiState.value.series ?: return
        viewModelScope.launch {
            try {
                // Si ya esta descargado, exportar directamente
                if (downloadRepository.isChapterDownloaded(chapterId, serverId)) {
                    val success = downloadRepository.exportToDownloads(chapterId, serverId, fileName)
                    _snackbar.emit(if (success) "OK_EXPORT" else "ERR_EXPORT")
                } else {
                    // Descargar y exportar en un solo paso
                    val success = downloadRepository.downloadAndExport(
                        chapterId = chapterId,
                        seriesId = seriesId,
                        serverId = serverId,
                        seriesName = series.name,
                        chapterName = chapterName,
                        format = format,
                        fileName = fileName,
                    )
                    _snackbar.emit(if (success) "OK_EXPORT" else "ERR_EXPORT")
                }
            } catch (_: Exception) {
                _snackbar.emit("ERR_EXPORT")
            }
        }
    }

    // --- Collections & Reading Lists ---

    private val _collections = MutableStateFlow<List<Collection>>(emptyList())
    val collections: StateFlow<List<Collection>> = _collections.asStateFlow()

    private val _readingLists = MutableStateFlow<List<ReadingList>>(emptyList())
    val readingLists: StateFlow<List<ReadingList>> = _readingLists.asStateFlow()

    fun loadCollections() {
        viewModelScope.launch {
            try {
                _collections.value = collectionRepository.getCollections()
            } catch (_: Exception) { }
        }
    }

    fun loadReadingLists() {
        viewModelScope.launch {
            try {
                _readingLists.value = readingListRepository.getReadingLists()
            } catch (_: Exception) { }
        }
    }

    fun addToCollection(collectionId: Int) {
        viewModelScope.launch {
            try {
                collectionRepository.addSeriesToCollection(collectionId, listOf(seriesId))
                _snackbar.emit("OK_COLLECTION")
            } catch (_: Exception) {
                _snackbar.emit("ERR_COLLECTION")
            }
        }
    }

    fun createCollectionAndAdd(title: String) {
        viewModelScope.launch {
            try {
                collectionRepository.createCollectionWithSeries(title, listOf(seriesId))
                _snackbar.emit("OK_COLLECTION")
            } catch (_: Exception) {
                _snackbar.emit("ERR_COLLECTION")
            }
        }
    }

    fun addToReadingList(readingListId: Int) {
        viewModelScope.launch {
            try {
                readingListRepository.addSeriesToReadingList(readingListId, listOf(seriesId))
                _snackbar.emit("OK_READING_LIST")
            } catch (_: Exception) {
                _snackbar.emit("ERR_READING_LIST")
            }
        }
    }

    fun createReadingListAndAdd(title: String) {
        viewModelScope.launch {
            try {
                val list = readingListRepository.createReadingList(title)
                readingListRepository.addSeriesToReadingList(list.id, listOf(seriesId))
                _snackbar.emit("OK_READING_LIST")
            } catch (_: Exception) {
                _snackbar.emit("ERR_READING_LIST")
            }
        }
    }
}
