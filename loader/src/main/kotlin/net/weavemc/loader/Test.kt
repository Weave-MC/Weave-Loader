package net.weavemc.loader

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.net.URLClassLoader

class SomeTest {
    fun test() {
        println("bla")
    }
}

fun main() {
    val cl = object : URLClassLoader(emptyArray()) {
        fun createClass(name: String, bytes: ByteArray): Class<*> = defineClass(name, bytes, 0, bytes.size)
    }

    val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES)
    val name = "net/minecraft/client/main/Main"
    writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null)

    with (writer.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null)) {
        visitCode()
        visitInsn(Opcodes.RETURN)
        visitMaxs(-1, -1)
        visitEnd()
    }

    writer.visitEnd()

    cl.createClass(name.replace('/', '.'), writer.toByteArray())
        .getMethod("main", Array<String>::class.java)(null, arrayOf("--version", "1.8.9"))

    SomeTest().test()
}