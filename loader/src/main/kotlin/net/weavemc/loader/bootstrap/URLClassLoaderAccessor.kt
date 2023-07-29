package net.weavemc.loader.bootstrap

import net.weavemc.weave.api.bytecode.internalNameOf
import net.weavemc.weave.api.bytecode.visitAsm
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.net.URL
import java.net.URLClassLoader

interface URLClassLoaderAccessor {
    fun addWeaveURL(url: URL)
}

object URLClassLoaderTransformer : SafeTransformer {
    override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray? {
        val cr = ClassReader(originalClass)
        if(cr.superName != internalNameOf<URLClassLoader>()) return null

        val cw = ClassWriter(cr, ClassWriter.COMPUTE_MAXS)

        cr.accept(object : ClassVisitor(Opcodes.ASM9, cw) {
            override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<String>) {
                super.visit(version, access, name, signature, superName, interfaces + internalNameOf<URLClassLoaderAccessor>())
            }

            override fun visitEnd() {
                val mv = super.visitMethod(Opcodes.ACC_PUBLIC, "addWeaveURL", "(Ljava/net/URL;)V", null, null)

                mv.visitAsm {
                    aload(0)
                    aload(1)
                    invokevirtual(cr.className, "addURL", "(Ljava/net/URL;)V")
                    _return
                }

                mv.visitMaxs(2, 2)

                super.visitEnd()
            }
        }, 0)

        return cw.toByteArray()
    }
}
