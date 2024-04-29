package net.weavemc.loader.bootstrap

import net.weavemc.api.Tweaker
import net.weavemc.internals.GameInfo
import net.weavemc.internals.GameInfo.commandLineArgs
import net.weavemc.internals.ModConfig
import net.weavemc.loader.bootstrap.transformer.SafeTransformer
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderAccessor
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderTransformer
import net.weavemc.loader.util.*
import java.lang.instrument.Instrumentation
import java.util.jar.JarFile
import kotlin.io.path.Path
import kotlin.io.path.pathString

object Bootstrap {
    fun bootstrap(inst: Instrumentation) {
        inst.addTransformer(object: SafeTransformer {
            override fun transform(loader: ClassLoader?, className: String, originalClass: ByteArray): ByteArray? {
                if (className.startsWith("net/minecraft/client")) {
                    printBootstrap(loader)

                    // remove bootstrap transformers
                    inst.removeTransformer(this)
                    inst.removeTransformer(URLClassLoaderTransformer)
//                    inst.removeTransformer(ApplicationWrapper)

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
                        Instrumentation::class.java
                    ).newInstance(clAccessor, inst)
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