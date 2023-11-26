package net.weavemc.loader.bootstrap

import net.weavemc.api.GameInfo
import net.weavemc.api.gameClient
import net.weavemc.api.gameLauncher
import net.weavemc.api.gameVersion
import net.weavemc.loader.FileManager
import net.weavemc.loader.JSON
import net.weavemc.loader.fetchModConfig
import net.weavemc.loader.mapping.MappingsHandler
import java.io.File
import java.lang.instrument.Instrumentation
import java.net.URL
import java.util.jar.JarFile

class WeaveBootstrapEntryPoint(val inst: Instrumentation) : SafeTransformer {
    override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray? {
        // Initialize Weave once the first Minecraft class is loaded into LaunchClassLoader (or main classloader for Minecraft)
        if (
            (gameClient != GameInfo.Client.FORGE && className.startsWith("net/minecraft/client/")) ||
            (gameClient == GameInfo.Client.FORGE && className == "net/minecraftforge/fml/common/Loader")
        ) {
            println("[Weave] Detected Minecraft version: $gameVersion")

            inst.removeTransformer(AntiLunarCache)
            inst.removeTransformer(this)

            val urlClassLoaderAccessor =
                if (loader is URLClassLoaderAccessor) {
                    loader
                } else if (gameLauncher == GameInfo.Launcher.MULTIMC || gameLauncher == GameInfo.Launcher.PRISM) {
                    object : URLClassLoaderAccessor {
                        override fun addWeaveURL(url: URL) {
                            inst.appendToSystemClassLoaderSearch(JarFile(url.file))
                        }
                    }
                } else {
                    error("ClassLoader was not transformed to implement URLClassLoaderAccessor interface and neither MultiMC nor Prism were detected. Report to Developers.")
                }

            val versionApiFile = FileManager.getVersionApi()
            val versionApi = versionApiFile?.fetchModConfig(JSON)?.mappings?.let { target ->
                val temp = File.createTempFile("version-api", "weavemod.jar")
                MappingsHandler.remapModJar(
                    MappingsHandler.fullMappings,
                    versionApiFile,
                    temp,
                    target,
                    MappingsHandler.environmentNamespace,
                    listOf(FileManager.getVanillaMinecraftJar())
                )
                temp.deleteOnExit()
                temp
            }

            val modFiles = FileManager.getMods().map { it.file }
            val mods = modFiles.map { unmappedMod ->
                unmappedMod.fetchModConfig(JSON).mappings.let { target ->
                    val temp = File.createTempFile(unmappedMod.nameWithoutExtension, "weavemod.jar")
                    MappingsHandler.remapModJar(
                        MappingsHandler.fullMappings,
                        unmappedMod,
                        temp,
                        target,
                        MappingsHandler.environmentNamespace,
                        listOf(FileManager.getVanillaMinecraftJar())
                    )
                    temp.deleteOnExit()
                    temp
                }
            }

            urlClassLoaderAccessor.addWeaveURL(FileManager.getCommonApi().toURI().toURL())
            if (versionApi != null)
                urlClassLoaderAccessor.addWeaveURL(versionApi.toURI().toURL())

            mods.forEach { urlClassLoaderAccessor.addWeaveURL(it.toURI().toURL()) }

            /*
            Load the rest of the loader using Minecraft's class loader.
            This allows us to access Minecraft's classes throughout the project.
            */
            loader.loadClass("net.weavemc.loader.WeaveLoader")
                .getDeclaredMethod("init", Instrumentation::class.java, File::class.java, List::class.java)
                .invoke(null, inst, versionApi, mods)
        }

        return null
    }
}