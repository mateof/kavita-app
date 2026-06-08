package com.kavita.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kavita.core.model.AppTheme
import com.kavita.core.model.ReadingDirection
import com.kavita.core.core.ui.R
import java.io.File

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
                supportingContent = { Text(stringResource(R.string.app_version_fmt, viewModel.currentVersion)) },
            )

            UpdateSection(viewModel)
        }
    }
}

@Composable
private fun UpdateSection(viewModel: SettingsViewModel) {
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Lanzar el instalador cuando el APK termina de descargarse
    LaunchedEffect(updateState.downloadedApk) {
        val apk = updateState.downloadedApk
        if (apk != null) {
            installApk(context, apk)
            viewModel.onApkInstallLaunched()
        }
    }

    val available = updateState.available

    ListItem(
        headlineContent = { Text(stringResource(R.string.check_updates)) },
        supportingContent = {
            Text(
                when {
                    updateState.checking -> stringResource(R.string.checking_updates)
                    available != null -> stringResource(R.string.update_available_fmt, available.versionName)
                    updateState.checkFailed -> stringResource(R.string.update_check_failed)
                    else -> stringResource(R.string.up_to_date)
                }
            )
        },
        trailingContent = if (updateState.checking) {
            { CircularProgressIndicator(modifier = Modifier.size(20.dp)) }
        } else {
            null
        },
        modifier = Modifier.clickable(enabled = !updateState.checking) { viewModel.checkForUpdate() },
    )

    if (available != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            if (updateState.downloading) {
                Text(
                    stringResource(R.string.downloading_update, (updateState.downloadProgress * 100).toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { updateState.downloadProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (available.apkUrl != null) {
                        Button(onClick = { viewModel.downloadUpdate() }) {
                            Text(stringResource(R.string.download_and_install))
                        }
                    }
                    OutlinedButton(onClick = { openUrl(context, available.releaseUrl) }) {
                        Text(stringResource(R.string.view_on_github))
                    }
                }
                if (updateState.downloadFailed) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.update_download_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun installApk(context: Context, file: File) {
    // En Android 8+ hace falta el permiso de "instalar apps desconocidas" para esta fuente.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        !context.packageManager.canRequestPackageInstalls()
    ) {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        return
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
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
