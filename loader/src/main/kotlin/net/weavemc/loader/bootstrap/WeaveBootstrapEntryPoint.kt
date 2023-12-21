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

            fun File.createRemappedTemp(name: String): File {
                val temp = File.createTempFile(name, "weavemod.jar")
                MappingsHandler.remapModJar(
                    MappingsHandler.fullMappings,
                    this,
                    temp,
                    "official",
                    MappingsHandler.environmentNamespace,
                    listOf(FileManager.getVanillaMinecraftJar())
                )
                temp.deleteOnExit()
                return temp
            }

            val versionApi = FileManager.getVersionApi()
            val mods = FileManager.getMods().map { it.file }

            val mappedVersionApi = versionApi?.createRemappedTemp("version-api")
            val mappedMods = mods.map { it.createRemappedTemp(it.nameWithoutExtension) }

            urlClassLoaderAccessor.addWeaveURL(FileManager.getCommonApi().toURI().toURL())
            if (mappedVersionApi != null)
                urlClassLoaderAccessor.addWeaveURL(mappedVersionApi.toURI().toURL())

            mappedMods.forEach { urlClassLoaderAccessor.addWeaveURL(it.toURI().toURL()) }

            /*
            Load the rest of the loader using Minecraft's class loader.
            This allows us to access Minecraft's classes throughout the project.
            */
            loader.loadClass("net.weavemc.loader.WeaveLoader")
                .getDeclaredMethod("init", Instrumentation::class.java, File::class.java, List::class.java)
                .invoke(null, inst, mappedVersionApi, mappedMods)
        }

        return null
    }
}