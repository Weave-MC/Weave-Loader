package net.weavemc.gradle

import kotlinx.serialization.encodeToString
import net.weavemc.gradle.configuration.*
import net.weavemc.internals.*
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.*
import java.net.URL
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.io.path.*

val weaveGradleVersion by lazy {
    (object {}).javaClass.classLoader.getResourceAsStream("weave-gradle-version")?.readBytes()?.decodeToString()
}

/**
 * Gradle build system plugin used to automate the setup of a modding environment.
 */
class WeaveGradle : Plugin<Project> {
    /**
     * [Plugin.apply]
     *
     * @param project The target project.
     */
    @OptIn(ExperimentalPathApi::class)
    override fun apply(project: Project) {
        // Applying our default plugins
        project.pluginManager.apply(JavaPlugin::class)

        val ext = project.extensions.create("weave", WeaveMinecraftExtension::class)

        val devLaunch by project.configurations.creating { isCanBeResolved = true }
        val devWeaveLoader by project.configurations.creating {
            isCanBeResolved = true
            isTransitive = false
        }

        if (weaveGradleVersion != null) {
            project.dependencies.add(devLaunch.name, "net.weavemc:weave-gradle-launcher:$weaveGradleVersion") {
                exclude(group = "net.weavemc") // exclude unrelocated transitive dependencies: they are in the agent
            }

            project.dependencies.add(devWeaveLoader.name, "net.weavemc:loader:$weaveGradleVersion:all")
        }

        val writeModConfig = project.tasks.register<WriteModConfig>("writeModConfig")

        val jarProvider = project.tasks.named<Jar>("jar") {
            dependsOn(writeModConfig)
            from(writeModConfig)
        }

        project.tasks.named<Delete>("clean") { delete(writeModConfig) }

        val version by lazy { ext.version.getOrElse(MinecraftVersion.V1_8_9) }
        val versionInfo by lazy {
            fetchVersionManifest()?.fetchVersion(version.versionName)
                ?: throw GradleException("Could not fetch Minecraft version $version")
        }

        val launchGame by project.tasks.registering(JavaExec::class) {
            val toolchains = project.extensions.getByType<JavaToolchainService>()
            val java = project.extensions.getByType<JavaPluginExtension>()

            javaLauncher.set(toolchains.launcherFor {
                languageVersion.set(
                    JavaLanguageVersion.of(
                        maxOf(
                            versionInfo.javaVersion.majorVersion,
                            java.toolchain.languageVersion.get().asInt()
                        )
                    )
                )

                vendor.set(java.toolchain.vendor)
                implementation.set(java.toolchain.implementation)
            })

            dependsOn(jarProvider)
            workingDir(project.localGradleCache().dir("game-workdir").also { it.asFile.mkdirs() })
            mainClass.set("net.weavemc.gradle.LauncherKt")
            args(version.versionName, "--version", version.versionName)
            classpath(devLaunch)

            doFirst {
                val librariesDir = project.localGradleCache().dir("game-libraries").asFile.toPath()
                val nativesDir = project.localGradleCache().dir("game-natives").asFile.toPath().also {
                    it.deleteRecursively()
                    it.createDirectories()
                }

                val allNatives = versionInfo.relevantLibraries.asSequence().mapNotNull { it.downloads.natives }
                val allNativePaths = mutableListOf<Path>()

                for (native in allNatives) {
                    val target = native.path.split('/').fold(librariesDir) { acc, curr -> acc.resolve(curr) }
                    allNativePaths.add(target)
                    if (!target.exists()) DownloadUtil.download(URL(native.url), target)
                }

                for (zip in allNativePaths) runCatching { unzip(zip, nativesDir) }.onFailure {
                    println("Failed unzipping $zip, ignoring")
                    it.printStackTrace()
                }

                if (devWeaveLoader.files.size != 1) throw GradleException(
                    "Weave-Loader unexpectedly is comprised of multiple files: ${devWeaveLoader.files}"
                )

                jvmArgs(
                    "-javaagent:${devWeaveLoader.singleFile.absolutePath}",
                    "-Djava.library.path=${nativesDir.absolutePathString()}",
                    "-Dweave.devlauncher.mods=${jarProvider.get().outputs.files.singleFile.absolutePath}"
                )
            }
        }

        project.afterEvaluate {
            if (!ext.configuration.isPresent) throw GradleException(
                "Configuration is missing, make sure to add a configuration through the weave {} block!"
            )

            if (!ext.version.isPresent) throw GradleException(
                "Set a Minecraft version through the weave {} block!"
            )

            pullDeps(version, versionInfo, ext.configuration.get().namespace)
        }
    }

    open class WriteModConfig : DefaultTask() {
        @get:OutputFile
        val output = project.localCache().map { it.file("weave.mod.json") }

        @TaskAction
        fun run() {
            val config = project.extensions.getByName<WeaveMinecraftExtension>("weave").configuration.get()
            output.get().asFile.writeText(Constants.JSON.encodeToString(config))
        }
    }
}

fun MinecraftVersion.loadMergedMappings() =
    MappingsRetrieval.loadMergedWeaveMappings(versionName, minecraftJarCache).mappings

val Project.sourceSets get() = extensions.getByName<SourceSetContainer>("sourceSets")

private fun unzip(file: Path, to: Path) = ZipInputStream(file.inputStream()).use { zip ->
    generateSequence { zip.nextEntry }.filterNot { it.isDirectory }.forEach { entry ->
        entry.name.split("/")
            .fold(to) { acc, curr -> acc.resolve(curr) }
            .also { it.parent.createDirectories() }
            .writeBytes(zip.readBytes())
    }
}