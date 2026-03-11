package com.kavita.feature.reader.comic

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.kavita.core.model.PageLayout
import com.kavita.core.model.PageScaleType
import com.kavita.core.model.PageTransition
import com.kavita.core.model.ReadingDirection
import com.kavita.core.model.TapNavigation
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.math.absoluteValue
import kotlin.math.ceil

/**
 * Precarga imagenes alrededor de la pagina actual usando Coil.
 * Precarga [aheadCount] paginas hacia delante y [behindCount] hacia atras.
 * Las paginas mas cercanas se precargan primero, priorizando la direccion de lectura.
 * Usa memoria cache solo para las mas proximas y disco para el resto.
 */
@Composable
private fun PreloadPages(
    currentPage: Int,
    totalPages: Int,
    pageUrls: List<String>,
    aheadCount: Int = 5,
    behindCount: Int = 2,
) {
    val context = LocalContext.current
    val imageLoader = context.imageLoader

    LaunchedEffect(currentPage) {
        // Generar lista de paginas a precargar ordenadas por proximidad
        val pagesToPreload = buildList {
            for (i in 1..aheadCount) {
                val nextPage = currentPage + i
                if (nextPage in pageUrls.indices) add(nextPage to i)
            }
            for (i in 1..behindCount) {
                val prevPage = currentPage - i
                if (prevPage in pageUrls.indices) add(prevPage to aheadCount + i)
            }
        }.sortedBy { it.second }

        for ((pageIndex, distance) in pagesToPreload) {
            // Solo cache en memoria las 3 paginas mas cercanas para ahorrar RAM
            val memoryPolicy = if (distance <= 3) CachePolicy.ENABLED else CachePolicy.DISABLED
            val request = ImageRequest.Builder(context)
                .data(pageUrls[pageIndex])
                .memoryCachePolicy(memoryPolicy)
                .diskCachePolicy(CachePolicy.ENABLED)
                .size(Size.ORIGINAL)
                .build()
            imageLoader.enqueue(request)
        }
    }
}

@Composable
fun ComicReader(
    totalPages: Int,
    currentPage: Int,
    readingDirection: ReadingDirection,
    pageLayout: PageLayout,
    pageScaleType: PageScaleType,
    pageTransition: PageTransition,
    tapNavigation: TapNavigation,
    getPageImageUrl: (Int) -> String,
    onPageChanged: (Int) -> Unit,
    onTapCenter: () -> Unit,
    pageUrls: List<String> = emptyList(),
) {
    if (totalPages == 0) return

    // Precarga de imagenes con Coil si las URLs estan precomputadas
    if (pageUrls.isNotEmpty()) {
        PreloadPages(
            currentPage = currentPage,
            totalPages = totalPages,
            pageUrls = pageUrls,
        )
    }

    // Webtoon siempre es scroll continuo
    if (readingDirection == ReadingDirection.WEBTOON) {
        WebtoonReader(
            totalPages = totalPages,
            currentPage = currentPage,
            pageUrls = pageUrls,
            getPageImageUrl = getPageImageUrl,
            onPageChanged = onPageChanged,
            onTapCenter = onTapCenter,
        )
        return
    }

    val isDouble = pageLayout == PageLayout.DOUBLE
    val effectivePageCount = if (isDouble) ceil(totalPages / 2.0).toInt() else totalPages
    val initialPage = if (isDouble) currentPage / 2 else currentPage

    when (readingDirection) {
        ReadingDirection.VERTICAL -> {
            VerticalPagedReader(
                effectivePageCount = effectivePageCount,
                initialPage = initialPage,
                isDouble = isDouble,
                totalPages = totalPages,
                pageScaleType = pageScaleType,
                pageTransition = pageTransition,
                tapNavigation = tapNavigation,
                getPageImageUrl = getPageImageUrl,
                onPageChanged = onPageChanged,
                onTapCenter = onTapCenter,
            )
        }
        else -> {
            val reverseLayout = readingDirection == ReadingDirection.RIGHT_TO_LEFT
            HorizontalPagedReader(
                effectivePageCount = effectivePageCount,
                initialPage = initialPage,
                reverseLayout = reverseLayout,
                isDouble = isDouble,
                totalPages = totalPages,
                pageScaleType = pageScaleType,
                pageTransition = pageTransition,
                tapNavigation = tapNavigation,
                getPageImageUrl = getPageImageUrl,
                onPageChanged = onPageChanged,
                onTapCenter = onTapCenter,
            )
        }
    }
}

