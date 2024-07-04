package net.weavemc.gradle

import kotlinx.serialization.encodeToString
import net.weavemc.gradle.configuration.WeaveMinecraftExtension
import net.weavemc.gradle.configuration.pullDeps
import net.weavemc.gradle.util.Constants
import net.weavemc.gradle.util.localCache
import net.weavemc.gradle.util.minecraftJarCache
import net.weavemc.internals.MappingsRetrieval
import net.weavemc.internals.MinecraftVersion
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*

/**
 * Gradle build system plugin used to automate the setup of a modding environment.
 */
class WeaveGradle : Plugin<Project> {
    /**
     * [Plugin.apply]
     *
     * @param project The target project.
     */
    override fun apply(project: Project) {
        // Applying our default plugins
        project.pluginManager.apply(JavaPlugin::class)

        val ext = project.extensions.create("weave", WeaveMinecraftExtension::class)

        project.afterEvaluate {
            if (!ext.configuration.isPresent) throw GradleException(
                "Configuration is missing, make sure to add a configuration through the weave {} block!"
            )

            if (!ext.version.isPresent) throw GradleException(
                "Set a Minecraft version through the weave {} block!"
            )

            val version = ext.version.getOrElse(MinecraftVersion.V1_8_9)
            pullDeps(version, ext.configuration.get().namespace)
        }

        val writeModConfig = project.tasks.register<WriteModConfig>("writeModConfig")

        project.tasks.named<Jar>("jar") {
            dependsOn(writeModConfig)
            from(writeModConfig)
        }

        project.tasks.named<Delete>("clean") { delete(writeModConfig) }
    }

    open class WriteModConfig : DefaultTask() {
        @get:OutputFile
        val output = project.localCache().map { it.file("weave.mod.json") }

        @TaskAction
        fun run() {
            val config = project.extensions.getByName<WeaveMinecraftExtension>("minecraft").configuration.get()
            output.get().asFile.writeText(Constants.JSON.encodeToString(config))
        }
    }
}

fun MinecraftVersion.loadMergedMappings() =
    MappingsRetrieval.loadMergedWeaveMappings(versionName, minecraftJarCache).mappings


val Project.sourceSets get() = extensions.getByName<SourceSetContainer>("sourceSets")
