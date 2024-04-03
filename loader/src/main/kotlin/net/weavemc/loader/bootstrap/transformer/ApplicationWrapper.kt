package net.weavemc.loader.bootstrap.transformer

import net.weavemc.internals.asm
import net.weavemc.loader.mixin.LoaderClassWriter
import net.weavemc.loader.util.asClassNode
import net.weavemc.loader.util.asClassReader
import net.weavemc.loader.util.illegalToReload
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Type
import org.objectweb.asm.tree.LabelNode
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.invoke.WrongMethodTypeException
import java.net.URLClassLoader

// Makes sure to run the application within some notion of a "custom" ClassLoader,
// such that signing integrity errors will not occur
object ApplicationWrapper : SafeTransformer {
    override fun transform(loader: ClassLoader?, className: String, originalClass: ByteArray): ByteArray? {
        val usingLaunchwrapper = className == "net/minecraft/launchwrapper/Launch"
        val usingMinecraftMain = className == "net/minecraft/client/main/Main"
        if (!usingMinecraftMain && !usingLaunchwrapper) return null

        val reader = originalClass.asClassReader()
        val node = reader.asClassNode()

        val targetMethod = if (usingLaunchwrapper) "launch" else "main"
        val methodNode = node.methods.find { it.name == targetMethod } ?: return null

        methodNode.instructions.insert(asm {
            if (usingMinecraftMain) {
                ldc(Type.getObjectType(node.name))
                invokevirtual("java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;")
                invokestatic("java/lang/ClassLoader", "getSystemClassLoader", "()Ljava/lang/ClassLoader;")

                val label = LabelNode()
                if_acmpne(label)

                ldc(node.name.replace('/', '.'))
                aload(0)
                invokestatic(
                    "net/weavemc/loader/bootstrap/transformer/ApplicationWrapper",
                    "wrap",
                    "(Ljava/lang/String;[Ljava/lang/String;)V"
                )

                // just in case
                _return

                +label
            }

            ldc(className)

            if (usingLaunchwrapper) {
                getstatic(
                    "net/minecraft/launchwrapper/Launch",
                    "classLoader",
                    "Lnet/minecraft/launchwrapper/LaunchClassLoader;"
                )
            } else {
                ldc(Type.getObjectType(className))
                invokevirtual("java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;")
            }

            aload(if (usingLaunchwrapper) 1 else 0)
            invokestatic(
                "net/weavemc/loader/bootstrap/BootstrapContainer",
                "finishBootstrap",
                "(Ljava/lang/String;Ljava/lang/ClassLoader;[Ljava/lang/String;)V"
            )
        })

        return LoaderClassWriter(ClassLoader.getSystemClassLoader(), reader, ClassWriter.COMPUTE_FRAMES)
            .also { node.accept(it) }.toByteArray()
    }

    @JvmStatic
    @Suppress("unused")
    fun wrap(targetMain: String, args: Array<String>) {
        println("[Weave] Minecraft Main was directly invoked, which potentially blocks transformation")
        println(
            "[Weave] This is normal to happen on Vanilla Minecraft pre-launchwrapper. " +
                    "Therefore, the game will be wrapped into a new ClassLoader"
        )

        val mainClass = WrappingLoader().loadClass(targetMain)

        try {
            val type = MethodType.methodType(Void::class.javaPrimitiveType, args::class.java)
            MethodHandles.lookup().findStatic(mainClass, "main", type).invokeExact(args) as Unit
        } catch (e: Throwable) {
            when (e) {
                is WrongMethodTypeException, is NoSuchMethodException, is IllegalAccessException, is ClassNotFoundException -> {
                    // Some error occurred within reflective access
                    e.printStackTrace()

                    println("[Weave] Failed to wrap game using java.lang.invoke, using Reflection fallback")
                    mainClass.getMethod("main", args::class.java)(null, args)
                }
                else -> throw e
            }
        }
    }

    class WrappingLoader : URLClassLoader(emptyArray(), getSystemClassLoader()) {
        override fun loadClass(name: String, resolve: Boolean) =
            findClass(name).also { if (resolve) resolveClass(it) }

        override fun findClass(name: String): Class<*> {
            if (name == javaClass.name) return javaClass
            findLoadedClass(name)?.let { return it }

            if (
                illegalToReload.any { name.startsWith(it) } ||
                name == "net.weavemc.loader.bootstrap.BootstrapContainer"
            ) return parent.loadClass(name)

            val internalName = name.replace('.', '/')
            val bytes = getResourceAsStream("$internalName.class")?.readBytes() ?: throw ClassNotFoundException()

            // bye-bye protectiondomain!
            return defineClass(name, bytes, 0, bytes.size)
        }
    }
}