@Composable
private fun HorizontalPagedReader(
    effectivePageCount: Int,
    initialPage: Int,
    reverseLayout: Boolean,
    isDouble: Boolean,
    totalPages: Int,
    pageScaleType: PageScaleType,
    pageTransition: PageTransition,
    tapNavigation: TapNavigation,
    getPageImageUrl: (Int) -> String,
    onPageChanged: (Int) -> Unit,
    onTapCenter: () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { effectivePageCount },
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onPageChanged(if (isDouble) page * 2 else page)
        }
    }

    HorizontalPager(
        state = pagerState,
        reverseLayout = reverseLayout,
        beyondViewportPageCount = 3,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        val pageModifier = Modifier
            .fillMaxSize()
            .pageTransitionEffect(pagerState, page, pageTransition)
            .pointerInput(tapNavigation) {
                detectTapGestures { offset ->
                    val width = size.width
                    val height = size.height
                    when (tapNavigation) {
                        TapNavigation.LATERAL -> {
                            val zone = width / 3
                            when {
                                offset.x < zone -> scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                                offset.x > width - zone -> scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                                else -> onTapCenter()
                            }
                        }
                        TapNavigation.VERTICAL -> {
                            val zone = height / 3
                            when {
                                offset.y < zone -> scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                                offset.y > height - zone -> scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                                else -> onTapCenter()
                            }
                        }
                        TapNavigation.NONE -> {
                            val zone = width / 3
                            if (offset.x > zone && offset.x < width - zone) {
                                onTapCenter()
                            }
                        }
                    }
                }
            }

        if (isDouble) {
            DoublePageContent(
                spreadIndex = page,
                totalPages = totalPages,
                pageScaleType = pageScaleType,
                getPageImageUrl = getPageImageUrl,
                modifier = pageModifier,
            )
        } else {
            SinglePageContent(
                pageIndex = page,
                pageScaleType = pageScaleType,
                getPageImageUrl = getPageImageUrl,
                modifier = pageModifier,
            )
        }
    }
}

@Composable
private fun VerticalPagedReader(
    effectivePageCount: Int,
    initialPage: Int,
    isDouble: Boolean,
    totalPages: Int,
    pageScaleType: PageScaleType,
    pageTransition: PageTransition,
    tapNavigation: TapNavigation,
    getPageImageUrl: (Int) -> String,
    onPageChanged: (Int) -> Unit,
    onTapCenter: () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { effectivePageCount },
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onPageChanged(if (isDouble) page * 2 else page)
        }
    }

    VerticalPager(
        state = pagerState,
        beyondViewportPageCount = 3,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        val pageModifier = Modifier
            .fillMaxSize()
            .verticalPageTransitionEffect(pagerState, page, pageTransition)
            .pointerInput(tapNavigation) {
                detectTapGestures { offset ->
                    val width = size.width
                    val height = size.height
                    when (tapNavigation) {
                        TapNavigation.LATERAL -> {
                            val zone = width / 3
                            when {
                                offset.x < zone -> scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                                offset.x > width - zone -> scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                                else -> onTapCenter()
                            }
                        }
                        TapNavigation.VERTICAL -> {
                            val zone = height / 3
                            when {
                                offset.y < zone -> scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                                offset.y > height - zone -> scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                                else -> onTapCenter()
                            }
                        }
                        TapNavigation.NONE -> {
                            val zone = height / 3
                            if (offset.y > zone && offset.y < height - zone) {
                                onTapCenter()
                            }
                        }
                    }
                }
            }

        if (isDouble) {
            DoublePageContent(
                spreadIndex = page,
                totalPages = totalPages,
                pageScaleType = pageScaleType,
                getPageImageUrl = getPageImageUrl,
                modifier = pageModifier,
            )
        } else {
            SinglePageContent(
                pageIndex = page,
                pageScaleType = pageScaleType,
                getPageImageUrl = getPageImageUrl,
                modifier = pageModifier,
            )
        }
    }
}

