package net.weavemc.loader.bootstrap

import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.dump
import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.api.bytecode.named
import net.weavemc.api.sunArgs
import net.weavemc.loader.*
import net.weavemc.loader.asClassNode
import net.weavemc.loader.asClassReader
import net.weavemc.loader.bootstrap.transformer.AntiCacheTransformer
import net.weavemc.loader.bootstrap.transformer.SafeTransformer
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderTransformer
import net.weavemc.loader.getOrCreateDirectory
import org.objectweb.asm.ClassWriter
import java.lang.instrument.Instrumentation
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeText

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
            val usingLaunchwrapper = className == "net/minecraft/launchwrapper/Launch"
            if (className != "net/minecraft/client/main/Main" && !usingLaunchwrapper)
                return null

            val reader = originalClass.asClassReader()
            val node = reader.asClassNode()

            val (methodName, argsIdx) = if (usingLaunchwrapper) "launch" to 1 else "main" to 0

            with(node.methods.named(methodName)) {
                instructions.insert(asm {
                    ldc(className)


                    if (usingLaunchwrapper) {
                        getstatic("net/minecraft/launchwrapper/Launch", "classLoader", "Lnet/minecraft/launchwrapper/LaunchClassLoader;")
                    } else {
                        aload(0)
                        invokevirtual("java/lang/Object", "getClass", "()Ljava/lang/Class;")
                        invokevirtual("java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;")
                    }

                    aload(argsIdx)
                    invokestatic(
                        "net/weavemc/loader/bootstrap/BootstrapContainer",
                        "bootstrapCallback",
                        "(Ljava/lang/String;Ljava/lang/ClassLoader;[Ljava/lang/String;)V"
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