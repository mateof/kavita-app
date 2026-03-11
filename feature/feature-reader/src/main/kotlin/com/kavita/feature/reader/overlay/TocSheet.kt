package com.kavita.feature.reader.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kavita.core.core.ui.R
import com.kavita.feature.reader.TocEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TocSheet(
    entries: List<TocEntry>,
    onEntryClick: (TocEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Text(
            text = stringResource(R.string.table_of_contents),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyColumn(
            modifier = Modifier.padding(bottom = 32.dp),
        ) {
            items(entries) { entry ->
                Text(
                    text = entry.title,
                    style = if (entry.level == 0) {
                        MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    color = if (entry.level == 0) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onEntryClick(entry)
                            onDismiss()
                        }
                        .padding(
                            start = (16 + entry.level * 24).dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 12.dp,
                        ),
                )
            }
        }
    }
}
