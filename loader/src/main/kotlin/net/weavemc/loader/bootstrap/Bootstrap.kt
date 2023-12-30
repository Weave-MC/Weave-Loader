package net.weavemc.loader.bootstrap

import net.weavemc.api.gameClient
import net.weavemc.api.gameVersion
import net.weavemc.loader.FileManager
import net.weavemc.loader.bootstrap.transformer.*
import net.weavemc.loader.mapping.MappingsHandler
import java.io.File
import java.lang.instrument.Instrumentation
import java.net.URL
import java.util.jar.JarFile

object BootstrapContainer {
    lateinit var bootstrap: Bootstrap

    @JvmStatic
    fun bootstrap(caller: String, loader: ClassLoader, args: Array<String>) =
        bootstrap.bootstrap(caller, loader, args)

    @JvmStatic
    fun bootstrapCallback(caller: String, args: Array<String>) {
        Class.forName("net.weavemc.loader.bootstrap.BootstrapContainer", false, ClassLoader.getSystemClassLoader())
            .getMethod("bootstrap", String::class.java, ClassLoader::class.java, Array<String>::class.java)
            .invoke(null, caller, javaClass.classLoader, args)
    }
}

class Bootstrap(val inst: Instrumentation) {
    fun bootstrap(caller: String, loader: ClassLoader, args: Array<String>) {
        // set game info arguments
        System.getProperties()["weave.main.args"] = args.toList().chunked(2)
            .associate { (a, b) -> a.removePrefix("--") to b }

        println("[Weave] Bootstrapping...\n" +
                "    -Caller: $caller\n" +
                "    -Version: $gameVersion\n" +
                "    -Client: $gameClient")

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
                MappingsHandler.environmentMappings.mappings,
                this,
                temp,
                "official",
                "named",
                listOf(FileManager.getVanillaMinecraftJar())
            )
            println(temp.path)
//            temp.deleteOnExit()
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

        println("[Weave] Bootstrapping complete.")
        /*
        Load the rest of the loader using Minecraft's class loader.
        This allows us to access Minecraft's classes throughout the project.
        */
        loader.loadClass("net.weavemc.loader.WeaveLoader")
            .getDeclaredMethod("init", Instrumentation::class.java, File::class.java, List::class.java)
            .invoke(null, inst, mappedVersionApi, mappedMods)
    }

    private fun removeTransformers() {
        println("[Weave] Removing Bootstrapping Transformers")
        inst.removeTransformer(AntiCacheTransformer)
        inst.removeTransformer(URLClassLoaderTransformer)
        println("[Weave] Removed Bootstrapping Transformers")
    }
}