package net.weavemc.loader.bootstrap

import net.weavemc.loader.api.util.asm
import net.weavemc.loader.api.util.visitAsm
import net.weavemc.loader.util.internalNameOf
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode
import java.net.URL
import java.net.URLClassLoader

public interface URLClassLoaderAccessor {
    public fun addWeaveURL(url: URL)
}

public object URLClassLoaderTransformer : SafeTransformer {
    override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray? {
        val reader = ClassReader(originalClass)
        if (reader.superName != internalNameOf<URLClassLoader>()) return null

        val node = ClassNode()
        reader.accept(node, 0)

        node.interfaces.add(internalNameOf<URLClassLoaderAccessor>())

        node.visitMethod(Opcodes.ACC_PUBLIC, "addWeaveURL", "(Ljava/net/URL;)V", null, null).visitAsm {
            aload(0)
            aload(1)
            invokevirtual(node.name, "addURL", "(Ljava/net/URL;)V")
            _return
        }

        val loadClassInject = asm {
            aload(1)
            ldc("net.weavemc.loader.bootstrap")
            invokevirtual("java/lang/String", "startsWith", "(Ljava/lang/String;)Z")

            val failed = LabelNode()
            ifeq(failed)

            invokestatic("java/lang/ClassLoader", "getSystemClassLoader", "()Ljava/lang/ClassLoader;")
            aload(1)
            invokevirtual("java/lang/ClassLoader", "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;")
            areturn

            +failed
        }

        node.methods.find { it.name == "loadClass" }?.instructions?.insert(loadClassInject)
            ?: node.visitMethod(Opcodes.ACC_PUBLIC, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;", null, null).visitAsm {
                +loadClassInject
                aload(0)
                aload(1)
                invokespecial("java/lang/ClassLoader", "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;")
                areturn
            }

        return ClassWriter(reader, ClassWriter.COMPUTE_FRAMES).also { node.accept(it) }.toByteArray()
    }
}
