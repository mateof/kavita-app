plugins {
    id("kavita.android.library")
    id("kavita.android.hilt")
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-common"))
    implementation(project(":core:core-network"))

    // Readium - exposed via api so consumers can use Publication, Navigator, etc.
    api(libs.readium.shared)
    api(libs.readium.streamer)
    api(libs.readium.navigator)
    api(libs.readium.adapter.pdfium)
    implementation(libs.readium.opds)
    implementation(libs.kotlinx.coroutines.android)
}
