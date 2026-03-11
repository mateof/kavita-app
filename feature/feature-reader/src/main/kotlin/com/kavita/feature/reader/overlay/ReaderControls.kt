package com.kavita.feature.reader.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kavita.core.core.ui.R

@Composable
fun ReaderControls(
    visible: Boolean,
    title: String,
    currentPage: Int,
    totalPages: Int,
    isBookmarked: Boolean,
    brightness: Float,
    hasPrevChapter: Boolean,
    hasNextChapter: Boolean,
    hasToc: Boolean = false,
    onClose: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onPageSliderChange: (Int) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onPrevChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenToc: () -> Unit = {},
) {
    var showBrightnessSlider by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Barra superior
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent),
                        ),
                    )
                    .systemBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.close),
                        tint = Color.White,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onBookmarkToggle) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = if (isBookmarked) stringResource(R.string.remove_bookmark) else stringResource(R.string.add_bookmark),
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else Color.White,
                    )
                }
                if (hasToc) {
                    IconButton(onClick = onOpenToc) {
                        Icon(
                            Icons.Filled.FormatListBulleted,
                            contentDescription = stringResource(R.string.table_of_contents),
                            tint = Color.White,
                        )
                    }
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.reader_settings),
                        tint = Color.White,
                    )
                }
                IconButton(onClick = { showBrightnessSlider = !showBrightnessSlider }) {
                    Icon(
                        Icons.Filled.Brightness6,
                        contentDescription = stringResource(R.string.brightness),
                        tint = Color.White,
                    )
                }
            }
        }

        // Slider de brillo
        AnimatedVisibility(
            visible = visible && showBrightnessSlider,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
        ) {
            // Slider vertical simulado con rotacion
            Column(
                modifier = Modifier
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        MaterialTheme.shapes.medium,
                    )
                    .padding(16.dp),
            ) {
                Text(stringResource(R.string.brightness), color = Color.White, style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = brightness,
                    onValueChange = onBrightnessChange,
                    valueRange = 0.1f..1f,
                )
            }
        }

        // Barra inferior
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        ),
                    )
                    .systemBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                // Slider de pagina
                if (totalPages > 1) {
                    Slider(
                        value = currentPage.toFloat(),
                        onValueChange = { onPageSliderChange(it.toInt()) },
                        valueRange = 0f..(totalPages - 1).toFloat(),
                        steps = totalPages - 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onPrevChapter,
                        enabled = hasPrevChapter,
                    ) {
                        Icon(
                            Icons.Filled.SkipPrevious,
                            contentDescription = stringResource(R.string.previous_chapter),
                            tint = if (hasPrevChapter) Color.White else Color.White.copy(alpha = 0.3f),
                        )
                    }

                    Text(
                        text = "${currentPage + 1} / $totalPages",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                    )

                    IconButton(
                        onClick = onNextChapter,
                        enabled = hasNextChapter,
                    ) {
                        Icon(
                            Icons.Filled.SkipNext,
                            contentDescription = stringResource(R.string.next_chapter),
                            tint = if (hasNextChapter) Color.White else Color.White.copy(alpha = 0.3f),
                        )
                    }
                }
            }
        }
    }
}
