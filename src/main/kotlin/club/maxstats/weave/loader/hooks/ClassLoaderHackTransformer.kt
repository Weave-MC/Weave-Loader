package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.util.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode
import java.net.URLClassLoader

class ClassLoaderHackTransformer : SafeTransformer() {
    override fun transform(
        loader: ClassLoader,
        className: String,
        originalClass: ByteArray
    ): ByteArray? {
        val reader = ClassReader(originalClass)
        if (reader.superName != internalNameOf<URLClassLoader>()) return null

        val node = ClassNode()
        reader.accept(node, 0)

        val loadClass = node.methods.find {
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
                internalNameOf<URLClassLoader>(),
                "loadClass",
                "(Ljava/lang/String;Z)Ljava/lang/Class;"
            )

            areturn
            +end
        })

        val writer = ClassWriter(reader, ClassWriter.COMPUTE_FRAMES)
        node.accept(writer)
        return writer.toByteArray()
    }

}