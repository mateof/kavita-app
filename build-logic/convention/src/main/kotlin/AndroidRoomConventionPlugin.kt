import androidx.room.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("androidx.room")
                apply("com.google.devtools.ksp")
            }

            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }

            dependencies {
                add("implementation", "androidx.room:room-runtime:2.6.1")
                add("implementation", "androidx.room:room-ktx:2.6.1")
                add("ksp", "androidx.room:room-compiler:2.6.1")
                add("implementation", "androidx.room:room-paging:2.6.1")
            }
        }
    }
}
