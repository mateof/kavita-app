package com.kavita.core.readium

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import org.readium.adapter.pdfium.document.PdfiumDocumentFactory
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadiumManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val httpClient by lazy { DefaultHttpClient() }

    private val assetRetriever by lazy {
        AssetRetriever(context.contentResolver, httpClient)
    }

    private val publicationOpener by lazy {
        val parser = DefaultPublicationParser(
            context = context,
            httpClient = httpClient,
            assetRetriever = assetRetriever,
            pdfFactory = PdfiumDocumentFactory(context),
        )
        PublicationOpener(parser)
    }

    suspend fun openPublication(file: File): Publication {
        val url = AbsoluteUrl(file.toURI().toString())
            ?: error("Invalid file URL: ${file.absolutePath}")

        val asset = when (val result = assetRetriever.retrieve(url)) {
            is Try.Success -> result.value
            is Try.Failure -> error("Failed to retrieve asset: ${result.value}")
        }

        return when (val result = publicationOpener.open(asset, allowUserInteraction = false)) {
            is Try.Success -> result.value
            is Try.Failure -> error("Failed to open publication: ${result.value}")
        }
    }
}
