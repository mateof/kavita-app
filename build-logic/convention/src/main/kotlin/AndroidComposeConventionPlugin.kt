import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.plugin.compose")
            }

            extensions.configure<LibraryExtension> {
                buildFeatures {
                    compose = true
                }
            }

            dependencies {
                val bom = platform("androidx.compose:compose-bom:2024.12.01")
                add("implementation", bom)
                add("implementation", "androidx.compose.material3:material3")
                add("implementation", "androidx.compose.ui:ui")
                add("implementation", "androidx.compose.ui:ui-graphics")
                add("implementation", "androidx.compose.ui:ui-tooling-preview")
                add("implementation", "androidx.compose.foundation:foundation")
                add("implementation", "androidx.compose.runtime:runtime")
                add("implementation", "androidx.compose.animation:animation")
                add("debugImplementation", "androidx.compose.ui:ui-tooling")
                add("implementation", "androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
                add("implementation", "androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
            }
        }
    }
}
