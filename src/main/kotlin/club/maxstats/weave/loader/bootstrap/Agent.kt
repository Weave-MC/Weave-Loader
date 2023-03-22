package club.maxstats.weave.loader.bootstrap

import java.lang.instrument.Instrumentation

@Suppress("UNUSED_PARAMETER")
public fun premain(opt: String?, inst: Instrumentation) {
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
}
