package net.weavemc.loader.mixins

import net.weavemc.weave.api.bytecode.visitAsm
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import kotlin.random.Random
import kotlin.random.nextUInt

fun ClassLoader.findBytes(name: String) = ClassNode().also {
    ClassReader(
        getResourceAsStream("$name.class")?.readBytes()
            ?: error("Could not automatically get the bytes for $name!")
    ).accept(it, 0)
}

inline fun <reified T : Annotation> AnnotationNode.reflect(
    loader: ClassLoader = T::class.java.classLoader
) = reflect(loader, loader.findBytes(desc.drop(1).dropLast(1))) as T

private val annotationImplementationCache = mutableMapOf<String, Class<*>>()

fun AnnotationNode.reflect(
    loader: ClassLoader,
    annotationClass: ClassNode = loader.findBytes(desc.drop(1).dropLast(1)),
): Any {
    val type = annotationImplementationCache.getOrPut(desc) {
        object : ClassLoader(loader) {
            fun createClass(bytes: ByteArray) = defineClass(null, bytes, 0, bytes.size)
        }.createClass(annotationClass.implementAnnotation())
    }

    fun attachValue(v: Any): Any = when (v) {
        is Type -> loader.loadClass(v.className)
        is Array<*> -> {
            val (enumType, enumName) = v.filterIsInstance<String>()
            loader.loadClass(enumType.replace('/', '.').drop(1).dropLast(1)).getDeclaredField(enumName)[null]
        }

        is AnnotationNode -> v.reflect(loader)
        is List<*> -> v.map { attachValue(it!!) }
        else -> v
    }

    val instance = type.getConstructor().newInstance()
    val windows = (values ?: emptyList()).windowed(2, 2)
    val keys = windows.mapTo(hashSetOf()) { (a) -> a as String }

    windows.forEach { (k, v) -> type.getField(k as String)[instance] = attachValue(v) }
    annotationClass.methods.filter { it.name !in keys }.forEach { defaults ->
        type.getField(defaults.name)[instance] = attachValue(defaults.annotationDefault)
    }

    return instance
}

private inline fun MethodVisitor.implementMethod(block: MethodVisitor.() -> Unit) {
    visitCode()
    block()
    visitMaxs(-1, -1)
    visitEnd()
}

fun ClassNode.implementAnnotation(): ByteArray {
    require(access and ACC_ANNOTATION != 0) { "node did not represent an annotation" }

    val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES)
    val implName = "${name.substringAfterLast('/')}\$impl\$${Random.nextUInt()}"
    writer.visit(V1_8, ACC_PUBLIC or ACC_FINAL, implName, null, "java/lang/Object", arrayOf(name))

    writer.visitMethod(ACC_PUBLIC or ACC_FINAL, "annotationType", "()Ljava/lang/Class;", null, null).implementMethod {
        visitAsm {
            ldc(Type.getObjectType(implName))
            areturn
        }
    }

    writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null).implementMethod {
        visitAsm {
            aload(0)
            invokespecial("java/lang/Object", "<init>", "()V")
            _return
        }
    }

    fun noop(name: String, desc: String) =
        writer.visitMethod(ACC_PUBLIC or ACC_FINAL, name, desc, null, null).implementMethod {
            visitAsm {
                new("java/lang/UnsupportedOperationException")
                dup
                invokespecial("java/lang/UnsupportedOperationException", "<init>", "()V")
                athrow
            }
        }

    noop("equals", "(Ljava/lang/Object;)Z")
    noop("hashCode", "()I")
    noop("toString", "()Ljava/lang/String;")

    methods.filter { it.access and ACC_ABSTRACT != 0 }.forEach { toImplement ->
        val type = Type.getReturnType(toImplement.desc)
        val fieldDesc = type.descriptor

        writer.visitField(ACC_PUBLIC, toImplement.name, fieldDesc, null, null).visitEnd()
        writer.visitMethod(ACC_PUBLIC or ACC_FINAL, toImplement.name, toImplement.desc, null, null).implementMethod {
            visitAsm {
                aload(0)
                getfield(implName, toImplement.name, fieldDesc)
                visitInsn(type.getOpcode(IRETURN))
            }
        }
    }

    return writer.toByteArray()
}