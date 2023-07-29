package net.weavemc.loader.bootstrap

import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.internalNameOf
import net.weavemc.weave.api.bytecode.visitAsm
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.io.FileOutputStream
import java.net.URL
import java.net.URLClassLoader

interface URLClassLoaderAccessor {
    fun addWeaveURL(url: URL)
}

object URLClassLoaderTransformer : SafeTransformer {
    override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray? {
        val cr = ClassReader(originalClass)
        if(cr.superName != internalNameOf<URLClassLoader>()) return null

        val cw = ClassWriter(cr, 0)

        cr.accept(object : ClassVisitor(Opcodes.ASM9, cw) {
            override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<String>) {
                super.visit(version, access, name, signature, superName, interfaces + internalNameOf<URLClassLoaderAccessor>())
            }

            override fun visitEnd() {
                super.visitMethod(Opcodes.ACC_PUBLIC, "addWeaveURL", "(Ljava/net/URL;)V", null, null).visitAsm {
                    aload(0)
                    aload(1)
                    invokevirtual(cr.className, "addURL", "(Ljava/net/URL;)V")
                    _return
                }

                super.visitEnd()
            }
        }, 0)

        return cw.toByteArray()
    }
}
