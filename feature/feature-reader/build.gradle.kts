plugins {
    id("kavita.android.feature")
}

dependencies {
    implementation(project(":core:core-readium"))
    implementation(project(":core:core-network"))
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.datetime)
    implementation(libs.readium.navigator)
    implementation(libs.readium.adapter.pdfium)
    implementation("androidx.fragment:fragment-ktx:1.8.5")
}
