package com.kavita.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.kavita.core.model.AppTheme
import com.kavita.core.model.ReadingDirection
import com.kavita.core.model.UserPreferences
import com.kavita.core.data.preferences.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesDataStore: UserPreferencesDataStore,
    private val imageLoader: ImageLoader,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesDataStore.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    private val _imageCacheSizeBytes = MutableStateFlow(0L)
    val imageCacheSizeBytes: StateFlow<Long> = _imageCacheSizeBytes.asStateFlow()

    init {
        refreshCacheSize()
    }

    private fun refreshCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            _imageCacheSizeBytes.value = imageLoader.diskCache?.size ?: 0L
        }
    }

    fun clearImageCache() {
        viewModelScope.launch(Dispatchers.IO) {
            imageLoader.diskCache?.clear()
            imageLoader.memoryCache?.clear()
            refreshCacheSize()
        }
    }

    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch { preferencesDataStore.updateTheme(theme) }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.updateDynamicColor(enabled) }
    }

    fun updateReadingDirection(direction: ReadingDirection) {
        viewModelScope.launch { preferencesDataStore.updateReadingDirection(direction) }
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.updateKeepScreenOn(enabled) }
    }

    fun updateWifiOnlyDownloads(enabled: Boolean) {
        viewModelScope.launch { preferencesDataStore.updateWifiOnlyDownloads(enabled) }
    }

    fun updateDailyGoal(minutes: Int) {
        viewModelScope.launch { preferencesDataStore.updateDailyReadingGoal(minutes) }
    }
}
