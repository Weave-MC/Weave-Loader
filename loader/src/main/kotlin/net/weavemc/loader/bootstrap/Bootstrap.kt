package net.weavemc.loader.bootstrap

import net.weavemc.internals.GameInfo.gameClient
import net.weavemc.internals.GameInfo.gameVersion
import net.weavemc.loader.bootstrap.transformer.ApplicationWrapper
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderAccessor
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderTransformer
import java.lang.instrument.Instrumentation
import kotlin.system.exitProcess

object BootstrapContainer {
    private lateinit var instrumentation: Instrumentation

    fun offerInstrumentation(inst: Instrumentation) {
        instrumentation = inst
    }

    @JvmStatic
    fun finishBootstrap(caller: String, loader: ClassLoader, args: Array<String>) {
        val bootstrapType = loader.loadClass("net.weavemc.loader.bootstrap.Bootstrap")
        val bootstrap = bootstrapType.getConstructor(Instrumentation::class.java).newInstance(instrumentation)
        val method = bootstrapType.getMethod("bootstrap", String::class.java, ClassLoader::class.java, args::class.java)
        method(bootstrap, caller, loader, args)
    }
}

class Bootstrap(val inst: Instrumentation) {
    fun bootstrap(caller: String, loader: ClassLoader, args: Array<String>) {
        // set game info arguments
        System.getProperties()["weave.main.args"] = args.toList().chunked(2)
            .associate { (a, b) -> a.removePrefix("--") to b }

        println(
            """
[Weave] Bootstrapping...
    - Caller: $caller
    - Version: ${gameVersion.versionName}
    - Client: ${gameClient.clientName}
    - Loader: $loader
            """.trim()
        )

        val urlClassLoaderAccessor = if (loader is URLClassLoaderAccessor) loader
        else {
            println("[Weave] Failed to transform URLClassLoader to implement URLClassLoaderAccessor. Impossible to recover")
            exitProcess(-1)
        }

        inst.removeTransformer(URLClassLoaderTransformer)
        inst.removeTransformer(ApplicationWrapper)
        runCatching {
            urlClassLoaderAccessor.addWeaveURL(javaClass.protectionDomain.codeSource.location)
        }.onFailure {
            println("Failed to deliberately add Weave to the target classloader:")
            it.printStackTrace()
        }

        println("[Weave] Bootstrapping complete.")

        /**
         * Start the Weave Loader initialization phase
         */
        val wlc = loader.loadClass("net.weavemc.loader.WeaveLoader")
        wlc.getConstructor(
            loader.loadClass("net.weavemc.loader.bootstrap.transformer.URLClassLoaderAccessor"),
            Instrumentation::class.java
        ).newInstance(urlClassLoaderAccessor, inst)
    }
}