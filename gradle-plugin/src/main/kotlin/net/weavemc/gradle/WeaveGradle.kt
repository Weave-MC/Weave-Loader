package net.weavemc.gradle

import com.grappenmaker.mappings.format.Mappings
import kotlinx.serialization.encodeToString
import net.weavemc.gradle.configuration.WeaveMinecraftExtension
import net.weavemc.gradle.configuration.pullDeps
import net.weavemc.gradle.util.Constants
import net.weavemc.gradle.util.localCache
import net.weavemc.gradle.util.minecraftJarCache
import net.weavemc.internals.MappingsRetrieval
import net.weavemc.internals.MinecraftVersion
import net.weavemc.internals.ModConfig
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

/**
 * Gradle build system plugin used to automate the setup of a modding environment.
 */
public class WeaveGradle : Plugin<Project> {
    /**
     * [Plugin.apply]
     *
     * @param project The target project.
     */
    override fun apply(project: Project) {
        // Applying our default plugins
        project.pluginManager.apply(JavaPlugin::class.java)

        val ext = project.extensions.create(Constants.WEAVE_EXTENSION, WeaveMinecraftExtension::class)

        project.afterEvaluate {
            val configuration = ext.configuration.orNull
            val versionProvider = ext.version.orNull

            if (configuration == null) {
                project.logger.warn("WARNING: Configuration is missing! Make sure to add a configuration through the weave { } block.")
                return@afterEvaluate
            }

            if (versionProvider == null) {
                project.logger.warn("WARNING: Minecraft version is missing in the weave { } block. Defaulting to 1.8.9.")
            }

            val version = versionProvider ?: MinecraftVersion.V1_8_9
            it.pullDeps(ext, version, configuration.namespace)
        }

        val writeModConfig = project.tasks.register<WriteModConfig>("writeModConfig") {
            configuration.set(ext.configuration)
            output.set(project.localCache().map { it.file("weave.mod.json") })
        }

        project.tasks.named<Jar>("jar") {
            dependsOn(writeModConfig)
            from(writeModConfig)
        }

        project.tasks.named<Delete>("clean") { delete(writeModConfig) }
    }

    @CacheableTask
    public abstract class WriteModConfig : DefaultTask() {
        @get:Internal
        public abstract val configuration: Property<ModConfig>

        @get:Input
        protected val configurationJson: Provider<String> = configuration.map {
            Constants.JSON.encodeToString(it)
        }

        @get:OutputFile
        public abstract val output: RegularFileProperty

        @TaskAction
        public fun run() {
            val json = configurationJson.get()
            val outputFile = output.get().asFile
            outputFile.parentFile?.mkdirs()
            outputFile.writeText(json)
        }
    }
}

public fun MinecraftVersion.loadMergedMappings(): Mappings =
    MappingsRetrieval.loadMergedWeaveMappings(mappingName, minecraftJarCache).mappings

public val Project.sourceSets: SourceSetContainer
    get() = extensions.getByName<SourceSetContainer>("sourceSets")
