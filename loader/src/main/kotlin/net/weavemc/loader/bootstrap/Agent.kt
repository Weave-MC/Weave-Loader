package net.weavemc.loader.bootstrap

import net.weavemc.internals.asm
import net.weavemc.loader.*
import net.weavemc.loader.bootstrap.transformer.ApplicationWrapper
import net.weavemc.loader.bootstrap.transformer.MixinRelocator
import net.weavemc.loader.bootstrap.transformer.SafeTransformer
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderTransformer
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

    System.setProperty("ichor.prebakeClasses", "false")

    // Hack: sometimes the state is improperly initialized, which causes Swing to feel like it is headless?
    // Calling this solves the problem
    GraphicsEnvironment.isHeadless()
    inst.addTransformer(object : ClassFileTransformer {
        override fun transform(
            loader: ClassLoader?,
            className: String,
            classBeingRedefined: Class<*>?,
            protectionDomain: ProtectionDomain?,
            classfileBuffer: ByteArray
        ): ByteArray? {
            if (className != "sun/management/RuntimeImpl" || classBeingRedefined == null) return null
            inst.removeTransformer(this)

            val node = ClassNode().also { ClassReader(classfileBuffer).accept(it, 0) }
            val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES)

            with(node.methods.first { it.name == "getInputArguments" }) {
                val insn = instructions.first { it.opcode == Opcodes.ARETURN }
                instructions.insertBefore(insn, asm {
                    invokeinterface("java/lang/Iterable", "iterator", "()Ljava/util/Iterator;")
                    astore(2)
                    new("java/util/ArrayList")
                    dup
                    invokespecial("java/util/ArrayList", "<init>", "()V")
                    astore(3)

                    val loop = LabelNode()
                    val end = LabelNode()

                    +loop
                    aload(2)
                    invokeinterface("java/util/Iterator", "hasNext", "()Z")
                    ifeq(end)

                    aload(2)
                    invokeinterface("java/util/Iterator", "next", "()Ljava/lang/Object;")
                    checkcast("java/lang/String")
                    dup
                    astore(4)
                    ldc("javaagent")
                    invokevirtual("java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z")
                    ifne(loop)

                    aload(3)
                    aload(4)
                    invokeinterface("java/util/List", "add", "(Ljava/lang/Object;)Z")
                    pop

                    goto(loop)
                    +end

                    aload(3)
                })
            }

            node.accept(writer)

            return writer.toByteArray()
        }
    }, true)
    inst.retransformClasses(Class.forName("sun.management.RuntimeImpl", false, ClassLoader.getSystemClassLoader()))

    // initialize bootstrap
    BootstrapContainer.offerInstrumentation(inst)
    println("[Weave] Bootstrapped Weave")
}