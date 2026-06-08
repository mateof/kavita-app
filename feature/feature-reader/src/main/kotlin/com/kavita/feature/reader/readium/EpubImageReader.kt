package com.kavita.feature.reader.readium

import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import com.kavita.core.model.PageScaleType
import com.kavita.core.model.ReadingDirection
import com.kavita.core.model.TapNavigation
import com.kavita.feature.reader.comic.ZoomablePageImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.util.Url

/**
 * Lector para EPUB basados en imágenes (cómic / fixed-layout). Readium renderiza estos
 * EPUB como reflowable cuando no declaran bien el fixed-layout, dejando la imagen pequeña
 * y anclada arriba-izquierda, y sin gesto de arrastre. Aquí extraemos la imagen de cada
 * página del propio EPUB y la mostramos con un HorizontalPager + pinch-zoom (reutilizando
 * [ZoomablePageImage]), igual que el lector de cómics.
 */
@OptIn(ExperimentalReadiumApi::class)
@Composable
fun EpubImageReader(
    publication: Publication,
    currentPage: Int,
    readingDirection: ReadingDirection,
    pageScaleType: PageScaleType,
    tapNavigation: TapNavigation,
    onPageChanged: (Int) -> Unit,
    onTotalPagesResolved: ((Int) -> Unit)? = null,
    onTapCenter: () -> Unit,
) {
    val pages = remember(publication) { publication.readingOrder }
    val pageCount = pages.size

    LaunchedEffect(pageCount) { onTotalPagesResolved?.invoke(pageCount) }
    if (pageCount == 0) return

    val reverseLayout = readingDirection == ReadingDirection.RIGHT_TO_LEFT
    val pagerState = rememberPagerState(
        initialPage = currentPage.coerceIn(0, pageCount - 1),
        pageCount = { pageCount },
    )
    val scope = rememberCoroutineScope()

    // Reportar la página visible al ViewModel
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page -> onPageChanged(page) }
    }
    // Navegación externa (slider de página / prev-next)
    LaunchedEffect(currentPage) {
        val target = currentPage.coerceIn(0, pageCount - 1)
        if (target != pagerState.currentPage) pagerState.scrollToPage(target)
    }

    HorizontalPager(
        state = pagerState,
        reverseLayout = reverseLayout,
        beyondViewportPageCount = 2,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        // Un único manejador de tap (navegación/centro). El doble-tap de zoom lo gestiona
        // ZoomablePageImage en el mismo detector, para que no compitan dos detectores.
        val onTap: (Offset, IntSize) -> Unit = { offset, size ->
            handleReaderTap(
                offset = offset,
                size = size,
                tapNavigation = tapNavigation,
                onCenter = onTapCenter,
                onPrev = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                onNext = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
            )
        }

        EpubImagePage(
            publication = publication,
            link = pages[page],
            contentScale = pageScaleType.toContentScale(),
            pageNumber = page + 1,
            onTap = onTap,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun handleReaderTap(
    offset: Offset,
    size: IntSize,
    tapNavigation: TapNavigation,
    onCenter: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val width = size.width
    val height = size.height
    when (tapNavigation) {
        TapNavigation.LATERAL -> {
            val zone = width / 3
            when {
                offset.x < zone -> onPrev()
                offset.x > width - zone -> onNext()
                else -> onCenter()
            }
        }
        TapNavigation.VERTICAL -> {
            val zone = height / 3
            when {
                offset.y < zone -> onPrev()
                offset.y > height - zone -> onNext()
                else -> onCenter()
            }
        }
        TapNavigation.NONE -> {
            val zone = width / 3
            if (offset.x > zone && offset.x < width - zone) onCenter()
        }
    }
}

@OptIn(ExperimentalReadiumApi::class)
@Composable
private fun EpubImagePage(
    publication: Publication,
    link: Link,
    contentScale: ContentScale,
    pageNumber: Int,
    onTap: (Offset, IntSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    var model by remember(link) { mutableStateOf<Any?>(null) }
    var failed by remember(link) { mutableStateOf(false) }

    LaunchedEffect(link) {
        val bytes = withContext(Dispatchers.IO) { publication.imageBytesForPage(link) }
        if (bytes != null) model = bytes else failed = true
    }

    val current = model
    when {
        current != null -> ZoomablePageImage(
            model = current,
            contentDescription = "Pagina $pageNumber",
            contentScale = contentScale,
            onTap = onTap,
            modifier = modifier,
        )
        else -> Box(
            modifier = modifier.pointerInput(Unit) {
                detectTapGestures { offset -> onTap(offset, size) }
            },
            contentAlignment = Alignment.Center,
        ) {
            if (failed) Text("No se pudo cargar la página $pageNumber")
            else CircularProgressIndicator()
        }
    }
}

// --- Detección y extracción de imágenes del EPUB ---

private const val TAG = "EpubImageReader"

// Referencia a un fichero de imagen, ya sea en <img src>, SVG <image xlink:href>, o CSS url(...)
private val IMAGE_REF = Regex(
    """(?:src|href|xlink:href)\s*=\s*["']([^"']+\.(?:jpe?g|png|webp|gif|bmp|avif))["']""",
    RegexOption.IGNORE_CASE,
)
private val CSS_IMAGE_URL = Regex(
    """url\(\s*["']?([^"')]+\.(?:jpe?g|png|webp|gif|bmp|avif))["']?\s*\)""",
    RegexOption.IGNORE_CASE,
)
private val HTML_TAGS = Regex("(?s)<[^>]+>")
private val HTML_ENTITIES = Regex("&[a-zA-Z#0-9]+;")
private val WHITESPACE = Regex("\\s+")

private fun findImageRef(html: String): String? =
    IMAGE_REF.find(html)?.groupValues?.getOrNull(1)
        ?: CSS_IMAGE_URL.find(html)?.groupValues?.getOrNull(1)

/**
 * Decide si un EPUB debe tratarse como cómic/imágenes. Cubre tres casos:
 * 1. Declara fixed-layout en su metadata.
 * 2. El reading order ya son recursos de imagen (estilo Divina).
 * 3. Las páginas de contenido son XHTML que básicamente envuelven una sola imagen (cómics
 *    exportados como EPUB reflowable que Readium no escala bien).
 *
 * Para el caso 3 muestreamos páginas repartidas por el interior del documento (saltando las
 * primeras, que suelen ser portada/título/créditos con texto real) y decidimos por mayoría.
 */
@OptIn(ExperimentalReadiumApi::class)
suspend fun Publication.isImageBasedEpub(): Boolean = withContext(Dispatchers.IO) {
    val ro = readingOrder
    if (ro.isEmpty()) return@withContext false

    val layout = metadata.presentation.layout
    if (layout == EpubLayout.FIXED) {
        Log.d(TAG, "isImageBasedEpub=true (metadata layout=FIXED)")
        return@withContext true
    }
    if (ro.all { it.mediaType?.isBitmap == true }) {
        Log.d(TAG, "isImageBasedEpub=true (reading order de imágenes)")
        return@withContext true
    }

    // Muestrear hasta 8 páginas repartidas, saltando las 2 primeras
    val body = if (ro.size > 4) ro.subList(2, ro.size) else ro
    val step = maxOf(1, body.size / 8)
    val sample = body.filterIndexed { index, _ -> index % step == 0 }.take(8)
    if (sample.isEmpty()) return@withContext false

    var readable = 0
    var imageLike = 0
    var firstSnippet = ""
    for (link in sample) {
        if (link.mediaType?.isBitmap == true) {
            readable++
            imageLike++
            continue
        }
        val html = readBytes(link)?.decodeToString() ?: continue
        readable++
        if (firstSnippet.isEmpty()) firstSnippet = html.take(400)
        if (looksLikeSingleImagePage(html)) imageLike++
    }

    val ratio = if (readable > 0) imageLike.toFloat() / readable else 0f
    val result = readable > 0 && ratio >= 0.6f
    Log.d(
        TAG,
        "isImageBasedEpub=$result (layout=$layout, readingOrder=${ro.size}, " +
            "muestra=$readable, imageLike=$imageLike, ratio=$ratio)\nsnippet=$firstSnippet",
    )
    result
}

private fun looksLikeSingleImagePage(html: String): Boolean {
    if (findImageRef(html) == null) return false
    val text = html
        .replace(HTML_TAGS, " ")
        .replace(HTML_ENTITIES, " ")
        .replace(WHITESPACE, " ")
        .trim()
    return text.length < 60
}

@OptIn(ExperimentalReadiumApi::class)
private suspend fun Publication.imageBytesForPage(link: Link): ByteArray? {
    val url = imageUrlForPage(link) ?: return null
    return readBytes(url)
}

@OptIn(ExperimentalReadiumApi::class)
private suspend fun Publication.imageUrlForPage(link: Link): Url? {
    if (link.mediaType?.isBitmap == true) return link.url()
    val html = readBytes(link)?.decodeToString() ?: return null
    val raw = findImageRef(html)
    if (raw == null) {
        Log.w(TAG, "Sin imagen en página ${link.url()}\nsnippet=${html.take(400)}")
        return null
    }
    val relative = Url(raw.trim()) ?: return null
    return link.url().resolve(relative)
}

@OptIn(ExperimentalReadiumApi::class)
private suspend fun Publication.readBytes(link: Link): ByteArray? {
    val resource = get(link) ?: return null
    return try {
        resource.read().getOrNull()
    } finally {
        resource.close()
    }
}

@OptIn(ExperimentalReadiumApi::class)
private suspend fun Publication.readBytes(url: Url): ByteArray? {
    val resource = get(url) ?: return null
    return try {
        resource.read().getOrNull()
    } finally {
        resource.close()
    }
}

private fun PageScaleType.toContentScale(): ContentScale = when (this) {
    PageScaleType.FIT_SCREEN -> ContentScale.Fit
    PageScaleType.FIT_WIDTH -> ContentScale.FillWidth
    PageScaleType.FIT_HEIGHT -> ContentScale.FillHeight
}
