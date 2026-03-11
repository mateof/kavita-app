package com.kavita.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kavita.core.model.AppTheme
import com.kavita.core.model.PageLayout
import com.kavita.core.model.PageScaleType
import com.kavita.core.model.PageTransition
import com.kavita.core.model.ReadingDirection
import com.kavita.core.model.TapNavigation
import com.kavita.core.model.ReaderTheme
import com.kavita.core.model.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val READING_DIRECTION = stringPreferencesKey("reading_direction")
        val PAGE_LAYOUT = stringPreferencesKey("page_layout")
        val PAGE_SCALE_TYPE = stringPreferencesKey("page_scale_type")
        val PAGE_TRANSITION = stringPreferencesKey("page_transition")
        val TAP_NAVIGATION = stringPreferencesKey("tap_navigation")
        val EPUB_FONT_SIZE = floatPreferencesKey("epub_font_size")
        val EPUB_FONT_FAMILY = stringPreferencesKey("epub_font_family")
        val EPUB_LINE_SPACING = floatPreferencesKey("epub_line_spacing")
        val EPUB_THEME = stringPreferencesKey("epub_theme")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val WIFI_ONLY_DOWNLOADS = booleanPreferencesKey("wifi_only_downloads")
        val DAILY_READING_GOAL = intPreferencesKey("daily_reading_goal_minutes")
        val PDF_NIGHT_MODE = booleanPreferencesKey("pdf_night_mode")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            theme = prefs[Keys.THEME]?.let { AppTheme.valueOf(it) } ?: AppTheme.SYSTEM,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            defaultReadingDirection = prefs[Keys.READING_DIRECTION]?.let { ReadingDirection.valueOf(it) }
                ?: ReadingDirection.LEFT_TO_RIGHT,
            pageLayout = prefs[Keys.PAGE_LAYOUT]?.let { PageLayout.valueOf(it) } ?: PageLayout.SINGLE,
            pageScaleType = prefs[Keys.PAGE_SCALE_TYPE]?.let { PageScaleType.valueOf(it) } ?: PageScaleType.FIT_SCREEN,
            pageTransition = prefs[Keys.PAGE_TRANSITION]?.let { PageTransition.valueOf(it) } ?: PageTransition.SLIDE,
            tapNavigation = prefs[Keys.TAP_NAVIGATION]?.let { TapNavigation.valueOf(it) } ?: TapNavigation.LATERAL,
            epubFontSize = prefs[Keys.EPUB_FONT_SIZE] ?: 16f,
            epubFontFamily = prefs[Keys.EPUB_FONT_FAMILY] ?: "default",
            epubLineSpacing = prefs[Keys.EPUB_LINE_SPACING] ?: 1.5f,
            epubTheme = prefs[Keys.EPUB_THEME]?.let { ReaderTheme.valueOf(it) } ?: ReaderTheme.SYSTEM,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
            wifiOnlyDownloads = prefs[Keys.WIFI_ONLY_DOWNLOADS] ?: true,
            dailyReadingGoalMinutes = prefs[Keys.DAILY_READING_GOAL] ?: 30,
            pdfNightMode = prefs[Keys.PDF_NIGHT_MODE] ?: false,
        )
    }

    suspend fun updateTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun updateDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun updateReadingDirection(direction: ReadingDirection) {
        context.dataStore.edit { it[Keys.READING_DIRECTION] = direction.name }
    }

    suspend fun updatePageLayout(layout: PageLayout) {
        context.dataStore.edit { it[Keys.PAGE_LAYOUT] = layout.name }
    }

    suspend fun updatePageScaleType(scaleType: PageScaleType) {
        context.dataStore.edit { it[Keys.PAGE_SCALE_TYPE] = scaleType.name }
    }

    suspend fun updatePageTransition(transition: PageTransition) {
        context.dataStore.edit { it[Keys.PAGE_TRANSITION] = transition.name }
    }

    suspend fun updateTapNavigation(tapNavigation: TapNavigation) {
        context.dataStore.edit { it[Keys.TAP_NAVIGATION] = tapNavigation.name }
    }

    suspend fun updateEpubFontSize(size: Float) {
        context.dataStore.edit { it[Keys.EPUB_FONT_SIZE] = size }
    }

    suspend fun updateEpubFontFamily(family: String) {
        context.dataStore.edit { it[Keys.EPUB_FONT_FAMILY] = family }
    }

    suspend fun updateEpubLineSpacing(spacing: Float) {
        context.dataStore.edit { it[Keys.EPUB_LINE_SPACING] = spacing }
    }

    suspend fun updateEpubTheme(theme: ReaderTheme) {
        context.dataStore.edit { it[Keys.EPUB_THEME] = theme.name }
    }

    suspend fun updateKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }

    suspend fun updateWifiOnlyDownloads(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WIFI_ONLY_DOWNLOADS] = enabled }
    }

    suspend fun updateDailyReadingGoal(minutes: Int) {
        context.dataStore.edit { it[Keys.DAILY_READING_GOAL] = minutes }
    }

    suspend fun updatePdfNightMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PDF_NIGHT_MODE] = enabled }
    }
}
