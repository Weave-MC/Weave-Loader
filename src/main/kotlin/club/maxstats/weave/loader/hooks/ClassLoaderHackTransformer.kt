package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.util.asm
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.lang.instrument.ClassFileTransformer
import java.net.URLClassLoader
import java.security.ProtectionDomain

class ClassLoaderHackTransformer : ClassFileTransformer {
    override fun transform(
        loader: ClassLoader,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        originalClass: ByteArray
    ): ByteArray? {
        val cr = ClassReader(originalClass)
        if (cr.superName != Type.getInternalName(URLClassLoader::class.java)) return null

        val cn = ClassNode()
        cr.accept(cn, 0)

        val loadClass = cn.methods.find {
            it.name == "loadClass" && it.desc == "(Ljava/lang/String;Z)Ljava/lang/Class;"
        } ?: return null

        loadClass.instructions.insert(asm {
            val end = LabelNode()

            aload(1)
            ldc("club.maxstats.weave.")
            invokevirtual("java/lang/String", "startsWith", "(Ljava/lang/String;)Z")
            ifeq(end)

            aload(0)
            aload(1)
            iconst_0
            invokespecial(
                Type.getInternalName(URLClassLoader::class.java),
                "loadClass",
                "(Ljava/lang/String;Z)Ljava/lang/Class;"
            )
            areturn

            +end
        })

        val addUrl = MethodNode(
            Opcodes.ACC_PUBLIC,
            "weave_addURL",
            "(Ljava/net/URL;)V",
            null,
            null
        ).apply {
            instructions = asm {
                aload(0)
                aload(1)
                invokevirtual(cn.name, "addURL", "(Ljava/net/URL;)V")
                _return
            }
        }

        cn.methods.add(addUrl)

        val cw = ClassWriter(cr, ClassWriter.COMPUTE_FRAMES)
        cn.accept(cw)
        return cw.toByteArray()
    }
}