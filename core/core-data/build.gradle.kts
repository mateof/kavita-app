plugins {
    id("kavita.android.library")
    id("kavita.android.hilt")
}

dependencies {
    implementation(project(":core:core-model"))
    implementation(project(":core:core-common"))
    implementation(project(":core:core-network"))
    implementation(project(":core:core-database"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.core)
    implementation(libs.datastore.preferences)
    implementation(libs.paging.runtime)
    implementation(libs.work.runtime)
    implementation(libs.readium.opds)
    implementation(libs.readium.shared)
}
