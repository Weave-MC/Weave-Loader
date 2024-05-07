package net.weavemc.loader.bootstrap

import me.xtrm.klog.dsl.klog
import net.weavemc.internals.GameInfo
import net.weavemc.loader.bootstrap.transformer.ApplicationWrapper
import net.weavemc.loader.bootstrap.transformer.SafeTransformer
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderAccessor
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderTransformer
import net.weavemc.loader.util.exit
import net.weavemc.loader.util.fatalError
import java.lang.instrument.Instrumentation

object Bootstrap {
    val logger by klog

    fun bootstrap(inst: Instrumentation) {
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

                    clAccessor.addWeaveIgnoredPackage("net.weavemc.loader.bootstrap")
                    clAccessor.addWeaveIgnoredPackage("kotlin.")
                    clAccessor.addWeaveIgnoredPackage("me.xtrm.klog.")

                    runCatching {
                        clAccessor.addWeaveURL(javaClass.protectionDomain.codeSource.location)
                    }.onFailure {
                        it.printStackTrace()
                        fatalError("Failed to deliberately add Weave to the target classloader")
                    }

                    logger.info("Bootstrapping complete, initializing loader...")

                    /**
                     * Start the Weave Loader initialization phase
                     */
                    val wlc = loader.loadClass("net.weavemc.loader.WeaveLoader")
                    runCatching {
                        wlc.getConstructor(
                            URLClassLoaderAccessor::class.java,
                            Instrumentation::class.java,
                        ).newInstance(clAccessor, inst)
                    }.onFailure {
                        logger.fatal("Failed to instantiate WeaveLoader", it)
                        exit(-1)
                    }
                }

                return null
            }
        })
    }

    private fun printBootstrap(loader: ClassLoader?) {
        logger.info("Bootstrapping Weave Loader...")
        logger.debug(" - Version: ${GameInfo.version.versionName}")
        logger.debug(" - Client: ${GameInfo.client.clientName}")
        logger.debug(" - Loader: $loader")
    }
}