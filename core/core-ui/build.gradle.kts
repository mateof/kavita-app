plugins {
    id("kavita.android.library")
    id("kavita.android.compose")
    id("kavita.android.hilt")
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-common"))

    api(libs.coil.compose)
    api(libs.compose.icons.extended)
}
