package com.kavita.core.model

import kotlinx.datetime.Instant

data class OpdsFeed(
    val title: String,
    val entries: List<OpdsEntry>,
    val links: OpdsLinks,
    val searchUrl: String? = null,
)

data class OpdsEntry(
    val id: String,
    val title: String,
    val author: String? = null,
    val summary: String? = null,
    val coverUrl: String? = null,
    val acquisitionLinks: List<OpdsAcquisitionLink> = emptyList(),
    val navigationLink: String? = null,
    val updated: Instant? = null,
)

data class OpdsAcquisitionLink(
    val href: String,
    val type: String,
    val rel: String = "",
    val title: String? = null,
    val fileSize: Long? = null,
)

data class OpdsLinks(
    val self: String? = null,
    val next: String? = null,
    val search: String? = null,
    val start: String? = null,
)
