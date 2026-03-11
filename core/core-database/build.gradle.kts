plugins {
    id("kavita.android.library")
    id("kavita.android.hilt")
    id("kavita.android.room")
}

dependencies {
    implementation(project(":core:core-model"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
}
