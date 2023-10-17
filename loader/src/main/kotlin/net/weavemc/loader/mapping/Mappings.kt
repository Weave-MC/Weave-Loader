package net.weavemc.loader.mapping

import com.grappenmaker.mappings.*
import net.weavemc.loader.WeaveLoader
import net.weavemc.weave.api.GameInfo
import net.weavemc.weave.api.GameInfo.Client
import net.weavemc.weave.api.gameClient
import net.weavemc.weave.api.gameVersion
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.commons.SimpleRemapper

val fullMappings by lazy {
    MappingsLoader.loadMappings(MappingsManager.getOrCreateBundledMappings().readLines())
}

val environmentNamespace by lazy {
    when (gameClient) {
        // TODO: correct version
        Client.LUNAR -> if (gameVersion < GameInfo.Version.V1_16_5) "mcp" else "mojang"
        Client.FORGE, Client.LABYMOD -> "mcp"
        Client.VANILLA -> "official"
        Client.BADLION -> error("We do not know")
    }
}

const val resourceNamespace = "official"

private fun bytesProvider(expectedNamespace: String): (String) -> ByteArray? {
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