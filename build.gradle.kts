plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

// Workaround: NonNullableMutableLiveDataDetector crashes with Kotlin 2.1+
subprojects {
    tasks.withType<com.android.build.gradle.internal.lint.AndroidLintTask>().configureEach {
        enabled = false
    }
    tasks.withType<com.android.build.gradle.internal.lint.AndroidLintAnalysisTask>().configureEach {
        enabled = false
    }
    tasks.withType<com.android.build.gradle.internal.lint.AndroidLintTextOutputTask>().configureEach {
        enabled = false
    }
}

