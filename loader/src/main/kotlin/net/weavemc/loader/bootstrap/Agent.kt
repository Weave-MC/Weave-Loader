package net.weavemc.loader.bootstrap

import net.weavemc.internals.asm
import net.weavemc.loader.*
import net.weavemc.loader.bootstrap.transformer.*
import net.weavemc.loader.util.asClassNode
import net.weavemc.loader.util.asClassReader
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode
import java.awt.GraphicsEnvironment
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain

/**
 * The JavaAgent's `premain()` method, this is where initialization of Weave Loader begins.
 * Weave Loader's initialization begins by calling [WeaveLoader.init]
 */
@Suppress("UNUSED_PARAMETER")
fun premain(opt: String?, inst: Instrumentation) {
    println("[Weave] Bootstrapping Weave")

    inst.addTransformer(URLClassLoaderTransformer)
    inst.addTransformer(MixinRelocator)
    inst.addTransformer(ApplicationWrapper)

    inst.addTransformer(ArgumentSanitizer, true)
    inst.retransformClasses(Class.forName("sun.management.RuntimeImpl", false, ClassLoader.getSystemClassLoader()))
    inst.removeTransformer(ArgumentSanitizer)

    // Prevent ichor prebake
    System.setProperty("ichor.prebakeClasses", "false")

    // Hack: sometimes the state is improperly initialized, which causes Swing to feel like it is headless?
    // Calling this solves the problem
    GraphicsEnvironment.isHeadless()

    // initialize bootstrap
    BootstrapContainer.offerInstrumentation(inst)
    println("[Weave] Bootstrapped Weave")
}