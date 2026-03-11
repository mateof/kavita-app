package com.kavita.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun SeriesCard(
    name: String,
    coverUrl: String?,
    pagesRead: Int,
    totalPages: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDownloaded: Boolean = false,
) {
    Column(
        modifier = modifier
            .width(120.dp)
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box {
            CoverImage(
                imageUrl = coverUrl,
                contentDescription = name,
            )
            if (pagesRead > 0 && totalPages > 0) {
                ProgressBadge(
                    progress = pagesRead.toFloat() / totalPages,
                )
            }
            if (isDownloaded) {
                DownloadBadge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                )
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (pagesRead > 0 && totalPages > 0) {
            LinearProgressIndicator(
                progress = { pagesRead.toFloat() / totalPages },
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