@Composable
private fun SinglePageContent(
    pageIndex: Int,
    pageScaleType: PageScaleType,
    getPageImageUrl: (Int) -> String,
    modifier: Modifier = Modifier,
) {
    ZoomablePageImage(
        imageUrl = getPageImageUrl(pageIndex),
        contentDescription = "Pagina ${pageIndex + 1}",
        contentScale = pageScaleType.toContentScale(),
        modifier = modifier,
    )
}

@Composable
private fun DoublePageContent(
    spreadIndex: Int,
    totalPages: Int,
    pageScaleType: PageScaleType,
    getPageImageUrl: (Int) -> String,
    modifier: Modifier = Modifier,
) {
    val leftPage = spreadIndex * 2
    val rightPage = leftPage + 1
    val scale = pageScaleType.toContentScale()

    Row(modifier = modifier) {
        AsyncImage(
            model = getPageImageUrl(leftPage),
            contentDescription = "Pagina ${leftPage + 1}",
            contentScale = scale,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        if (rightPage < totalPages) {
            AsyncImage(
                model = getPageImageUrl(rightPage),
                contentDescription = "Pagina ${rightPage + 1}",
                contentScale = scale,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun WebtoonReader(
    totalPages: Int,
    currentPage: Int,
    pageUrls: List<String>,
    getPageImageUrl: (Int) -> String,
    onPageChanged: (Int) -> Unit,
    onTapCenter: () -> Unit,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentPage)

    val currentVisiblePage by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }

    LaunchedEffect(currentVisiblePage) {
        onPageChanged(currentVisiblePage)
    }

    // Precarga de imagenes para scroll continuo (webtoon)
    if (pageUrls.isNotEmpty()) {
        PreloadPages(
            currentPage = currentVisiblePage,
            totalPages = totalPages,
            pageUrls = pageUrls,
            aheadCount = 7,  // Mas precarga para scroll continuo
            behindCount = 2,
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val height = size.height
                    val centerZone = height / 3
                    if (offset.y > centerZone && offset.y < height - centerZone) {
                        onTapCenter()
                    }
                }
            },
    ) {
        items(totalPages) { page ->
            AsyncImage(
                model = getPageImageUrl(page),
                contentDescription = "Pagina ${page + 1}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// --- Efectos de transicion ---

private fun Modifier.pageTransitionEffect(
    pagerState: PagerState,
    page: Int,
    transition: PageTransition,
): Modifier = when (transition) {
    PageTransition.SLIDE -> this // Comportamiento por defecto del pager
    PageTransition.CURL -> this.graphicsLayer {
        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
        // Efecto de giro 3D tipo pasar hoja
        cameraDistance = 12f * density
        rotationY = lerp(0f, -90f, pageOffset.coerceIn(0f, 1f))
        // Fade ligero para suavizar
        alpha = lerp(1f, 0.3f, pageOffset.coerceIn(0f, 1f))
    }
    PageTransition.FADE -> this.graphicsLayer {
        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
        alpha = lerp(1f, 0f, pageOffset.coerceIn(0f, 1f))
        // Mantener en su sitio (sin deslizamiento visual)
        translationX = size.width * pagerState.currentPageOffsetFraction * if (page < pagerState.currentPage) 1f else -1f
    }
}

private fun Modifier.verticalPageTransitionEffect(
    pagerState: PagerState,
    page: Int,
    transition: PageTransition,
): Modifier = when (transition) {
    PageTransition.SLIDE -> this
    PageTransition.CURL -> this.graphicsLayer {
        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
        cameraDistance = 12f * density
        rotationX = lerp(0f, 90f, pageOffset.coerceIn(0f, 1f))
        alpha = lerp(1f, 0.3f, pageOffset.coerceIn(0f, 1f))
    }
    PageTransition.FADE -> this.graphicsLayer {
        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
        alpha = lerp(1f, 0f, pageOffset.coerceIn(0f, 1f))
        translationY = size.height * pagerState.currentPageOffsetFraction * if (page < pagerState.currentPage) 1f else -1f
    }
}

// --- Utilidades ---

private fun PageScaleType.toContentScale(): ContentScale = when (this) {
    PageScaleType.FIT_SCREEN -> ContentScale.Fit
    PageScaleType.FIT_WIDTH -> ContentScale.FillWidth
    PageScaleType.FIT_HEIGHT -> ContentScale.FillHeight
}
