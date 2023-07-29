package net.weavemc.loader.bootstrap

import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.internalNameOf
import net.weavemc.weave.api.bytecode.visitAsm
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodNode
import java.net.URL
import java.net.URLClassLoader

interface URLClassLoaderAccessor {
    fun addWeaveURL(url: URL)
}

object URLClassLoaderTransformer : SafeTransformer {
    override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray? {
        val cr = ClassReader(originalClass)
        if(cr.superName != internalNameOf<URLClassLoader>()) return null

        val cn = ClassNode()
        cr.accept(cn, 0)

        cn.interfaces.add(internalNameOf<URLClassLoaderAccessor>())
        cn.visitMethod(Opcodes.ACC_PUBLIC, "addWeaveURL", "(Ljava/net/URL;)V", null, null).visitAsm {
            aload(0)
            aload(1)
            invokevirtual(cn.name, "addURL", "(Ljava/net/URL;)V")
            _return
        }

        val loadClass = cn.methods.find { it.name == "loadClass" && it.desc == "(Ljava/lang/String;Z)Ljava/lang/Class;" }
        loadClass?.instructions?.insert(asm {
            val end = LabelNode()
            aload(1)
            ldc("org.objectweb.asm.")
            invokevirtual("java/lang/String", "startsWith", "(Ljava/lang/String;)Z")
            ifeq(end)

            aload(0)
            aload(1)
            iload(2)
            invokespecial(cn.superName, "loadClass", "(Ljava/lang/String;Z)Ljava/lang/Class;")

            +end
            f_same()
        })

        val cw = ClassWriter(cr, ClassWriter.COMPUTE_MAXS)
        cn.accept(cw)
        return cw.toByteArray()
    }
}
