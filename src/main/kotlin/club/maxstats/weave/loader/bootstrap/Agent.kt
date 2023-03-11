package club.maxstats.weave.loader.bootstrap

import java.lang.instrument.Instrumentation

fun premain(opt: String?, inst: Instrumentation) {
    inst.addTransformer(object : SafeTransformer {
        override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray? {
            if (className.startsWith("net/minecraft/")) {
                inst.removeTransformer(this)

                //load the rest of the loader using lunars classloader
                //allows us to access minecraft classes throughout the project

                val weave = loader.loadClass("club.maxstats.weave.loader.WeaveLoader")
                weave.getDeclaredMethod("preInit", Instrumentation::class.java).invoke(weave, inst)
            }

            return null
        }
    })
}
