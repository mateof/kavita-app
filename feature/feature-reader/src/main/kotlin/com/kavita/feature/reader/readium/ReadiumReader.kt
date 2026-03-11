package com.kavita.feature.reader.readium

import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import com.kavita.core.model.MangaFormat
import com.kavita.core.model.PageLayout
import com.kavita.core.model.PageScaleType
import com.kavita.core.model.ReaderTheme
import com.kavita.core.model.ReadingDirection
import com.kavita.core.model.TapNavigation
import org.readium.adapter.pdfium.navigator.PdfiumEngineProvider
import org.readium.adapter.pdfium.navigator.PdfiumPreferences
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.pdf.PdfNavigatorFactory
import org.readium.r2.navigator.pdf.PdfNavigatorFragment
import org.readium.r2.navigator.preferences.Axis
import org.readium.r2.navigator.preferences.Fit
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.epub.css.FontStyle
import org.readium.r2.navigator.epub.css.FontWeight
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positionsByReadingOrder
import org.readium.r2.shared.util.AbsoluteUrl

private const val FRAGMENT_TAG = "readium_nav"

@OptIn(ExperimentalReadiumApi::class)
@Composable
fun ReadiumReader(
    publication: Publication,
    format: MangaFormat,
    currentPage: Int,
    pageLayout: PageLayout,
    pageScaleType: PageScaleType,
    readingDirection: ReadingDirection,
    tapNavigation: TapNavigation,
    epubFontSize: Float,
    epubFontFamily: String,
    epubLineSpacing: Float,
    epubTheme: ReaderTheme,
    onPageChanged: (Int) -> Unit,
    onTotalPagesResolved: ((Int) -> Unit)? = null,
    onTapCenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current as FragmentActivity
    val fragmentManager = activity.supportFragmentManager
    val containerId = remember { View.generateViewId() }

    val fit = when (pageScaleType) {
        PageScaleType.FIT_SCREEN -> Fit.CONTAIN
        PageScaleType.FIT_WIDTH -> Fit.WIDTH
        PageScaleType.FIT_HEIGHT -> Fit.CONTAIN
    }
    val progression = when (readingDirection) {
        ReadingDirection.RIGHT_TO_LEFT -> ReadingProgression.RTL
        else -> ReadingProgression.LTR
    }

    // Solo el Fragment, sin overlay
    AndroidView(
        factory = { context ->
            FragmentContainerView(context).apply {
                id = containerId
            }
        },
        modifier = modifier.fillMaxSize(),
    )

    LaunchedEffect(containerId, publication) {
        val existing = fragmentManager.findFragmentByTag(FRAGMENT_TAG)
        if (existing != null) {
            fragmentManager.commitNow { remove(existing) }
        }

        val initialLocator = if (currentPage > 0 && publication.readingOrder.isNotEmpty()) {
            if (format == MangaFormat.EPUB) {
                // Para EPUB: buscar el Locator real en las posiciones de Readium
                val allPositions = publication.positionsByReadingOrder()
                    .flatMap { it }
                // position de Readium es 1-based, allPositions es 0-based
                allPositions.getOrNull((currentPage - 1).coerceAtLeast(0))
            } else {
                // Para PDF: usar posición directa
                Locator(
                    href = publication.readingOrder.first().url(),
                    mediaType = publication.readingOrder.first().mediaType
                        ?: org.readium.r2.shared.util.mediatype.MediaType.PDF,
                    locations = Locator.Locations(position = currentPage),
                )
            }
        } else null

        when (format) {
            MangaFormat.PDF -> {
                val pdfEngineProvider = PdfiumEngineProvider()
                val factory = PdfNavigatorFactory(
                    publication = publication,
                    pdfEngineProvider = pdfEngineProvider,
                )
                val pdfPrefs = PdfiumPreferences(
                    fit = fit,
                    readingProgression = progression,
                    scrollAxis = Axis.HORIZONTAL,
                )
                val fragmentFactory = factory.createFragmentFactory(
                    initialLocator = initialLocator,
                    initialPreferences = pdfPrefs,
                )
                fragmentManager.fragmentFactory = fragmentFactory
                fragmentManager.commitNow {
                    add(containerId, PdfNavigatorFragment::class.java, null, FRAGMENT_TAG)
                }
            }
            MangaFormat.EPUB -> {
                val epubPreferences = EpubPreferences(
                    fontSize = epubFontSize.toDouble(),
                    fontFamily = if (epubFontFamily != "default") FontFamily(epubFontFamily) else null,
                    lineHeight = epubLineSpacing.toDouble(),
                    theme = when (epubTheme) {
                        ReaderTheme.LIGHT -> Theme.LIGHT
                        ReaderTheme.DARK -> Theme.DARK
                        ReaderTheme.SEPIA -> Theme.SEPIA
                        ReaderTheme.SYSTEM -> null
                    },
                )
                // Configurar fuentes personalizadas
                val fragmentConfig = EpubNavigatorFragment.Configuration {
                    servedAssets = listOf("fonts/.*")
                    addFontFamilyDeclaration(
                        fontFamily = FontFamily("OpenDyslexic"),
                        alternates = listOf(FontFamily("sans-serif")),
                    ) {
                        addFontFace {
                            addSource("fonts/OpenDyslexic-Regular.otf")
                            setFontStyle(FontStyle.NORMAL)
                            setFontWeight(FontWeight.NORMAL)
                        }
                        addFontFace {
                            addSource("fonts/OpenDyslexic-Bold.otf")
                            setFontStyle(FontStyle.NORMAL)
                            setFontWeight(FontWeight.BOLD)
                        }
                        addFontFace {
                            addSource("fonts/OpenDyslexic-Italic.otf")
                            setFontStyle(FontStyle.ITALIC)
                            setFontWeight(FontWeight.NORMAL)
                        }
                    }
                }
                // Calcular total de posiciones EPUB y notificar
                val epubTotalPositions = publication.positionsByReadingOrder()
                    .flatMap { it }.size
                onTotalPagesResolved?.invoke(epubTotalPositions)

                val epubFactory = EpubNavigatorFactory(publication)
                val fragmentFactory = epubFactory.createFragmentFactory(
                    initialLocator = initialLocator,
                    initialPreferences = epubPreferences,
                    listener = object : EpubNavigatorFragment.Listener {
                        override fun onExternalLinkActivated(url: AbsoluteUrl) { }
                    },
                    configuration = fragmentConfig,
                )
                fragmentManager.fragmentFactory = fragmentFactory
                fragmentManager.commitNow {
                    add(containerId, EpubNavigatorFragment::class.java, null, FRAGMENT_TAG)
                }
            }
            else -> { }
        }

        // Esperar a que el fragment se adjunte y registrar tap listener
        kotlinx.coroutines.delay(500)
        val fragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG)

        val tapListener = object : InputListener {
            override fun onTap(event: TapEvent): Boolean {
                val view = fragment?.view ?: return false
                val viewWidth = view.width
                val viewHeight = view.height

                when (tapNavigation) {
                    TapNavigation.LATERAL -> {
                        val zone = viewWidth / 3
                        return when {
                            event.point.x < zone -> {
                                when (fragment) {
                                    is EpubNavigatorFragment -> fragment.goBackward(animated = true)
                                    is PdfNavigatorFragment<*, *> -> fragment.goBackward(animated = true)
                                    else -> {}
                                }
                                true
                            }
                            event.point.x > viewWidth - zone -> {
                                when (fragment) {
                                    is EpubNavigatorFragment -> fragment.goForward(animated = true)
                                    is PdfNavigatorFragment<*, *> -> fragment.goForward(animated = true)
                                    else -> {}
                                }
                                true
                            }
                            else -> {
                                onTapCenter()
                                true
                            }
                        }
                    }
                    TapNavigation.VERTICAL -> {
                        val zone = viewHeight / 3
                        return when {
                            event.point.y < zone -> {
                                when (fragment) {
                                    is EpubNavigatorFragment -> fragment.goBackward(animated = true)
                                    is PdfNavigatorFragment<*, *> -> fragment.goBackward(animated = true)
                                    else -> {}
                                }
                                true
                            }
                            event.point.y > viewHeight - zone -> {
                                when (fragment) {
                                    is EpubNavigatorFragment -> fragment.goForward(animated = true)
                                    is PdfNavigatorFragment<*, *> -> fragment.goForward(animated = true)
                                    else -> {}
                                }
                                true
                            }
                            else -> {
                                onTapCenter()
                                true
                            }
                        }
                    }
                    TapNavigation.NONE -> {
                        val centerStart = viewWidth / 3
                        val centerEnd = viewWidth * 2 / 3
                        if (event.point.x > centerStart && event.point.x < centerEnd) {
                            onTapCenter()
                            return true
                        }
                        return false
                    }
                }
            }
        }

        when (fragment) {
            is PdfNavigatorFragment<*, *> -> {
                fragment.addInputListener(tapListener)
                fragment.currentLocator.collect { locator ->
                    val pageIndex = locator.locations.position ?: return@collect
                    onPageChanged(pageIndex)
                }
            }
            is EpubNavigatorFragment -> {
                fragment.addInputListener(tapListener)
                fragment.currentLocator.collect { locator ->
                    val position = locator.locations.position
                    if (position != null) {
                        onPageChanged(position)
                    }
                }
            }
        }
    }

    // Actualizar preferencias EPUB en tiempo real cuando el usuario las modifica
    LaunchedEffect(epubFontSize, epubFontFamily, epubLineSpacing, epubTheme) {
        val fragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG)
        if (fragment is EpubNavigatorFragment) {
            val updatedPreferences = EpubPreferences(
                fontSize = epubFontSize.toDouble(),
                fontFamily = if (epubFontFamily != "default") FontFamily(epubFontFamily) else null,
                lineHeight = epubLineSpacing.toDouble(),
                theme = when (epubTheme) {
                    ReaderTheme.LIGHT -> Theme.LIGHT
                    ReaderTheme.DARK -> Theme.DARK
                    ReaderTheme.SEPIA -> Theme.SEPIA
                    ReaderTheme.SYSTEM -> null
                },
            )
            fragment.submitPreferences(updatedPreferences)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val fragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG)
            if (fragment != null) {
                fragmentManager.commitNow { remove(fragment) }
            }
        }
    }
}
