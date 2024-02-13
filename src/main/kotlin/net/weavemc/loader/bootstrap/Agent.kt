package net.weavemc.loader.bootstrap

import net.weavemc.loader.WeaveLoader
import net.weavemc.loader.api.util.asm
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain

/**
 * The JavaAgent's `premain()` method, this is where initialization of Weave Loader begins.
 * Weave Loader's initialization begins by calling [WeaveLoader.init()][WeaveLoader.init], which is loaded through Genesis.
 */
@Suppress("UNUSED_PARAMETER")
public fun premain(opt: String?, inst: Instrumentation) {
    val version = findVersion()
    if(version !in arrayOf("1.8", "1.8.9")) {
        println("[Weave] $version not supported, disabling...")
        return
    }

    inst.addTransformer(object : ClassFileTransformer /*SafeTransformer*/ {
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
    inst.addTransformer(URLClassLoaderTransformer)

    inst.addTransformer(object : SafeTransformer {
        override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray? {
            // net/minecraft/ false flags on launchwrapper which gets loaded earlier
            if (className.startsWith("net/minecraft/client/")) {
                inst.removeTransformer(URLClassLoaderTransformer)
                inst.removeTransformer(this)

                (loader as URLClassLoaderAccessor).addWeaveURL(javaClass.protectionDomain.codeSource.location)
                loader.loadClass("net.weavemc.loader.WeaveLoader")
                    .getDeclaredMethod("init", Instrumentation::class.java)
                    .invoke(null, inst)
            }

            return null
        }
    })
}

private fun findVersion() =
    """--version\s+(\S+)""".toRegex()
        .find(System.getProperty("sun.java.command"))
        ?.groupValues?.get(1)
