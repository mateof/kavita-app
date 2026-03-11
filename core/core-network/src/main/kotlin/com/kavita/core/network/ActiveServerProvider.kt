package com.kavita.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActiveServerProvider @Inject constructor() {

    private val _activeServerUrl = MutableStateFlow<String?>(null)
    val activeServerUrl: StateFlow<String?> = _activeServerUrl.asStateFlow()

    private val _activeServerId = MutableStateFlow<Long?>(null)
    val activeServerId: StateFlow<Long?> = _activeServerId.asStateFlow()

    private val _activeApiKey = MutableStateFlow<String?>(null)

    fun setActiveServer(id: Long, url: String, apiKey: String? = null) {
        _activeServerId.value = id
        _activeServerUrl.value = url.trimEnd('/')
        _activeApiKey.value = apiKey
    }

    fun clear() {
        _activeServerId.value = null
        _activeServerUrl.value = null
        _activeApiKey.value = null
    }

    fun requireUrl(): String =
        _activeServerUrl.value ?: error("No hay servidor activo configurado")

    fun requireId(): Long =
        _activeServerId.value ?: error("No hay servidor activo configurado")

    fun getApiKey(): String? = _activeApiKey.value
}
