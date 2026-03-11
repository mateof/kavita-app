package com.kavita.core.data.repository

import android.util.Base64
import com.kavita.core.model.OpdsAcquisitionLink
import com.kavita.core.model.OpdsEntry
import com.kavita.core.model.OpdsFeed
import com.kavita.core.model.OpdsLinks
import com.kavita.core.model.repository.OpdsRepository
import kotlinx.datetime.Instant
import okhttp3.OkHttpClient
import okhttp3.Request
import org.readium.r2.opds.OPDS1Parser
import org.readium.r2.opds.OPDS2Parser
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(kotlin.time.ExperimentalTime::class)
class OpdsRepositoryImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
) : OpdsRepository {

    override suspend fun fetchFeed(
        url: String,
        username: String?,
        password: String?,
    ): Result<OpdsFeed> = withContext(Dispatchers.IO) {
        runCatching {
            val responseBytes = fetchUrl(url, username, password)
            parseFeed(responseBytes, url)
        }
    }

    override suspend fun searchCatalog(
        searchUrl: String,
        query: String,
    ): Result<OpdsFeed> = withContext(Dispatchers.IO) {
        runCatching {
            val expandedUrl = expandSearchUrl(searchUrl, query)
            val responseBytes = fetchUrl(expandedUrl, username = null, password = null)
            parseFeed(responseBytes, expandedUrl)
        }
    }

    /**
     * Descarga el contenido de la URL con autenticacion Basic opcional.
     */
    private fun fetchUrl(
        url: String,
        username: String?,
        password: String?,
    ): ByteArray {
        val requestBuilder = Request.Builder().url(url)
        if (username != null && password != null) {
            val credentials = "$username:$password"
            val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
            requestBuilder.addHeader("Authorization", "Basic $encoded")
        }
        val response = okHttpClient.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            throw Exception("Error HTTP ${response.code}: ${response.message}")
        }
        return response.body?.bytes() ?: throw Exception("Respuesta vacia")
    }

    /**
     * Expande la URL de busqueda con el parametro de consulta.
     * Soporta plantillas OpenSearch con {searchTerms} y URLs simples.
     */
    private fun expandSearchUrl(searchUrl: String, query: String): String {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        return if (searchUrl.contains("{searchTerms}")) {
            searchUrl.replace("{searchTerms}", encodedQuery)
        } else {
            val separator = if (searchUrl.contains("?")) "&" else "?"
            "$searchUrl${separator}q=$encodedQuery"
        }
    }

    /**
     * Intenta parsear los bytes como OPDS 1.x (Atom XML) y, si falla,
     * como OPDS 2.0 (JSON). Mapea el resultado al modelo de dominio.
     */
    private fun parseFeed(data: ByteArray, feedUrl: String): OpdsFeed {
        val url = Url(feedUrl)
            ?: throw Exception("URL no valida: $feedUrl")

        // Intentar OPDS 1.x (formato Atom XML, el mas comun)
        val opds1Result = runCatching { OPDS1Parser.parse(data, url) }
        if (opds1Result.isSuccess) {
            val parseData = opds1Result.getOrThrow()
            if (parseData.feed != null) {
                return mapReadiumFeedToDomain(parseData.feed!!, feedUrl)
            }
            // Si solo hay una publicacion suelta, envolverla en un feed
            if (parseData.publication != null) {
                return wrapPublicationAsFeed(parseData.publication!!, feedUrl)
            }
        }

        // Intentar OPDS 2.0 (formato JSON)
        val opds2Result = runCatching { OPDS2Parser.parse(data, url) }
        if (opds2Result.isSuccess) {
            val parseData = opds2Result.getOrThrow()
            if (parseData.feed != null) {
                return mapReadiumFeedToDomain(parseData.feed!!, feedUrl)
            }
            if (parseData.publication != null) {
                return wrapPublicationAsFeed(parseData.publication!!, feedUrl)
            }
        }

        // Si ambos parsers fallan, lanzar excepcion con detalles
        val opds1Error = opds1Result.exceptionOrNull()
        val opds2Error = opds2Result.exceptionOrNull()
        throw Exception(
            "No se pudo parsear el feed OPDS. " +
                "OPDS1: ${opds1Error?.message ?: "sin datos"}, " +
                "OPDS2: ${opds2Error?.message ?: "sin datos"}"
        )
    }

    /**
     * Mapea el Feed de Readium al modelo OpdsFeed de dominio.
     */
    private fun mapReadiumFeedToDomain(
        feed: org.readium.r2.shared.opds.Feed,
        feedUrl: String,
    ): OpdsFeed {
        val entries = mutableListOf<OpdsEntry>()

        // Mapear publicaciones (entradas con links de adquisicion)
        feed.publications.forEach { publication ->
            entries.add(mapPublicationToEntry(publication, feedUrl))
        }

        // Mapear navegacion (entradas con links de navegacion)
        feed.navigation.forEach { navLink ->
            entries.add(mapNavigationLinkToEntry(navLink, feedUrl))
        }

        // Mapear grupos: cada grupo puede contener publicaciones y navegacion
        feed.groups.forEach { group ->
            group.publications.forEach { publication ->
                entries.add(mapPublicationToEntry(publication, feedUrl))
            }
            group.navigation.forEach { navLink ->
                entries.add(mapNavigationLinkToEntry(navLink, feedUrl))
            }
        }

        // Extraer links del feed
        val links = mapFeedLinks(feed.links, feedUrl)

        // Buscar URL de busqueda en los links del feed
        val searchUrl = findSearchUrl(feed.links, feedUrl)

        return OpdsFeed(
            title = feed.title,
            entries = entries,
            links = links,
            searchUrl = searchUrl,
        )
    }

    /**
     * Mapea una Publication de Readium a OpdsEntry con links de adquisicion.
     */
    private fun mapPublicationToEntry(
        publication: Publication,
        feedBaseUrl: String,
    ): OpdsEntry {
        val metadata = publication.metadata

        // Autores: concatenar los nombres de los autores
        val authorName = metadata.authors
            .takeIf { it.isNotEmpty() }
            ?.joinToString(", ") { it.name }

        // Resumen / descripcion
        val summary = metadata.description

        // URL de portada: buscar en links con rel "http://opds-spec.org/image"
        // o "http://opds-spec.org/image/thumbnail"
        val coverUrl = publication.linkWithRel("http://opds-spec.org/image")
            ?.let { resolveHref(it, feedBaseUrl) }
            ?: publication.linkWithRel("http://opds-spec.org/image/thumbnail")
                ?.let { resolveHref(it, feedBaseUrl) }

        // Links de adquisicion
        val acquisitionLinks = publication.links
            .filter { link -> link.rels.any { it.contains("acquisition") } }
            .map { link ->
                OpdsAcquisitionLink(
                    href = resolveHref(link, feedBaseUrl),
                    type = link.mediaType?.toString() ?: "application/octet-stream",
                    rel = link.rels.firstOrNull { it.contains("acquisition") } ?: "",
                    title = link.title,
                    fileSize = link.properties.otherProperties["length"]?.let {
                        (it as? Number)?.toLong()
                    },
                )
            }

        // Link de navegacion: si no hay links de adquisicion, buscar un link
        // de navegacion (subsection, alternate, etc.)
        val navigationLink = if (acquisitionLinks.isEmpty()) {
            publication.links
                .firstOrNull { link ->
                    link.rels.any {
                        it == "subsection" || it == "http://opds-spec.org/sort/new" ||
                            it == "http://opds-spec.org/sort/popular" || it == "alternate"
                    }
                }
                ?.let { resolveHref(it, feedBaseUrl) }
                ?: publication.links.firstOrNull()?.let { resolveHref(it, feedBaseUrl) }
        } else {
            null
        }

        // Fecha de actualizacion
        val updated = metadata.published?.let { instant ->
            runCatching { Instant.parse(instant.toString()) }.getOrNull()
        }

        return OpdsEntry(
            id = metadata.identifier ?: publication.hashCode().toString(),
            title = metadata.title ?: "Sin titulo",
            author = authorName,
            summary = summary,
            coverUrl = coverUrl,
            acquisitionLinks = acquisitionLinks,
            navigationLink = navigationLink,
            updated = updated,
        )
    }

    /**
     * Mapea un Link de navegacion de Readium a OpdsEntry.
     */
    private fun mapNavigationLinkToEntry(
        link: Link,
        feedBaseUrl: String,
    ): OpdsEntry {
        return OpdsEntry(
            id = link.href.toString(),
            title = link.title ?: link.href.toString(),
            navigationLink = resolveHref(link, feedBaseUrl),
        )
    }

    /**
     * Envuelve una publicacion suelta en un OpdsFeed.
     */
    private fun wrapPublicationAsFeed(
        publication: Publication,
        feedUrl: String,
    ): OpdsFeed {
        val entry = mapPublicationToEntry(publication, feedUrl)
        return OpdsFeed(
            title = entry.title,
            entries = listOf(entry),
            links = OpdsLinks(self = feedUrl),
        )
    }

    /**
     * Extrae los links del feed (self, next, search, start).
     */
    private fun mapFeedLinks(links: List<Link>, feedUrl: String): OpdsLinks {
        return OpdsLinks(
            self = links.firstOrNull { "self" in it.rels }
                ?.let { resolveHref(it, feedUrl) }
                ?: feedUrl,
            next = links.firstOrNull { "next" in it.rels }
                ?.let { resolveHref(it, feedUrl) },
            search = links.firstOrNull { "search" in it.rels }
                ?.let { resolveHref(it, feedUrl) },
            start = links.firstOrNull { "start" in it.rels }
                ?.let { resolveHref(it, feedUrl) },
        )
    }

    /**
     * Busca la URL de busqueda en los links del feed.
     * Soporta links directos y plantillas OpenSearch.
     */
    private fun findSearchUrl(links: List<Link>, feedBaseUrl: String): String? {
        val searchLink = links.firstOrNull { "search" in it.rels }
            ?: return null
        return resolveHref(searchLink, feedBaseUrl)
    }

    /**
     * Resuelve el href de un Link contra la URL base del feed.
     */
    private fun resolveHref(link: Link, feedBaseUrl: String): String {
        val baseUrl = Url(feedBaseUrl)
        val resolved = link.url(base = baseUrl)
        return resolved.toString()
    }
}
