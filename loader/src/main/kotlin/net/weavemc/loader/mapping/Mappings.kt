package net.weavemc.loader.mapping

import com.grappenmaker.mappings.*
import net.weavemc.loader.WeaveLoader
import net.weavemc.loader.splitAround
import net.weavemc.api.GameInfo
import net.weavemc.api.GameInfo.Client
import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.api.gameClient
import net.weavemc.api.gameVersion
import net.weavemc.api.mixin.Mixin
import org.objectweb.asm.*
import org.objectweb.asm.commons.AnnotationRemapper
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.commons.SimpleRemapper
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

val fullMappings by lazy {
    MappingsLoader.loadMappings(MappingsManager.getOrCreateBundledMappings().readLines())
}

val environmentNamespace by lazy {
    when (gameClient) {
        // TODO: correct version
        Client.LUNAR -> if (gameVersion < GameInfo.Version.V1_16_5) "mcp" else "mcp"
        Client.FORGE, Client.LABYMOD -> "mcp"
        Client.VANILLA -> "official"
        Client.BADLION -> error("We do not know")
    }
}

const val resourceNamespace = "official"

internal fun bytesProvider(expectedNamespace: String): (String) -> ByteArray? {
    val names = if (expectedNamespace != "official") fullMappings.asASMMapping(
        from = expectedNamespace,
        to = "official",
        includeMethods = false,
        includeFields = false
    ) else emptyMap()

    val mapper = SimpleRemapper(names)
    val callback = ClasspathLoaders.fromLoader(WeaveLoader.javaClass.classLoader)

    return { name -> callback(names[name] ?: name)?.remap(mapper) }
}

fun mapper(from: String, to: String) = MappingsRemapper(fullMappings, from, to, loader = bytesProvider(from))
fun MappingsRemapper.reverseWithBytes() = reverse(bytesProvider(to))

val availableMappers by lazy {
    (fullMappings.namespaces - environmentNamespace).associateWith { mapper(environmentNamespace, it) }
}

val availableUnmappers by lazy { availableMappers.mapValues { it.value.reverseWithBytes() } }

fun cachedMapper(to: String) = availableMappers[to] ?: error("Could not find a mapper for $to!")
fun cachedUnmapper(to: String) = availableUnmappers[to] ?: error("Could not find an unmapper for $to!")

val resourceNameMapper by lazy { cachedMapper(resourceNamespace) }
val resourceNameUnmapper by lazy { cachedUnmapper(resourceNamespace) }
val mappable by lazy {
    val id = fullMappings.namespace("official")
    fullMappings.classes.mapTo(hashSetOf()) { it.names[id] }
}

fun ByteArray.remapToEnvironment(): ByteArray = remap(resourceNameUnmapper)
fun ByteArray.remap(remapper: Remapper): ByteArray {
    val reader = ClassReader(this)
    if (reader.className !in mappable) return this

    val writer = ClassWriter(reader, 0)
    reader.accept(LambdaAwareRemapper(writer, remapper), 0)

    return writer.toByteArray()
}
fun remapModJar(
    mappings: Mappings,
    input: File,
    output: File,
    from: String = "official",
    to: String = "named",
    classpath: List<File> = listOf(),
) {
    val cache = hashMapOf<String, ByteArray?>()
    val jarsToUse = (classpath + input).map { JarFile(it) }
    val lookup = jarsToUse.flatMap { j ->
        j.entries().asSequence().filter { it.name.endsWith(".class") }
            .map { it.name.dropLast(6) to { j.getInputStream(it).readBytes() } }
    }.toMap()

    JarFile(input).use { jar ->
        JarOutputStream(output.outputStream()).use { out ->
            val (classes, resources) = jar.entries().asSequence().partition { it.name.endsWith(".class") }

            fun write(name: String, bytes: ByteArray) {
                out.putNextEntry(JarEntry(name))
                out.write(bytes)
            }

            resources.filterNot { it.name.endsWith(".RSA") || it.name.endsWith(".SF") }
                .forEach { write(it.name, jar.getInputStream(it).readBytes()) }

            val remapper = MappingsRemapper(
                mappings, from, to,
                loader = { name -> if (name in lookup) cache.getOrPut(name) { lookup.getValue(name)() } else null }
            )

            classes.forEach { entry ->
                val reader = ClassReader(jar.getInputStream(entry).readBytes())
                val writer = ClassWriter(reader, 0)

                reader.accept(ModJarRemapper(writer, remapper), 0)

                write("${remapper.map(reader.className)}.class", writer.toByteArray())
            }
        }
    }

    jarsToUse.forEach { it.close() }
}
class ModJarRemapper(
    parent: ClassVisitor,
    remapper: Remapper
): ClassRemapper(Opcodes.ASM9, parent, remapper) {
    var annotationNode: SimpleAnnotationNode? = null
    override fun createAnnotationRemapper(
        descriptor: String?,
        parent: AnnotationVisitor?
    ): AnnotationVisitor {
        if (descriptor != "L${internalNameOf<Mixin>()};")
            return super.createAnnotationRemapper(descriptor, parent)

        annotationNode = SimpleAnnotationNode(parent)
        return annotationNode!!
    }
    override fun createMethodRemapper(parent: MethodVisitor): MethodVisitor {
        if (annotationNode == null)
            return super.createMethodRemapper(parent)

        val targetClass: String = annotationNode!!.values[0].toString()
        // substring targetClass to remove 'L' and ';' from the full name
        val formattedTargetClass = targetClass.substring(1, targetClass.length - 1)
        return ModMethodRemapper(formattedTargetClass, parent, remapper)
    }
}

