package com.kavita.core.model

data class SeriesMetadata(
    val summary: String = "",
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val writers: List<String> = emptyList(),
    val coverArtists: List<String> = emptyList(),
    val publishers: List<String> = emptyList(),
    val pencillers: List<String> = emptyList(),
    val colorists: List<String> = emptyList(),
    val letterers: List<String> = emptyList(),
    val editors: List<String> = emptyList(),
    val translators: List<String> = emptyList(),
    val characters: List<String> = emptyList(),
    val ageRating: AgeRating = AgeRating.UNKNOWN,
    val releaseYear: Int = 0,
    val language: String = "",
    val publicationStatus: PublicationStatus = PublicationStatus.ONGOING,
    val totalCount: Int = 0,
    val maxCount: Int = 0,
    val webLinks: String = "",
)

enum class AgeRating(val label: String) {
    UNKNOWN("Desconocido"),
    RATING_PENDING("Pendiente de clasificar"),
    EARLY_CHILDHOOD("Primera infancia"),
    EVERYONE("Para todos"),
    G("G"),
    EVERYONE_10PLUS("Mayores de 10"),
    PG("PG"),
    KIDS_TO_ADULTS("De niños a adultos"),
    TEEN("Adolescentes"),
    MA_15PLUS("Mayores de 15"),
    MATURE_17PLUS("Mayores de 17"),
    M("M"),
    R_18PLUS("Mayores de 18"),
    ADULTS_ONLY("Solo adultos"),
    X_18PLUS("X18+"),
}

enum class PublicationStatus(val label: String) {
    ONGOING("En curso"),
    HIATUS("En pausa"),
    COMPLETED("Completado"),
    CANCELLED("Cancelado"),
    ENDED("Finalizado"),
}
