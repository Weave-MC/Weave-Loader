package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.WeaveLoader
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain

class PreinitTransformer(private val inst: Instrumentation) : ClassFileTransformer {
    override fun transform(
        loader: ClassLoader,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray
    ): ByteArray {
        if (className.startsWith("net/minecraft/")) {
            WeaveLoader.preinit(loader)
            inst.removeTransformer(this)
        }

        return classfileBuffer
    }
}