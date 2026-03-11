package com.kavita.core.model

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val dynamicColor: Boolean = true,
    val defaultReadingDirection: ReadingDirection = ReadingDirection.LEFT_TO_RIGHT,
    val pageLayout: PageLayout = PageLayout.SINGLE,
    val pageScaleType: PageScaleType = PageScaleType.FIT_SCREEN,
    val pageTransition: PageTransition = PageTransition.SLIDE,
    val tapNavigation: TapNavigation = TapNavigation.LATERAL,
    val epubFontSize: Float = 16f,
    val epubFontFamily: String = "default",
    val epubLineSpacing: Float = 1.5f,
    val epubTheme: ReaderTheme = ReaderTheme.SYSTEM,
    val keepScreenOn: Boolean = true,
    val wifiOnlyDownloads: Boolean = true,
    val dailyReadingGoalMinutes: Int = 30,
    val pdfNightMode: Boolean = false,
)

@Serializable
enum class AppTheme { LIGHT, DARK, SYSTEM }

@Serializable
enum class ReadingDirection { LEFT_TO_RIGHT, RIGHT_TO_LEFT, VERTICAL, WEBTOON, DOUBLE_PAGE }

@Serializable
enum class PageLayout { SINGLE, DOUBLE }

@Serializable
enum class PageScaleType { FIT_SCREEN, FIT_WIDTH, FIT_HEIGHT }

@Serializable
enum class PageTransition { SLIDE, CURL, FADE }

@Serializable
enum class TapNavigation { NONE, LATERAL, VERTICAL }

@Serializable
enum class ReaderTheme { LIGHT, DARK, SEPIA, SYSTEM }
