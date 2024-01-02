package net.weavemc.loader.bootstrap.transformer

import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.visitAsm
import net.weavemc.loader.util.getOrCreateDirectory
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode
import java.net.URL
import java.net.URLClassLoader
import kotlin.io.path.absolutePathString

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

        var loadClass = node.methods.find { it.name == "loadClass" }

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

        if (loadClass == null) {
            node.visitMethod(Opcodes.ACC_PUBLIC, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;", null, null).visitAsm {
                +loadClassInject

                aload(0)
                aload(1)
                invokespecial("java/lang/ClassLoader", "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;")
                areturn
            }
        } else
            loadClass.instructions.insert(loadClassInject)

        return ClassWriter(reader, ClassWriter.COMPUTE_FRAMES).also { node.accept(it) }.toByteArray()
    }
}
