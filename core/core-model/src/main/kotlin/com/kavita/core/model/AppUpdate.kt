package com.kavita.core.model

/**
 * Información de una versión de la app publicada en GitHub Releases.
 */
data class AppUpdate(
    /** Versión limpia, sin la 'v' del tag (p. ej. "0.2.0"). */
    val versionName: String,
    /** URL de la página de la release en GitHub (para abrir en el navegador). */
    val releaseUrl: String,
    /** URL de descarga directa del APK, o null si la release no adjunta APK. */
    val apkUrl: String?,
    /** Tamaño del APK en bytes (0 si se desconoce). */
    val apkSizeBytes: Long,
    /** Notas de la release / changelog (puede ser null). */
    val notes: String?,
)
