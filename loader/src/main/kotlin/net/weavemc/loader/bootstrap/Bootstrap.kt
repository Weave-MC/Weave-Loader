package net.weavemc.loader.bootstrap

import net.weavemc.api.MinecraftClient
import net.weavemc.api.gameClient
import net.weavemc.api.gameVersion
import net.weavemc.loader.FileManager
import net.weavemc.loader.bootstrap.transformer.*
import net.weavemc.loader.bootstrap.transformer.SafeTransformer
import net.weavemc.loader.mapping.MappingsHandler
import java.io.File
import java.lang.instrument.Instrumentation
import java.net.URL
import java.util.jar.JarFile

class Bootstrap(val inst: Instrumentation) : SafeTransformer {
    override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray? {
        // Initialize Weave once the first Minecraft class is loaded into LaunchClassLoader (or main classloader for Minecraft)
        if (
            (gameClient != MinecraftClient.FORGE && className.startsWith("net/minecraft/client/")) ||
            (gameClient == MinecraftClient.FORGE && className == "net/minecraftforge/fml/common/Loader")
        ) {
            println("[Weave] Detected Minecraft version: $gameVersion")

            inst.removeTransformer(AntiCacheTransformer)
            inst.removeTransformer(this)

            val urlClassLoaderAccessor = if (loader is URLClassLoaderAccessor)
                loader
            else {
                println("[Weave] Failed to transform URLClassLoader to implement URLClassLoaderAccessor. Defaulting to SCL Search")
                object : URLClassLoaderAccessor {
                    override fun addWeaveURL(url: URL) {
                        inst.appendToSystemClassLoaderSearch(JarFile(url.file))
                    }
                }
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

            removeTransformers()

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

    private fun removeTransformers() {
        println("[Weave] Removing Bootstrapping Transformers")
        inst.removeTransformer(this)
        inst.removeTransformer(AntiCacheTransformer)
        inst.removeTransformer(URLClassLoaderTransformer)
        inst.removeTransformer(GameInfoTransformer)
        println("[Weave] Removed Bootstrapping Transformers")
    }
}