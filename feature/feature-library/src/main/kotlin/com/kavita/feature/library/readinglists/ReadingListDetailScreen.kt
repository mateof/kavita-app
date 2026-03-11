package com.kavita.feature.library.readinglists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kavita.core.model.ReadingListItem
import com.kavita.core.core.ui.R
import com.kavita.core.ui.components.CoverImage
import com.kavita.core.ui.components.EmptyState
import com.kavita.core.ui.components.ErrorScreen
import com.kavita.core.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingListDetailScreen(
    onSeriesClick: (seriesId: Int, serverId: Long) -> Unit,
    onNavigateBack: () -> Unit,
    serverId: Long,
    viewModel: ReadingListDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> LoadingIndicator()
            uiState.error != null -> ErrorScreen(message = uiState.error!!, onRetry = viewModel::refresh)
            uiState.items.isEmpty() -> EmptyState(message = stringResource(R.string.list_empty))
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding(),
                    ),
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        ReadingListItemRow(
                            item = item,
                            onClick = { onSeriesClick(item.seriesId, serverId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingListItemRow(
    item: ReadingListItem,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(item.seriesName) },
        supportingContent = {
            if (item.chapterNumber.isNotBlank()) {
                Text(stringResource(R.string.chapter_number, item.chapterNumber))
            }
        },
        leadingContent = {
            CoverImage(
                imageUrl = item.coverImage,
                contentDescription = item.seriesName,
                modifier = Modifier.size(56.dp),
            )
        },
    )
}
