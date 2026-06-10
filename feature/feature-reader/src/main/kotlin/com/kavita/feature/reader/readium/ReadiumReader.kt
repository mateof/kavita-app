package com.kavita.feature.reader.readium

import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.delay
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.css.FontStyle
import org.readium.r2.navigator.epub.css.FontWeight
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.preferences.ColumnCount
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.ReadingProgression
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positionsByReadingOrder
import org.readium.r2.shared.util.AbsoluteUrl

private const val FRAGMENT_TAG = "readium_nav"

// Readium interpreta EpubPreferences.fontSize como un ratio (1.0 = 100%).
// El slider de ajustes guarda un tamaño en "px" (12-32, 16 por defecto), así que
// lo convertimos respecto a esta base: 16px = 1.0 (100%), 12px = 0.75, 32px = 2.0.
private const val EPUB_FONT_SIZE_BASE = 16.0

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
    // Última posición reportada por el propio navegador. Sirve para distinguir un
    // cambio de página originado por el lector (tap/swipe) de uno externo (slider,
    // prev/next) y así evitar un bucle de realimentación al navegar.
    val lastNavigatorPosition = remember { mutableStateOf(-1) }

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
        if (format != MangaFormat.EPUB) return@LaunchedEffect

        val existing = fragmentManager.findFragmentByTag(FRAGMENT_TAG)
        if (existing != null) {
            fragmentManager.commitNow { remove(existing) }
        }

        val allPositions = publication.positionsByReadingOrder().flatMap { it }

        // position de Readium es 1-based, allPositions es 0-based
        val initialLocator = if (currentPage > 0) {
            allPositions.getOrNull((currentPage - 1).coerceAtLeast(0))
        } else null

        // Total de posiciones EPUB (estimación estable de Readium) y notificar
        onTotalPagesResolved?.invoke(allPositions.size)

        val epubPreferences = buildEpubPreferences(
            epubFontSize, epubFontFamily, epubLineSpacing, epubTheme, readingDirection, pageLayout,
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

        // Esperar a que el fragment tenga vista antes de registrar el listener
        // (en vez de un delay fijo: más fiable en arranques lentos).
        var fragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG)
        var attempts = 0
        while ((fragment == null || fragment.view == null) && attempts < 60) {
            delay(50)
            fragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG)
            attempts++
        }

        val epubFragment = fragment as? EpubNavigatorFragment ?: return@LaunchedEffect

        val tapListener = object : InputListener {
            override fun onTap(event: TapEvent): Boolean {
                val view = epubFragment.view ?: return false
                val viewWidth = view.width
                val viewHeight = view.height

                when (tapNavigation) {
                    TapNavigation.LATERAL -> {
                        val zone = viewWidth / 3
                        return when {
                            event.point.x < zone -> {
                                epubFragment.goBackward(animated = true)
                                true
                            }
                            event.point.x > viewWidth - zone -> {
                                epubFragment.goForward(animated = true)
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
                                epubFragment.goBackward(animated = true)
                                true
                            }
                            event.point.y > viewHeight - zone -> {
                                epubFragment.goForward(animated = true)
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

        epubFragment.addInputListener(tapListener)
        epubFragment.currentLocator.collect { locator ->
            val position = locator.locations.position
            if (position != null) {
                lastNavigatorPosition.value = position
                onPageChanged(position)
            }
        }
    }

    // Navegar el EPUB cuando el cambio de página viene de fuera (slider de página,
    // botones prev/next), no del propio lector. La guarda con lastNavigatorPosition
    // evita realimentación con el collector de currentLocator.
    LaunchedEffect(currentPage) {
        if (currentPage <= 0) return@LaunchedEffect
        if (currentPage == lastNavigatorPosition.value) return@LaunchedEffect
        val fragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG) as? EpubNavigatorFragment
            ?: return@LaunchedEffect
        val locator = publication.positionsByReadingOrder().flatMap { it }
            .getOrNull(currentPage - 1) ?: return@LaunchedEffect
        lastNavigatorPosition.value = currentPage
        fragment.go(locator, animated = false)
    }

    // Actualizar preferencias EPUB en tiempo real cuando el usuario las modifica
    LaunchedEffect(epubFontSize, epubFontFamily, epubLineSpacing, epubTheme, readingDirection, pageLayout) {
        val fragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG)
        if (fragment is EpubNavigatorFragment) {
            fragment.submitPreferences(
                buildEpubPreferences(
                    epubFontSize, epubFontFamily, epubLineSpacing, epubTheme, readingDirection, pageLayout,
                ),
            )
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

/**
 * Construye las preferencias de Readium a partir de los ajustes del lector. Además de la
 * tipografía, mapea la dirección de lectura (RTL / scroll vertical) y la disposición de
 * página (columnas) para que esos ajustes también tengan efecto en EPUB de texto.
 */
@OptIn(ExperimentalReadiumApi::class)
private fun buildEpubPreferences(
    fontSize: Float,
    fontFamily: String,
    lineSpacing: Float,
    theme: ReaderTheme,
    readingDirection: ReadingDirection,
    pageLayout: PageLayout,
): EpubPreferences = EpubPreferences(
    fontSize = fontSize / EPUB_FONT_SIZE_BASE,
    fontFamily = if (fontFamily != "default") FontFamily(fontFamily) else null,
    lineHeight = lineSpacing.toDouble(),
    theme = when (theme) {
        ReaderTheme.LIGHT -> Theme.LIGHT
        ReaderTheme.DARK -> Theme.DARK
        ReaderTheme.SEPIA -> Theme.SEPIA
        ReaderTheme.SYSTEM -> null
    },
    scroll = readingDirection == ReadingDirection.VERTICAL || readingDirection == ReadingDirection.WEBTOON,
    readingProgression = if (readingDirection == ReadingDirection.RIGHT_TO_LEFT) {
        ReadingProgression.RTL
    } else {
        ReadingProgression.LTR
    },
    columnCount = if (pageLayout == PageLayout.DOUBLE) ColumnCount.TWO else ColumnCount.AUTO,
)
