package net.weavemc.loader.bootstrap.transformer

import net.weavemc.internals.asm
import net.weavemc.internals.internalNameOf
import net.weavemc.internals.visitAsm
import net.weavemc.loader.mixin.LoaderClassWriter
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode
import java.net.URL
import java.net.URLClassLoader

interface URLClassLoaderAccessor {
    val weaveBacking: ClassLoader
    fun addWeaveIgnoredPackage(pkg: String)
    fun addWeaveURL(url: URL)
}

object URLClassLoaderTransformer : SafeTransformer {
    override fun transform(loader: ClassLoader?, className: String, originalClass: ByteArray): ByteArray? {
        if (loader == null)
            return null

        val reader = ClassReader(originalClass)
        if (reader.superName != internalNameOf<URLClassLoader>()) return null

        val node = ClassNode()
        reader.accept(node, 0)

        node.interfaces.add(internalNameOf<URLClassLoaderAccessor>())

        // public URLClassLoader getWeaveBacking()
        node.visitMethod(Opcodes.ACC_PUBLIC, "getWeaveBacking", "()Ljava/lang/ClassLoader;", null, null).visitAsm {
            aload(0)
            areturn
        }

        // private final List<String> weave$ignoredPackages = new ArrayList<>();
        node.visitField(Opcodes.ACC_PRIVATE or Opcodes.ACC_FINAL, "weave\$ignoredPackages", "Ljava/util/List;", "Ljava/util/List<Ljava/lang/String;>;", null)
        node.methods.filter { it.name == "<init>" }.forEach { methodNode ->
            methodNode.instructions.insert(asm {
                aload(0)
                new("java/util/ArrayList")
                dup
                invokespecial("java/util/ArrayList", "<init>", "()V")
                putfield(node.name, "weave\$ignoredPackages", "Ljava/util/List;")
            })
        }
        node.visitMethod(Opcodes.ACC_PUBLIC, "addWeaveIgnoredPackage", "(Ljava/lang/String;)V", null, null).visitAsm {
            aload(0)
            getfield(node.name, "weave\$ignoredPackages", "Ljava/util/List;")
            aload(1)
            invokeinterface("java/util/List", "add", "(Ljava/lang/Object;)Z")
            pop
            _return
        }
        node.visitMethod(Opcodes.ACC_PRIVATE or Opcodes.ACC_FINAL, "weave\$shouldIgnore", "(Ljava/lang/String;)Z", null, null).visitAsm {
            val loopStart = LabelNode()
            val loopEnd = LabelNode()

            getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
            aload(1)
            invokevirtual("java/io/PrintStream", "println", "(Ljava/lang/String;)V")

            // Iterator<String> iter = weave$ignoredPackages.iterator();
            aload(0)
            getfield(node.name, "weave\$ignoredPackages", "Ljava/util/List;")
            invokeinterface("java/util/List", "iterator", "()Ljava/util/Iterator;")
            astore(2)

            // while (iter.hasNext())
            +loopStart
            aload(2)
            invokeinterface("java/util/Iterator", "hasNext", "()Z")
            ifeq(loopEnd)

            // if (iter.next().startsWith(pkg))
            aload(2)
            invokeinterface("java/util/Iterator", "next", "()Ljava/lang/Object;")
            checkcast("java/lang/String")
            astore(3)
            aload(1)
            aload(3)
            invokevirtual("java/lang/String", "startsWith", "(Ljava/lang/String;)Z")
            ifeq(loopStart)

            // return true
            getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
            ldc("Returning true")
            invokevirtual("java/io/PrintStream", "println", "(Ljava/lang/String;)V")
            iconst_1
            ireturn

            +loopEnd
            // return false
            getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
            ldc("Returning false")
            invokevirtual("java/io/PrintStream", "println", "(Ljava/lang/String;)V")
            iconst_0
            ireturn
        }

        // public void addWeaveURL(URL url)
        node.visitMethod(Opcodes.ACC_PUBLIC, "addWeaveURL", "(Ljava/net/URL;)V", null, null).visitAsm {
            aload(0)
            aload(1)
            invokevirtual(node.name, "addURL", "(Ljava/net/URL;)V")
            _return
        }

        val loadClassInject = asm {
            val notIgnored = LabelNode()

            // if (weave$shouldIgnore(name))
            aload(0)
            aload(1)
            invokespecial(node.name, "weave\$shouldIgnore", "(Ljava/lang/String;)Z")
            ifeq(notIgnored)

            // return ClassLoader.getSystemClassLoader().loadClass(name);
            invokestatic("java/lang/ClassLoader", "getSystemClassLoader", "()Ljava/lang/ClassLoader;")
            aload(1)
            invokevirtual("java/lang/ClassLoader", "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;")
            areturn

            +notIgnored
        }

        listOf("loadClass", "findClass")
            .mapNotNull { t -> node.methods.find { it.name == t } }
            .forEach { it.instructions.insert(loadClassInject) }

        return LoaderClassWriter(loader, reader, ClassWriter.COMPUTE_FRAMES).also { node.accept(it) }.toByteArray()
    }
}