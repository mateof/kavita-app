package com.kavita.feature.reader.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kavita.core.core.ui.R
import com.kavita.core.model.MangaFormat
import com.kavita.core.model.PageLayout
import com.kavita.core.model.PageScaleType
import com.kavita.core.model.PageTransition
import com.kavita.core.model.ReaderTheme
import com.kavita.core.model.ReadingDirection
import com.kavita.core.model.TapNavigation
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReaderSettingsSheet(
    readingDirection: ReadingDirection,
    pageLayout: PageLayout,
    pageScaleType: PageScaleType,
    pageTransition: PageTransition,
    tapNavigation: TapNavigation,
    format: MangaFormat = MangaFormat.UNKNOWN,
    pdfNightMode: Boolean = false,
    epubFontSize: Float = 16f,
    epubFontFamily: String = "default",
    epubLineSpacing: Float = 1.5f,
    epubTheme: ReaderTheme = ReaderTheme.SYSTEM,
    onReadingDirectionChange: (ReadingDirection) -> Unit,
    onPageLayoutChange: (PageLayout) -> Unit,
    onPageScaleTypeChange: (PageScaleType) -> Unit,
    onPageTransitionChange: (PageTransition) -> Unit,
    onTapNavigationChange: (TapNavigation) -> Unit,
    onPdfNightModeChange: (Boolean) -> Unit = {},
    onEpubFontSizeChange: (Float) -> Unit = {},
    onEpubFontFamilyChange: (String) -> Unit = {},
    onEpubLineSpacingChange: (Float) -> Unit = {},
    onEpubThemeChange: (ReaderTheme) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.reader_settings),
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Direccion de lectura
            Text(
                text = stringResource(R.string.direction),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                directionOptions.forEach { (direction, label) ->
                    FilterChip(
                        selected = readingDirection == direction,
                        onClick = { onReadingDirectionChange(direction) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Navegacion por toque
            Text(
                text = stringResource(R.string.tap_navigation),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                tapNavOptions.forEach { (nav, label) ->
                    FilterChip(
                        selected = tapNavigation == nav,
                        onClick = { onTapNavigationChange(nav) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Disposicion de pagina
            Text(
                text = stringResource(R.string.pages_per_view),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                layoutOptions.forEach { (layout, label) ->
                    FilterChip(
                        selected = pageLayout == layout,
                        onClick = { onPageLayoutChange(layout) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Escala de pagina
            Text(
                text = stringResource(R.string.page_fit),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                scaleOptions.forEach { (scale, label) ->
                    FilterChip(
                        selected = pageScaleType == scale,
                        onClick = { onPageScaleTypeChange(scale) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Transicion
            Text(
                text = stringResource(R.string.page_transition),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                transitionOptions.forEach { (transition, label) ->
                    FilterChip(
                        selected = pageTransition == transition,
                        onClick = { onPageTransitionChange(transition) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }

            if (format == MangaFormat.PDF) {
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.night_mode),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Switch(
                        checked = pdfNightMode,
                        onCheckedChange = onPdfNightModeChange,
                    )
                }
            }

            // Ajustes especificos de EPUB
            if (format == MangaFormat.EPUB) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.epub_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tamano de fuente
                Text(
                    text = stringResource(R.string.font_size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Slider(
                        value = epubFontSize,
                        onValueChange = { onEpubFontSizeChange(it.roundToInt().toFloat()) },
                        valueRange = 12f..32f,
                        steps = 19, // (32 - 12) - 1 = 19 pasos intermedios para saltos de 1
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.font_size_px, epubFontSize.roundToInt()),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Fuente
                Text(
                    text = stringResource(R.string.font_family),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    epubFontOptions.forEach { (family, label) ->
                        FilterChip(
                            selected = epubFontFamily == family,
                            onClick = { onEpubFontFamilyChange(family) },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interlineado
                Text(
                    text = stringResource(R.string.line_spacing),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Slider(
                        value = epubLineSpacing,
                        onValueChange = { newValue ->
                            // Redondear a 1 decimal
                            val rounded = (newValue * 10).roundToInt() / 10f
                            onEpubLineSpacingChange(rounded)
                        },
                        valueRange = 1.0f..2.5f,
                        steps = 14, // (2.5 - 1.0) / 0.1 - 1 = 14 pasos intermedios
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "%.1f".format(epubLineSpacing),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tema de lectura
                Text(
                    text = stringResource(R.string.reader_theme),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    epubThemeOptions.forEach { (theme, label) ->
                        FilterChip(
                            selected = epubTheme == theme,
                            onClick = { onEpubThemeChange(theme) },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
            }
        }
    }
}

private val directionOptions = listOf(
    ReadingDirection.LEFT_TO_RIGHT to R.string.dir_ltr,
    ReadingDirection.RIGHT_TO_LEFT to R.string.dir_rtl,
    ReadingDirection.VERTICAL to R.string.dir_vertical,
    ReadingDirection.WEBTOON to R.string.dir_webtoon,
)

private val tapNavOptions = listOf(
    TapNavigation.LATERAL to R.string.tap_lateral,
    TapNavigation.VERTICAL to R.string.tap_vertical,
    TapNavigation.NONE to R.string.tap_disabled,
)

private val layoutOptions = listOf(
    PageLayout.SINGLE to R.string.layout_single,
    PageLayout.DOUBLE to R.string.layout_double,
)

private val scaleOptions = listOf(
    PageScaleType.FIT_SCREEN to R.string.scale_fit_screen,
    PageScaleType.FIT_WIDTH to R.string.scale_fit_width,
    PageScaleType.FIT_HEIGHT to R.string.scale_fit_height,
)

private val transitionOptions = listOf(
    PageTransition.SLIDE to R.string.transition_slide,
    PageTransition.CURL to R.string.transition_curl,
    PageTransition.FADE to R.string.transition_fade,
)

private val epubFontOptions = listOf(
    "default" to R.string.font_default,
    "serif" to R.string.font_serif,
    "sans-serif" to R.string.font_sans_serif,
    "monospace" to R.string.font_monospace,
    "cursive" to R.string.font_cursive,
    "Georgia" to R.string.font_georgia,
    "Literata" to R.string.font_literata,
    "OpenDyslexic" to R.string.font_opendyslexic,
)

private val epubThemeOptions = listOf(
    ReaderTheme.SYSTEM to R.string.theme_system,
    ReaderTheme.LIGHT to R.string.theme_light,
    ReaderTheme.DARK to R.string.theme_dark,
    ReaderTheme.SEPIA to R.string.theme_sepia,
)
