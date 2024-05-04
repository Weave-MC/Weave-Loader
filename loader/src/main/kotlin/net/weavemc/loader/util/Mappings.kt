package net.weavemc.loader.util

import com.grappenmaker.mappings.*
import net.weavemc.internals.GameInfo
import net.weavemc.internals.MappingsRetrieval
import net.weavemc.internals.MappingsType.MCP
import net.weavemc.internals.MappingsType.MOJANG
import net.weavemc.internals.MinecraftClient
import net.weavemc.internals.MinecraftVersion
import net.weavemc.loader.InjectionClassWriter
import net.weavemc.loader.WeaveLoader
import org.objectweb.asm.*
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.util.jar.JarFile

object MappingsHandler {
    private val vanillaJar by lazy {
        FileManager.getVanillaMinecraftJar()
    }

    val mergedMappings by lazy {
        println("Loading merged mappings for ${GameInfo.version.versionName}")
        println(" - Vanilla jar: $vanillaJar")
        MappingsRetrieval.loadMergedWeaveMappings(GameInfo.version.versionName, vanillaJar)
    }

    val environmentNamespace by lazy {
        System.getProperty("weave.environment.namespace") ?: when (GameInfo.client) {
            MinecraftClient.LUNAR -> if (GameInfo.version < MinecraftVersion.V1_16_5) MCP.named else MOJANG.named
            MinecraftClient.FORGE -> MCP.srg
            MinecraftClient.VANILLA, MinecraftClient.LABYMOD, MinecraftClient.BADLION -> "official"
        }
    }

    internal fun classLoaderBytesProvider(expectedNamespace: String): (String) -> ByteArray? {
        val names = if (expectedNamespace != "official") mergedMappings.mappings.asASMMapping(
            from = expectedNamespace,
            to = "official",
            includeMethods = false,
            includeFields = false
        ) else emptyMap()

        val mapper = SimpleRemapper(names.toList().associate { (k, v) -> v to k })
        val callback = ClasspathLoaders.fromLoader(WeaveLoader::class.java.classLoader)

        return { name -> callback(names[name] ?: name)?.remap(mapper) }
    }

    private val cachedMappers = mutableMapOf<Pair<String, String>, MappingsRemapper>()

    fun mapper(from: String, to: String) = cachedMappers.getOrPut(from to to) {
        MappingsRemapper(mergedMappings.mappings, from, to, loader = classLoaderBytesProvider(from))
    }

    internal val environmentRemapper = mapper("official", environmentNamespace)
    internal val environmentUnmapper = environmentRemapper.reverse()

    private val mappable by lazy {
        val id = mergedMappings.mappings.namespace("official")
        mergedMappings.mappings.classes.mapTo(hashSetOf()) { it.names[id] }
    }

    internal fun ByteArray.remap(remapper: Remapper, bypassMappableCheck: Boolean = false): ByteArray {
        val reader = ClassReader(this)
        if (reader.className !in mappable && !bypassMappableCheck) return this

        val writer = ClassWriter(reader, 0)
        reader.accept(MinecraftRemapper(writer, remapper), 0)

        return writer.toByteArray()
    }

    internal fun ClassNode.remap(remapper: Remapper, flags: Int = ClassWriter.COMPUTE_MAXS): ClassNode {
        val classWriter = InjectionClassWriter(flags)
        accept(classWriter)

        val bytes = classWriter.toByteArray()
        val remapped = bytes.remap(remapper, true)

        val classReader = ClassReader(remapped)
        val classNode = ClassNode()
        classReader.accept(classNode, 0)

        return classNode
    }

    class SimpleAnnotationNode(
        parent: AnnotationVisitor?,
        val descriptor: String?
    ) : AnnotationVisitor(Opcodes.ASM9, parent) {
        private val values: MutableList<Pair<String?, Any?>> = mutableListOf()

        override fun visit(name: String?, value: Any?) {
            values += name to value
            super.visit(name, value)
        }
    }

    // Remapper meant to be used with loaded Minecraft classes.
    // It will ignore MixinMerged methods (an issue with LabyMod)
    private class MinecraftRemapper(
        parent: ClassVisitor,
        remapper: Remapper
    ) : ClassRemapper(Opcodes.ASM9, parent, remapper) {
        private val annotationNodes: MutableList<SimpleAnnotationNode> = mutableListOf()

        override fun createAnnotationRemapper(
            descriptor: String?,
            parent: AnnotationVisitor?
        ): AnnotationVisitor {
            val node = SimpleAnnotationNode(parent, descriptor)
            annotationNodes += node
            return node
        }

        override fun createMethodRemapper(parent: MethodVisitor): MethodVisitor =
            MinecraftMethodRemapper(annotationNodes.map { it.descriptor ?: "" }, parent, remapper)
    }

    private class MinecraftMethodRemapper(
        private val annotations: List<String>,
        private val parent: MethodVisitor,
        remapper: Remapper
    ) : LambdaAwareMethodRemapper(parent, remapper) {
        override fun visitInvokeDynamicInsn(name: String, descriptor: String, handle: Handle, vararg args: Any) {
            if (annotations.contains("org/spongepowered/asm/mixin/transformer/meta/MixinMerged"))
                parent.visitInvokeDynamicInsn(name, descriptor, handle, args)
            else
                super.visitInvokeDynamicInsn(name, descriptor, handle, *args)
        }
    }

    fun remapModJar(
        mappings: Mappings,
        input: File,
        output: File,
        from: String = "official",
        to: String = environmentNamespace,
        classpath: List<File> = listOf(),
    ) {
        val jarsToUse = (classpath + input).map { JarFile(it) }

        remapJar(mappings, input, output, from, to, ClasspathLoaders.fromJars(jarsToUse).remappingNames(
            mappings = mergedMappings.mappings,
            from = "official",
            to = from,
        ), visitor = relocate()
        )

        jarsToUse.forEach { it.close() }
    }

    fun isNamespaceAvailable(ns: String) = ns in mergedMappings.mappings.namespaces
}

// Oh yeah we love duplicating code
private fun relocate(): ((parent: ClassVisitor) -> ClassVisitor) {
    /*
    ------------------------------------------------------------------------
    <THIS PORTION SHOULD BE UPDATED WHENEVER config-shade.gradle.kts IS UPDATED>
    ------------------------------------------------------------------------
     */
    val relocatePrefix = "net/weavemc/loader/shaded"
    // Note the missing trailing slash in those packages, this makes it so that
    // shadowJar won't rewrite them during relocation (since it's configured to target
    // `org.objectweb.asm.`, not `org.objectweb.asm` for example)
    val mapping = mapOf(
        "org/objectweb/asm" to "$relocatePrefix/asm",
        "com/google" to "$relocatePrefix/google",
        "org/spongepowered" to "$relocatePrefix/spongepowered"
    )
    /*
    -------------------------------------------------------------------------
    </THIS PORTION SHOULD BE UPDATED WHENEVER config-shade.gradle.kts IS UPDATED>
    -------------------------------------------------------------------------
     */

    fun findMapping(name: String) = mapping.entries.find { (k) -> name.startsWith("$k/") }

    fun remap(name: String) = findMapping(name)?.let { (k, v) -> name.replaceFirst(k, v) } ?: name

    val remapper = object : Remapper() {
        override fun map(name: String) = remap(name)
    }

    return { parent -> ClassRemapper(parent, remapper) }
}