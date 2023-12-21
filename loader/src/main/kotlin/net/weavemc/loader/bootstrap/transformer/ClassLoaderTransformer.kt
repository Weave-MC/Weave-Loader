package net.weavemc.loader.bootstrap.transformer

import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.api.bytecode.visitAsm
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.net.URL
import java.net.URLClassLoader

interface URLClassLoaderAccessor {
    fun addWeaveURL(url: URL)
}
interface LaunchClassLoaderAccessor {
    fun excludeFromClassLoader(pkg: String)
    fun excludeFromTransformer(pkg: String)
}

object URLClassLoaderTransformer : SafeTransformer {
    override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray? {
        val reader = ClassReader(originalClass)
        if (reader.superName != internalNameOf<URLClassLoader>()) return null

        val node = ClassNode()
        reader.accept(node, 0)

        node.interfaces.add(internalNameOf<URLClassLoaderAccessor>())
        // Standard Minecraft ClassLoader
        if (reader.className.contains("LaunchClassLoader")) {
            node.interfaces.add(internalNameOf<LaunchClassLoaderAccessor>())

            node.visitMethod(Opcodes.ACC_PUBLIC, "excludeFromClassLoader", "(Ljava/lang/String;)V", null, null).visitAsm {
                aload(0)
                aload(1)
                invokevirtual(node.name, "addClassLoaderExclusion", "(Ljava/lang/String;)V")
                _return
            }

            node.visitMethod(Opcodes.ACC_PUBLIC, "excludeFromTransformer", "(Ljava/lang/String;)V", null, null).visitAsm {
                aload(0)
                aload(1)
                invokevirtual(node.name, "addTransformerExclusion", "(Ljava/lang/String;)V")
                _return
            }
        }

        node.visitMethod(Opcodes.ACC_PUBLIC, "addWeaveURL", "(Ljava/net/URL;)V", null, null).visitAsm {
            aload(0)
            aload(1)
            invokevirtual(node.name, "addURL", "(Ljava/net/URL;)V")
            _return
        }

        return ClassWriter(reader, ClassWriter.COMPUTE_MAXS).also { node.accept(it) }.toByteArray()
    }
}
