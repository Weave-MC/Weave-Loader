package club.maxstats.weave.loader.bootstrap

import java.lang.instrument.Instrumentation
import kotlin.io.path.*

@Suppress("UNUSED_PARAMETER")
public fun premain(opt: String?, inst: Instrumentation) {
    if (findVersion() != "1.8.9") {
        println("[Weave] ${findVersion()} not supported, disabling...")
        return
    }

    inst.addTransformer(object : SafeTransformer {
        override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray? {
            if (className.startsWith("net/minecraft/")) {
                inst.removeTransformer(this)

                val mcLoader = if (loader.toString().contains("ichor", ignoreCase = true))
                    loader.parent
                else
                    loader

                /*
                Load the rest of the loader using Genesis class loader.
                This allows us to access Minecraft's classes throughout the project.
                */
                mcLoader.loadClass("club.maxstats.weave.loader.WeaveLoader")
                    .getDeclaredMethod("preInit", Instrumentation::class.java)
                    .invoke(null, inst)
            }

            return null
        }
    })

    //inst.addTransformer(GenesisTransformer)
}

private fun findVersion() =
    """--version ([^ ]+)""".toRegex()
        .find(System.getProperty("sun.java.command"))
        ?.groupValues?.get(1)
