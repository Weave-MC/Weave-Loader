package net.weavemc.loader.bootstrap

import net.weavemc.internals.GameInfo.gameClient
import net.weavemc.internals.GameInfo.gameVersion
import net.weavemc.loader.bootstrap.transformer.*
import java.lang.instrument.Instrumentation
import java.net.URL
import java.util.jar.JarFile

object BootstrapContainer {
    lateinit var bootstrap: Bootstrap

    @JvmStatic
    fun finishBootstrap(caller: String, loader: ClassLoader, args: Array<String>) {
        bootstrap.bootstrap(caller, loader, args)
    }
}

class Bootstrap(val inst: Instrumentation) {
    fun bootstrap(caller: String, loader: ClassLoader, args: Array<String>) {
        // set game info arguments
        System.getProperties()["weave.main.args"] = args.toList().chunked(2)
            .associate { (a, b) -> a.removePrefix("--") to b }

        println("[Weave] Bootstrapping...\n" +
                "    -Caller: $caller\n" +
                "    -Version: ${gameVersion.versionName}\n" +
                "    -Client: ${gameClient.clientName}")

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

        removeTransformers()

        println("[Weave] Bootstrapping complete.")
        /*
        Load the rest of the loader using Minecraft's class loader.
        This allows us to access Minecraft's classes throughout the project.
        */
        loader.loadClass("net.weavemc.loader.WeaveLoader")
            .getDeclaredMethod("init", Instrumentation::class.java, URLClassLoaderAccessor::class.java)
            .invoke(null, inst, urlClassLoaderAccessor)
    }

    private fun removeTransformers() {
        println("[Weave] Removing Bootstrapping Transformers")
        inst.removeTransformer(AntiCacheTransformer)
        inst.removeTransformer(URLClassLoaderTransformer)
        println("[Weave] Removed Bootstrapping Transformers")
    }
}