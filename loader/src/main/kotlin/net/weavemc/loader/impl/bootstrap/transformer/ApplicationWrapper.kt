package net.weavemc.loader.impl.bootstrap.transformer

import me.xtrm.klog.dsl.klog
import net.weavemc.internals.asm
import net.weavemc.loader.impl.bootstrap.PublicButInternal
import net.weavemc.loader.impl.mixin.LoaderClassWriter
import net.weavemc.loader.impl.util.asClassNode
import net.weavemc.loader.impl.util.asClassReader
import net.weavemc.loader.impl.util.fatalError
import net.weavemc.loader.impl.util.illegalToReload
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Type
import org.objectweb.asm.tree.LabelNode
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.invoke.WrongMethodTypeException
import java.net.URLClassLoader

// Makes sure to run the application within some notion of a "custom" ClassLoader,
// such that signing integrity errors will not occur
@PublicButInternal
public object ApplicationWrapper {
    internal fun insertWrapper(className: String, originalClass: ByteArray): ByteArray {
        val reader = originalClass.asClassReader()
        val node = reader.asClassNode()

        val methodNode = node.methods.find { it.name == "main" }
            ?: fatalError("Failed to find the main method in $className whilst inserting wrapper")

        methodNode.instructions.insert(asm {
            ldc(Type.getObjectType(node.name))
            invokevirtual("java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;")
            invokestatic("java/lang/ClassLoader", "getSystemClassLoader", "()Ljava/lang/ClassLoader;")

            val label = LabelNode()
            if_acmpne(label)

            ldc(node.name.replace('/', '.'))
            aload(0)
            invokestatic(
                "net/weavemc/loader/impl/bootstrap/transformer/ApplicationWrapper",
                "wrap",
                "(Ljava/lang/String;[Ljava/lang/String;)V"
            )

            // just in case
            _return

            +label

            ldc(className)

            ldc(Type.getObjectType(className))
            invokevirtual("java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;")

            aload(0)
            invokestatic(
                "net/weavemc/loader/impl/bootstrap/BootstrapContainer",
                "finishBootstrap",
                "(Ljava/lang/String;Ljava/lang/ClassLoader;[Ljava/lang/String;)V"
            )
        })

        return LoaderClassWriter(ClassLoader.getSystemClassLoader(), reader, ClassWriter.COMPUTE_FRAMES)
            .also { node.accept(it) }.toByteArray()
    }

    @JvmStatic
    @Suppress("unused")
    public fun wrap(targetMain: String, args: Array<String>) {
        val logger by klog
        logger.info("Minecraft Main was directly invoked, which potentially blocks transformation")
        logger.info(
            "This is normal to happen on Vanilla Minecraft pre-launchwrapper. " +
                    "Therefore, the game will be wrapped into a new ClassLoader"
        )

        val mainClass = WrappingLoader().loadClass(targetMain)

        try {
            val type = MethodType.methodType(Void::class.javaPrimitiveType, args::class.java)
            MethodHandles.lookup().findStatic(mainClass, "main", type).invokeExact(args) as Unit
        } catch (e: Throwable) {
            when (e) {
                is WrongMethodTypeException,
                is NoSuchMethodException,
                is IllegalAccessException,
                is ClassNotFoundException -> {
                    // Some error occurred within reflective access
                    e.printStackTrace()

                    logger.warn("Failed to wrap game using java.lang.invoke, using Reflection fallback")
                    mainClass.getMethod("main", args::class.java)(null, args)
                }

                else -> throw e
            }
        }
    }

    public class WrappingLoader : URLClassLoader(emptyArray(), getSystemClassLoader()) {
        override fun loadClass(name: String, resolve: Boolean): Class<*> =
            findClass(name).also { if (resolve) resolveClass(it) }

        override fun findClass(name: String): Class<*> {
            if (name == javaClass.name) return javaClass
            findLoadedClass(name)?.let { return it }

            if (
                illegalToReload.any { name.startsWith(it) } ||
                name == "net.weavemc.loader.impl.bootstrap.BootstrapContainer"
            ) return parent.loadClass(name)

            val internalName = name.replace('.', '/')
            val bytes = getResourceAsStream("$internalName.class")?.readBytes() ?: throw ClassNotFoundException()

            // bye-bye protectiondomain!
            // also we need a urlclassloader
            return defineClass(name, bytes, 0, bytes.size)
        }
    }
}