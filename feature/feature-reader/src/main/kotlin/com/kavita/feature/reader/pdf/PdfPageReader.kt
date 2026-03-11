package com.kavita.feature.reader.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.util.lerp
import com.kavita.core.model.PageLayout
import com.kavita.core.model.PageScaleType
import com.kavita.core.model.PageTransition
import com.kavita.core.model.ReadingDirection
import com.kavita.core.model.TapNavigation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.absoluteValue
import kotlin.math.ceil

@Composable
fun PdfPageReader(
    file: File,
    currentPage: Int,
    pageLayout: PageLayout,
    pageScaleType: PageScaleType,
    pageTransition: PageTransition,
    tapNavigation: TapNavigation,
    readingDirection: ReadingDirection,
    nightMode: Boolean = false,
    onPageChanged: (Int) -> Unit,
    onTapCenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fileDescriptor = remember(file) {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }
    val renderer = remember(fileDescriptor) {
        PdfRenderer(fileDescriptor)
    }
    val renderMutex = remember { Mutex() }

    DisposableEffect(renderer) {
        onDispose {
            renderer.close()
            fileDescriptor.close()
        }
    }

    val totalPages = renderer.pageCount
    if (totalPages == 0) return

    val isDouble = pageLayout == PageLayout.DOUBLE
    val effectivePageCount = if (isDouble) ceil(totalPages / 2.0).toInt() else totalPages
    val initialPage = if (isDouble) currentPage / 2 else currentPage
    val reverseLayout = readingDirection == ReadingDirection.RIGHT_TO_LEFT

    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, (effectivePageCount - 1).coerceAtLeast(0)),
        pageCount = { effectivePageCount },
    )
    val scope = rememberCoroutineScope()

    // Sync pager position when currentPage changes externally (e.g. progress restore, slider)
    LaunchedEffect(currentPage) {
        val targetPage = if (isDouble) currentPage / 2 else currentPage
        val coerced = targetPage.coerceIn(0, (effectivePageCount - 1).coerceAtLeast(0))
        if (pagerState.currentPage != coerced) {
            pagerState.scrollToPage(coerced)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onPageChanged(if (isDouble) page * 2 else page)
        }
    }

    HorizontalPager(
        state = pagerState,
        reverseLayout = reverseLayout,
        beyondViewportPageCount = 1,
        userScrollEnabled = true,
        modifier = modifier.fillMaxSize(),
    ) { page ->
        var scale by remember { mutableFloatStateOf(1f) }
        var zoomOffset by remember { mutableStateOf(Offset.Zero) }

        // Reset zoom when page changes
        LaunchedEffect(pagerState.currentPage) {
            if (pagerState.currentPage != page) {
                scale = 1f
                zoomOffset = Offset.Zero
            }
        }

        val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(1f, 5f)
            val newOffset = if (newScale > 1f) {
                zoomOffset + panChange
            } else {
                Offset.Zero
            }
            scale = newScale
            zoomOffset = newOffset
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pageTransitionEffect(pagerState, page, pageTransition)
                // Pinch-to-zoom: observe with Initial pass so single-finger passes through to pager
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var prevDist = 0f
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.size >= 2) {
                                val dist = (pressed[0].position - pressed[1].position).getDistance()
                                if (prevDist > 0f && dist > 0f) {
                                    val zoomChange = dist / prevDist
                                    scale = (scale * zoomChange).coerceIn(1f, 5f)
                                }
                                prevDist = dist
                                // Consume to prevent pager from scrolling during pinch
                                pressed.forEach { it.consume() }
                            } else {
                                prevDist = 0f
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                // When zoomed: transformable for pan + continued pinch zoom
                .then(
                    if (scale > 1.01f) Modifier.transformable(state = transformableState)
                    else Modifier
                )
                .pointerInput(tapNavigation, scale) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1.1f) {
                                scale = 1f
                                zoomOffset = Offset.Zero
                            } else {
                                scale = 2.5f
                            }
                        },
                        onTap = { offset ->
                            // Only navigate by tap when not zoomed in
                            if (scale > 1.1f) return@detectTapGestures

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
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            val zoomModifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = zoomOffset.x,
                    translationY = zoomOffset.y,
                )

            if (isDouble) {
                val leftIdx = page * 2
                val rightIdx = leftIdx + 1
                Row(modifier = zoomModifier) {
                    PdfPageBitmap(
                        renderer = renderer,
                        renderMutex = renderMutex,
                        pageIndex = leftIdx,
                        pageScaleType = pageScaleType,
                        nightMode = nightMode,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    )
                    if (rightIdx < totalPages) {
                        PdfPageBitmap(
                            renderer = renderer,
                            renderMutex = renderMutex,
                            pageIndex = rightIdx,
                            pageScaleType = pageScaleType,
                            nightMode = nightMode,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                        )
                    }
                }
            } else {
                PdfPageBitmap(
                    renderer = renderer,
                    renderMutex = renderMutex,
                    pageIndex = page,
                    pageScaleType = pageScaleType,
                    nightMode = nightMode,
                    modifier = zoomModifier,
                )
            }
        }
    }
}

@Composable
private fun PdfPageBitmap(
    renderer: PdfRenderer,
    renderMutex: Mutex,
    pageIndex: Int,
    pageScaleType: PageScaleType,
    nightMode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val bitmapState = remember(pageIndex) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex) {
        withContext(Dispatchers.IO) {
            renderMutex.withLock {
                val page = renderer.openPage(pageIndex)
                val scale = 2
                val bmp = Bitmap.createBitmap(
                    page.width * scale,
                    page.height * scale,
                    Bitmap.Config.ARGB_8888,
                )
                bmp.eraseColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmapState.value = bmp
            }
        }
    }

    val bitmap = bitmapState.value ?: return

    val contentScale = when (pageScaleType) {
        PageScaleType.FIT_SCREEN -> ContentScale.Fit
        PageScaleType.FIT_WIDTH -> ContentScale.FillWidth
        PageScaleType.FIT_HEIGHT -> ContentScale.FillHeight
    }

    val invertColorFilter = if (nightMode) {
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f,
                )
            )
        )
    } else {
        null
    }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Pagina ${pageIndex + 1}",
        contentScale = contentScale,
        colorFilter = invertColorFilter,
        modifier = modifier,
    )
}

private fun Modifier.pageTransitionEffect(
    pagerState: PagerState,
    page: Int,
    transition: PageTransition,
): Modifier = when (transition) {
    PageTransition.SLIDE -> this
    PageTransition.CURL -> this.graphicsLayer {
        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
        cameraDistance = 12f * density
        rotationY = lerp(0f, -90f, pageOffset.coerceIn(0f, 1f))
        alpha = lerp(1f, 0.3f, pageOffset.coerceIn(0f, 1f))
    }
    PageTransition.FADE -> this.graphicsLayer {
        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
        alpha = lerp(1f, 0f, pageOffset.coerceIn(0f, 1f))
        translationX = size.width * pagerState.currentPageOffsetFraction * if (page < pagerState.currentPage) 1f else -1f
    }
}
