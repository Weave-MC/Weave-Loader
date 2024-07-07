package net.weavemc.loader.bootstrap

import me.xtrm.klog.dsl.klog
import net.weavemc.internals.GameInfo
import net.weavemc.loader.bootstrap.transformer.ApplicationWrapper
import net.weavemc.loader.bootstrap.transformer.SafeTransformer
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderAccessor
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderTransformer
import net.weavemc.loader.util.*
import java.io.File
import java.lang.instrument.Instrumentation

object Bootstrap {
    val logger by klog

    fun bootstrap(inst: Instrumentation, mods: List<File>) = inst.addTransformer(object: SafeTransformer {
        override fun transform(loader: ClassLoader?, className: String, originalClass: ByteArray): ByteArray? {
            if (className != "net/minecraft/client/main/Main") return null
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

            logger.info("Bootstrapping complete, initializing loader...")

            runCatching {
                loader.loadClass("net.weavemc.loader.WeaveLoader").getConstructor(
                    URLClassLoaderAccessor::class.java,
                    Instrumentation::class.java,
                    java.util.List::class.java
                ).newInstance(clAccessor, inst, mods)
            }.onFailure {
                logger.fatal("Failed to instantiate WeaveLoader", it)
                exit(-1)
            }

            return null
        }
    })

    private fun printBootstrap(loader: ClassLoader?) {
        logger.info("Bootstrapping Weave Loader...")
        logger.debug(" - Version: ${GameInfo.version.versionName}")
        logger.debug(" - Client: ${GameInfo.client.clientName}")
        logger.debug(" - Loader: $loader")
    }
}