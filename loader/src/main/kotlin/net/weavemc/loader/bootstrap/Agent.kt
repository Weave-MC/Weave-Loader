package net.weavemc.loader.bootstrap

import net.weavemc.internals.asm
import net.weavemc.loader.*
import net.weavemc.loader.util.asClassNode
import net.weavemc.loader.util.asClassReader
import net.weavemc.loader.bootstrap.transformer.AntiCacheTransformer
import net.weavemc.loader.bootstrap.transformer.SafeTransformer
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderTransformer
import net.weavemc.loader.util.getOrCreateDirectory
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Type
import java.lang.instrument.Instrumentation
import kotlin.io.path.absolutePathString

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
            val usingMinecraftMain = className == "net/minecraft/client/main/Main"
            if (!usingMinecraftMain && !usingLaunchwrapper)
                return null

            val reader = originalClass.asClassReader()
            val node = reader.asClassNode()

            val methodNode = if (usingLaunchwrapper)
                node.methods.find { it.name == "launch" }
            else
                node.methods.find { it.name == "main" }

            if (methodNode == null)
                return null

            with(methodNode) {
                instructions.insert(asm {
                    ldc(className)

                    if (usingLaunchwrapper) {
                        getstatic("net/minecraft/launchwrapper/Launch", "classLoader", "Lnet/minecraft/launchwrapper/LaunchClassLoader;")
                    } else {
                        ldc(Type.getObjectType(className))
                        invokevirtual("java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;")
                    }

                    aload(if (usingLaunchwrapper) 1 else 0)
                    invokestatic(
                        "net/weavemc/loader/bootstrap/BootstrapContainer",
                        "finishBootstrap",
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