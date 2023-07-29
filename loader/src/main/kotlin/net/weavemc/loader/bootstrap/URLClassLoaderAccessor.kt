package net.weavemc.loader.bootstrap

import net.weavemc.weave.api.bytecode.asm
import net.weavemc.weave.api.bytecode.internalNameOf
import org.objectweb.asm.ClassReader
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

inline fun transformURLClassLoader(classReader: ClassReader): ByteArray {
    val cn = ClassNode()
    classReader.accept(cn, 0)

    cn.interfaces.add(internalNameOf<URLClassLoaderAccessor>())

    val mn = MethodNode(
        Opcodes.ACC_PUBLIC,
        "addWeaveURL",
        "(L${internalNameOf<URL>()};)V",
        null,
        null
    )

    mn.instructions = asm {
        aload(0)
        aload(1)
        invokevirtual(
            internalNameOf<URLClassLoader>(),
            "addURL",
            "(L${internalNameOf<URL>()};)V"
        )
        _return
    }

    cn.methods.add(mn)

    val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
    cn.accept(cw)
    return cw.toByteArray()
}