class SimpleAnnotationNode(
    parent: AnnotationVisitor?
): AnnotationVisitor(Opcodes.ASM9, parent) {
    val values: ArrayList<Any?> = arrayListOf()
    override fun visit(name: String?, value: Any?) {
        values += value
        super.visit(name, value)
    }
}
class ModMethodRemapper(
    private val targetMixinClass: String,
    private val parent: MethodVisitor,
    remapper: Remapper
): LambdaAwareMethodRemapper(parent, remapper) {
    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor {
        val visitor = parent.visitAnnotation(descriptor, visible)
        return if (visitor != null) MixinAnnotationRemapper(targetMixinClass, visitor, descriptor, remapper)
        else super.visitAnnotation(descriptor, visible)
    }
}
class MixinAnnotationRemapper(
    private val targetMixinClass: String,
    private val parent: AnnotationVisitor,
    descriptor: String?,
    remapper: Remapper
): AnnotationRemapper(Opcodes.ASM9, descriptor, parent, remapper) {
    override fun visit(name: String, value: Any) {
        val remappedValue = when {
            value is String && shouldRemap() -> {
                val annotationName = descriptor.substringAfterLast('/')
                when (name) {
                    "method" -> {
                        if (!value.isMethod())
                            error("[Weave] Failed to identify $value as a method in $annotationName annotation")

                        val method = parseMethod(targetMixinClass, value)
                            ?: error("Weave] Failed to parse mixin method value $value of annotation $annotationName")

                        val mappedName = remapper.mapMethodName(method.owner, method.name, method.descriptor)
                        val mappedDesc = remapper.mapMethodDesc(method.descriptor)
                        mappedName + mappedDesc
                    }
                    "field" -> {
                        if (value.isMethod())
                            error("[Weave] Identified $value as a method when field is expected in $annotationName annotation")

                        remapper.mapFieldName(targetMixinClass, value, null)
                    }
                    "target" -> {
                        name.parseAndRemapTarget()
                    }
                    else -> value
                }
            } else -> value
        }
        parent.visit(name, remappedValue)
    }

    private fun String.isMethod(): Boolean {
        val startIndex = this.indexOf('(')
        val endIndex = this.indexOf(')')
        return startIndex != -1 && endIndex != -1 && startIndex < endIndex
    }
    private fun String.parseAndRemapTarget(): String {
        return if (this.isMethod()) {
            val method = parseTargetMethod(this) ?: return this
            val mappedName = remapper.mapMethodName(method.owner, method.name, method.descriptor)
            val mappedDesc = remapper.mapMethodDesc(method.descriptor)
            mappedName + mappedDesc
        } else {
            val field = parseTargetField(this) ?: return this
            remapper.mapFieldName(field.owner, field.name, null)
        }
    }
    private fun shouldRemap(): Boolean =
        descriptor != null && descriptor.startsWith("Lnet/weavemc/api/mixin")
}
data class MethodDeclaration(
    val owner: String,
    val name: String,
    val descriptor: String,
    val returnType: Type,
    val parameterTypes: List<Type>
)
data class FieldDeclaration(
    val owner: String,
    val name: String
)

/**
 * Parses a method with an already known owner
 * @param owner The inferred owner
 * @param methodDeclaration The method declaration in methodName(methodDescriptor)methodReturnType format
 * @return MethodDeclaration data class including the parsed method name, descriptor, and the passed owner param
 */
private fun parseMethod(owner: String, methodDeclaration: String): MethodDeclaration? {
    try {
        val methodName = methodDeclaration.substringBefore('(')
        val methodDesc = methodDeclaration.drop(methodName.length)
        val methodType = Type.getMethodType(methodDesc)
        return MethodDeclaration(
            owner,
            methodName,
            methodDesc,
            methodType.returnType,
            methodType.argumentTypes.toList()
        )
    } catch (ex: Exception) {
        println("Failed to parse method declaration: $methodDeclaration")
        ex.printStackTrace()
    }
    return null
}
private fun parseTargetField(fieldDeclaration: String): FieldDeclaration? {
    try {
        val (classPath, fieldName) = fieldDeclaration.splitAround('.')
        return FieldDeclaration(classPath, fieldName)
    } catch (ex: Exception) {
        println("Failed to parse field declaration: $fieldDeclaration")
        ex.printStackTrace()
    }
    return null
}

/**
 * Parses a method without an already known owner.
 * Typically used for Mixin @At target values, where the owner cannot be inferred.
 * @param methodDeclaration The method declaration in methodOwner.methodName(methodDescriptor)methodReturnType format
 * @return MethodDeclaration data class including the parsed method name, descriptor, and owner
 */
private fun parseTargetMethod(methodDeclaration: String): MethodDeclaration? {
    try {
        val (classPath, fullMethod) = methodDeclaration.splitAround('.')
        val methodName = fullMethod.substringBefore('(')
        val methodDesc = fullMethod.drop(methodName.length)
        val methodType = Type.getMethodType(methodDesc)
        return MethodDeclaration(
            classPath,
            methodName,
            methodDesc,
            methodType.returnType,
            methodType.argumentTypes.toList()
        )
    } catch (ex: Exception) {
        println("Failed to parse method declaration: $methodDeclaration")
        ex.printStackTrace()
    }
    return null
}