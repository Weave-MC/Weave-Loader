package net.weavemc.loader.bootstrap

import net.weavemc.internals.GameInfo
import net.weavemc.loader.bootstrap.transformer.ApplicationWrapper
import net.weavemc.loader.bootstrap.transformer.SafeTransformer
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderAccessor
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderTransformer
import net.weavemc.loader.util.*
import java.io.File
import java.lang.instrument.Instrumentation

object Bootstrap {
    fun bootstrap(inst: Instrumentation, mods: List<File>) {
        inst.addTransformer(object: SafeTransformer {
            override fun transform(loader: ClassLoader?, className: String, originalClass: ByteArray): ByteArray? {
                if (className == "net/minecraft/client/main/Main") {
                    if (loader == ClassLoader.getSystemClassLoader())
                        return ApplicationWrapper.insertWrapper(className, originalClass)

                    printBootstrap(loader)

                    // remove bootstrap transformers
                    inst.removeTransformer(this)
                    inst.removeTransformer(URLClassLoaderTransformer)

                    val clAccessor = if (loader is URLClassLoaderAccessor) loader
                    else fatalError("Failed to transform URLClassLoader to implement URLClassLoaderAccessor. Impossible to recover")

                    runCatching {
                        clAccessor.addWeaveURL(javaClass.protectionDomain.codeSource.location)
                    }.onFailure {
                        it.printStackTrace()
                        fatalError("Failed to deliberately add Weave to the target classloader")
                    }

                    println("[Weave] Bootstrapping complete.")

                    /**
                     * Start the Weave Loader initialization phase
                     */
                    val wlc = loader.loadClass("net.weavemc.loader.WeaveLoader")
                    wlc.getConstructor(
                        URLClassLoaderAccessor::class.java,
                        Instrumentation::class.java,
                        java.util.List::class.java
                    ).newInstance(clAccessor, inst, mods)
                }

                return null
            }
        })
    }

    private fun printBootstrap(loader: ClassLoader?) {
        println(
            """
            [Weave] Bootstrapping...
                - Version: ${GameInfo.version.versionName}
                - Client: ${GameInfo.client.clientName}
                - Loader: $loader
            """.trimIndent()
        )
    }
}