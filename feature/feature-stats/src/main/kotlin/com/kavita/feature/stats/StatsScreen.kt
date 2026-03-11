package com.kavita.feature.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.kavita.core.model.DailyReadingStat
import com.kavita.core.model.ReadingStatsOverview
import com.kavita.core.core.ui.R
import com.kavita.core.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistics)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(innerPadding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                uiState.overview?.let { overview ->
                    OverviewCards(overview)
                    StreakCards(overview)
                }

                DailyGoalSection(
                    todayReadingSeconds = uiState.todayReadingSeconds,
                    dailyGoalMinutes = uiState.dailyGoalMinutes,
                )

                Text(
                    text = stringResource(R.string.last_30_days),
                    style = MaterialTheme.typography.titleMedium,
                )
                ReadingChart(
                    dailyStats = uiState.dailyStats,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                )
            }
        }
    }
}

@Composable
private fun OverviewCards(overview: ReadingStatsOverview) {
    val totalMinutes = overview.totalReadingTimeSeconds / 60
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatCard(
            title = stringResource(R.string.total_time),
            value = formatMinutes(totalMinutes),
            modifier = Modifier.weight(1f),
        )
        StatCard(
            title = stringResource(R.string.pages_read),
            value = overview.totalPagesRead.toString(),
            modifier = Modifier.weight(1f),
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatCard(
            title = stringResource(R.string.chapters_completed),
            value = overview.chaptersCompleted.toString(),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StreakCards(overview: ReadingStatsOverview) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StreakCard(
            title = stringResource(R.string.current_streak),
            days = overview.currentStreak,
            modifier = Modifier.weight(1f),
        )
        StreakCard(
            title = stringResource(R.string.best_streak),
            days = overview.longestStreak,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StreakCard(
    title: String,
    days: Int,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.streak_days, days),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DailyGoalSection(
    todayReadingSeconds: Long,
    dailyGoalMinutes: Int,
) {
    val todayMinutes = todayReadingSeconds / 60
    val goalSeconds = dailyGoalMinutes * 60L
    val progress = if (goalSeconds > 0) {
        (todayReadingSeconds.toFloat() / goalSeconds).coerceIn(0f, 1f)
    } else {
        0f
    }

    Text(
        text = stringResource(R.string.daily_goal),
        style = MaterialTheme.typography.titleMedium,
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(80.dp),
                    strokeWidth = 8.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Column {
                Text(
                    text = stringResource(R.string.daily_goal_progress, todayMinutes, dailyGoalMinutes),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (todayMinutes >= dailyGoalMinutes) {
                        stringResource(R.string.goal_achieved)
                    } else {
                        stringResource(R.string.goal_remaining, dailyGoalMinutes - todayMinutes)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReadingChart(
    dailyStats: List<DailyReadingStat>,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val emptyBarColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Rellenar 30 dias (incluidos los que no tienen datos)
    val days = 30
    val today = java.time.LocalDate.now()
    val statsByDay = dailyStats.associateBy { it.day }
    val allDays = (0 until days).map { offset ->
        val date = today.minusDays((days - 1 - offset).toLong())
        val key = date.toString()
        date to (statsByDay[key]?.seconds ?: 0L)
    }

    val maxSeconds = allDays.maxOf { it.second }.coerceAtLeast(1)

    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                val chartHeight = size.height
                val barWidth = size.width / days
                val gap = (barWidth * 0.25f).coerceAtMost(3.dp.toPx())
                val minBarHeight = 2.dp.toPx()

                allDays.forEachIndexed { index, (_, seconds) ->
                    val x = index * barWidth + gap
                    val w = barWidth - gap * 2

                    if (seconds > 0) {
                        val barHeight = ((seconds.toFloat() / maxSeconds) * chartHeight * 0.9f)
                            .coerceAtLeast(minBarHeight)
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, chartHeight - barHeight),
                            size = Size(w, barHeight),
                            cornerRadius = CornerRadius(2.dp.toPx()),
                        )
                    } else {
                        // Barra minima gris para indicar el dia
                        drawRoundRect(
                            color = emptyBarColor,
                            topLeft = Offset(x, chartHeight - minBarHeight),
                            size = Size(w, minBarHeight),
                            cornerRadius = CornerRadius(1.dp.toPx()),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Etiquetas: primer dia, medio, hoy
            Row(modifier = Modifier.fillMaxWidth()) {
                val first = allDays.first().first
                val mid = allDays[days / 2].first
                val last = allDays.last().first
                val fmt = java.time.format.DateTimeFormatter.ofPattern("d MMM")
                Text(
                    text = first.format(fmt),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = mid.format(fmt),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = last.format(fmt),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                )
            }
        }
    }
}

private fun formatMinutes(minutes: Long): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 -> "${hours}h ${mins}m"
        else -> "${mins}m"
    }
}
