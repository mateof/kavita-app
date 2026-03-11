import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("kavita.android.library")
                apply("kavita.android.compose")
                apply("kavita.android.hilt")
            }

            dependencies {
                add("implementation", project(":core:core-model"))
                add("implementation", project(":core:core-data"))
                add("implementation", project(":core:core-ui"))
                add("implementation", project(":core:core-common"))

                add("implementation", "androidx.hilt:hilt-navigation-compose:1.2.0")
                add("implementation", "androidx.navigation:navigation-compose:2.8.5")
                add("implementation", "androidx.paging:paging-compose:3.3.5")
            }
        }
    }
}
