package com.kavita.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kavita.core.model.AppTheme
import com.kavita.core.model.ReadingDirection
import com.kavita.core.core.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToServers: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Seccion Apariencia
            SectionHeader(stringResource(R.string.appearance))

            ListItem(
                headlineContent = { Text(stringResource(R.string.theme)) },
                supportingContent = {
                    Text(
                        when (preferences.theme) {
                            AppTheme.SYSTEM -> stringResource(R.string.follow_system)
                            AppTheme.LIGHT -> stringResource(R.string.light)
                            AppTheme.DARK -> stringResource(R.string.dark)
                        }
                    )
                },
                modifier = Modifier.clickable {
                    val next = when (preferences.theme) {
                        AppTheme.SYSTEM -> AppTheme.LIGHT
                        AppTheme.LIGHT -> AppTheme.DARK
                        AppTheme.DARK -> AppTheme.SYSTEM
                    }
                    viewModel.updateTheme(next)
                },
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.dynamic_color)) },
                supportingContent = { Text(stringResource(R.string.dynamic_color_desc)) },
                trailingContent = {
                    Switch(
                        checked = preferences.dynamicColor,
                        onCheckedChange = viewModel::updateDynamicColor,
                    )
                },
            )

            HorizontalDivider()

            // Seccion Lectura
            SectionHeader(stringResource(R.string.reading))

            ListItem(
                headlineContent = { Text(stringResource(R.string.reading_direction)) },
                supportingContent = {
                    Text(
                        when (preferences.defaultReadingDirection) {
                            ReadingDirection.LEFT_TO_RIGHT -> stringResource(R.string.dir_ltr)
                            ReadingDirection.RIGHT_TO_LEFT -> stringResource(R.string.dir_rtl_manga)
                            ReadingDirection.VERTICAL -> stringResource(R.string.dir_vertical)
                            ReadingDirection.WEBTOON -> stringResource(R.string.dir_webtoon)
                            ReadingDirection.DOUBLE_PAGE -> stringResource(R.string.dir_double)
                        }
                    )
                },
                modifier = Modifier.clickable {
                    val values = ReadingDirection.entries
                    val next = values[(preferences.defaultReadingDirection.ordinal + 1) % values.size]
                    viewModel.updateReadingDirection(next)
                },
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.keep_screen_on)) },
                trailingContent = {
                    Switch(
                        checked = preferences.keepScreenOn,
                        onCheckedChange = viewModel::updateKeepScreenOn,
                    )
                },
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.daily_goal_setting)) },
                supportingContent = { Text(stringResource(R.string.daily_goal_minutes, preferences.dailyReadingGoalMinutes)) },
                modifier = Modifier.clickable {
                    val options = listOf(10, 15, 20, 30, 45, 60, 90, 120)
                    val currentIndex = options.indexOf(preferences.dailyReadingGoalMinutes)
                    val nextIndex = (currentIndex + 1) % options.size
                    viewModel.updateDailyGoal(options[nextIndex])
                },
            )

            HorizontalDivider()

            // Seccion Descargas
            SectionHeader(stringResource(R.string.downloads_section))

            ListItem(
                headlineContent = { Text(stringResource(R.string.wifi_only)) },
                supportingContent = { Text(stringResource(R.string.wifi_only_desc)) },
                trailingContent = {
                    Switch(
                        checked = preferences.wifiOnlyDownloads,
                        onCheckedChange = viewModel::updateWifiOnlyDownloads,
                    )
                },
            )

            HorizontalDivider()

            // Seccion Servidores
            SectionHeader(stringResource(R.string.servers_section))

            ListItem(
                headlineContent = { Text(stringResource(R.string.manage_servers)) },
                supportingContent = { Text(stringResource(R.string.manage_servers_desc)) },
                modifier = Modifier.clickable(onClick = onNavigateToServers),
            )

            HorizontalDivider()

            // Seccion Almacenamiento
            SectionHeader(stringResource(R.string.storage_section))

            val cacheSizeBytes by viewModel.imageCacheSizeBytes.collectAsStateWithLifecycle()
            val cacheSizeText = formatFileSize(cacheSizeBytes)

            ListItem(
                headlineContent = { Text(stringResource(R.string.image_cache)) },
                supportingContent = { Text(stringResource(R.string.image_cache_size, cacheSizeText)) },
                modifier = Modifier.clickable { viewModel.clearImageCache() },
                trailingContent = {
                    Text(
                        stringResource(R.string.clear_cache),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )

            HorizontalDivider()

            // Seccion Acerca de
            SectionHeader(stringResource(R.string.about))

            ListItem(
                headlineContent = { Text(stringResource(R.string.app_name)) },
                supportingContent = { Text(stringResource(R.string.app_version)) },
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}
