package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.util.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode
import java.lang.instrument.ClassFileTransformer
import java.net.URLClassLoader
import java.security.ProtectionDomain

object ClassLoaderHackTransformer : ClassFileTransformer {
    override fun transform(
        loader: ClassLoader,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain,
        originalClass: ByteArray
    ): ByteArray? {
        val reader = ClassReader(originalClass)
        if (reader.superName != internalNameOf<URLClassLoader>()) return null

        val node = ClassNode()
        reader.accept(node, 0)

        val loadClass = node.methods.similar(URLClassLoader::loadClass) ?: return null
        loadClass.instructions.insert(asm {
            val end = LabelNode()

            aload(1)
            ldc("club.maxstats.weave.")
            invokevirtual("java/lang/String", "startsWith", "(Ljava/lang/String;)Z")
            ifeq(end)

            aload(0)
            aload(1)
            iconst_0
            invokeMethod(URLClassLoader::loadClass)

            areturn
            +end
        })

//        node.methods.add(node.generateMethod(name = "weave_addURL", desc = "(Ljava/net/URL;)V") {
//            aload(0)
//            aload(1)
//            invokevirtual(node.name, "addURL", "(Ljava/net/URL;)V")
//            _return
//        })

        val writer = ClassWriter(reader, ClassWriter.COMPUTE_FRAMES)
        node.accept(writer)
        return writer.toByteArray()
    }
}