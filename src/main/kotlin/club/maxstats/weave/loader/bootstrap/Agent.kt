package club.maxstats.weave.loader.bootstrap

import java.lang.instrument.Instrumentation

@Suppress("UNUSED_PARAMETER")
public fun premain(opt: String?, inst: Instrumentation) {
    if(findVersion() != "1.8.9") {
        println("[Weave] ${findVersion()} not supported, disabling...")
        return
    }

    inst.addTransformer(object : SafeTransformer {
        override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray? {
            if (className.startsWith("net/minecraft/")) {
                inst.removeTransformer(this)

                /*
                 * Load the rest of the loader using Lunar's own class loader.
                 * This allows us access to Minecraft's classes throughout the project.
                 */

                loader.loadClass("club.maxstats.weave.loader.WeaveLoader")
                    .getDeclaredMethod("preInit", Instrumentation::class.java, ClassLoader::class.java)
                    .invoke(null, inst, loader)
            }

            return null
        }
    })

    inst.addTransformer(MixinTransformer)
}

private fun findVersion() =
    """--version ([^ ]+)""".toRegex()
        .find(System.getProperty("sun.java.command"))
        ?.groupValues?.get(1)

