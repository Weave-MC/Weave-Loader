package net.weavemc.loader.bootstrap

import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.api.bytecode.named
import net.weavemc.loader.WeaveLoader
import net.weavemc.loader.asClassNode
import net.weavemc.loader.asClassReader
import net.weavemc.loader.bootstrap.transformer.AntiCacheTransformer
import net.weavemc.loader.bootstrap.transformer.SafeTransformer
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderTransformer
import org.objectweb.asm.ClassWriter
import java.lang.instrument.Instrumentation

/**
 * The JavaAgent's `premain()` method, this is where initialization of Weave Loader begins.
 * Weave Loader's initialization begins by calling [WeaveLoader.init], which is loaded through Genesis.
 */
@Suppress("UNUSED_PARAMETER")
fun premain(opt: String?, inst: Instrumentation) {
    println("[Weave] Bootstrapping Weave")

    inst.addTransformer(URLClassLoaderTransformer)
    inst.addTransformer(AntiCacheTransformer)

    inst.addTransformer(object: SafeTransformer {
        override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray? {
            if (className != "net/minecraft/client/main/Main") return null

            val reader = originalClass.asClassReader()
            val node = reader.asClassNode()

            with(node.methods.named("main")) {
                instructions.insert(asm {
                    ldc(className)
                    aload(0)
                    invokestatic(
                        "net/weavemc/loader/bootstrap/BootstrapContainer",
                        "bootstrapCallback",
                        "(Ljava/lang/String;[Ljava/lang/String;)V"
                    )
                })
            }

            inst.removeTransformer(this)

            return ClassWriter(reader, ClassWriter.COMPUTE_MAXS).also { node.accept(it) }.toByteArray()
        }
    })

    // initialize bootstrap
    Bootstrap(inst).also { BootstrapContainer.bootstrap = it }

    println("[Weave] Bootstrapped Weave")
}