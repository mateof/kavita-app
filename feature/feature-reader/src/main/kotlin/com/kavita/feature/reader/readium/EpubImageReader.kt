package com.kavita.feature.reader.readium

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.EpubLayout
import org.readium.r2.shared.publication.presentation.presentation
import org.readium.r2.shared.util.Url

/**
 * Soporte para EPUB basados en imágenes (cómic / fixed-layout). La detección decide si un
 * EPUB debe leerse con el lector de imágenes (ComicReader), y [rememberEpubPageModel] extrae
 * la imagen de cada página del propio EPUB para que Coil la muestre.
 */

private const val TAG = "EpubImageReader"

// Referencia a un fichero de imagen, ya sea en <img src>, SVG <image xlink:href>, o CSS url(...)
private val IMAGE_REF = Regex(
    """(?:src|href|xlink:href)\s*=\s*["']([^"']+\.(?:jpe?g|png|webp|gif|bmp|avif))["']""",
    RegexOption.IGNORE_CASE,
)
private val CSS_IMAGE_URL = Regex(
    """url\(\s*["']?([^"')]+\.(?:jpe?g|png|webp|gif|bmp|avif))["']?\s*\)""",
    RegexOption.IGNORE_CASE,
)
private val HTML_TAGS = Regex("(?s)<[^>]+>")
private val HTML_ENTITIES = Regex("&[a-zA-Z#0-9]+;")
private val WHITESPACE = Regex("\\s+")

private fun findImageRef(html: String): String? =
    IMAGE_REF.find(html)?.groupValues?.getOrNull(1)
        ?: CSS_IMAGE_URL.find(html)?.groupValues?.getOrNull(1)

/**
 * Carga (de forma asíncrona) los bytes de la imagen de la página [link] del EPUB y los
 * devuelve como modelo para Coil (ByteArray). null mientras carga o si falla.
 */
@OptIn(ExperimentalReadiumApi::class)
@Composable
fun rememberEpubPageModel(publication: Publication, link: Link): Any? {
    var model by remember(link) { mutableStateOf<Any?>(null) }
    LaunchedEffect(link) {
        model = withContext(Dispatchers.IO) { publication.imageBytesForPage(link) }
    }
    return model
}

/**
 * Decide si un EPUB debe tratarse como cómic/imágenes. Cubre tres casos:
 * 1. Declara fixed-layout en su metadata.
 * 2. El reading order ya son recursos de imagen (estilo Divina).
 * 3. Las páginas de contenido son XHTML que básicamente envuelven una sola imagen (cómics
 *    exportados como EPUB reflowable que Readium no escala bien).
 */
@OptIn(ExperimentalReadiumApi::class)
suspend fun Publication.isImageBasedEpub(): Boolean = withContext(Dispatchers.IO) {
    val ro = readingOrder
    if (ro.isEmpty()) return@withContext false

    val layout = metadata.presentation.layout
    if (layout == EpubLayout.FIXED) {
        Log.d(TAG, "isImageBasedEpub=true (metadata layout=FIXED)")
        return@withContext true
    }
    if (ro.all { it.mediaType?.isBitmap == true }) {
        Log.d(TAG, "isImageBasedEpub=true (reading order de imágenes)")
        return@withContext true
    }

    // Muestrear hasta 8 páginas repartidas, saltando las 2 primeras
    val body = if (ro.size > 4) ro.subList(2, ro.size) else ro
    val step = maxOf(1, body.size / 8)
    val sample = body.filterIndexed { index, _ -> index % step == 0 }.take(8)
    if (sample.isEmpty()) return@withContext false

    var readable = 0
    var imageLike = 0
    var firstSnippet = ""
    for (link in sample) {
        if (link.mediaType?.isBitmap == true) {
            readable++
            imageLike++
            continue
        }
        val html = readBytes(link)?.decodeToString() ?: continue
        readable++
        if (firstSnippet.isEmpty()) firstSnippet = html.take(400)
        if (looksLikeSingleImagePage(html)) imageLike++
    }

    val ratio = if (readable > 0) imageLike.toFloat() / readable else 0f
    val result = readable > 0 && ratio >= 0.6f
    Log.d(
        TAG,
        "isImageBasedEpub=$result (layout=$layout, readingOrder=${ro.size}, " +
            "muestra=$readable, imageLike=$imageLike, ratio=$ratio)\nsnippet=$firstSnippet",
    )
    result
}

private fun looksLikeSingleImagePage(html: String): Boolean {
    if (findImageRef(html) == null) return false
    val text = html
        .replace(HTML_TAGS, " ")
        .replace(HTML_ENTITIES, " ")
        .replace(WHITESPACE, " ")
        .trim()
    return text.length < 60
}

@OptIn(ExperimentalReadiumApi::class)
private suspend fun Publication.imageBytesForPage(link: Link): ByteArray? {
    val url = imageUrlForPage(link) ?: return null
    return readBytes(url)
}

@OptIn(ExperimentalReadiumApi::class)
private suspend fun Publication.imageUrlForPage(link: Link): Url? {
    if (link.mediaType?.isBitmap == true) return link.url()
    val html = readBytes(link)?.decodeToString() ?: return null
    val raw = findImageRef(html)
    if (raw == null) {
        Log.w(TAG, "Sin imagen en página ${link.url()}\nsnippet=${html.take(400)}")
        return null
    }
    val relative = Url(raw.trim()) ?: return null
    return link.url().resolve(relative)
}

@OptIn(ExperimentalReadiumApi::class)
private suspend fun Publication.readBytes(link: Link): ByteArray? {
    val resource = get(link) ?: return null
    return try {
        resource.read().getOrNull()
    } finally {
        resource.close()
    }
}

@OptIn(ExperimentalReadiumApi::class)
private suspend fun Publication.readBytes(url: Url): ByteArray? {
    val resource = get(url) ?: return null
    return try {
        resource.read().getOrNull()
    } finally {
        resource.close()
    }
}
