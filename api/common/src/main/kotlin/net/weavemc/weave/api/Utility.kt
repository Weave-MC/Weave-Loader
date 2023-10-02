@file:JvmName("Utility")

package net.weavemc.weave.api

import net.weavemc.weave.api.GameInfo.Client.*
import net.weavemc.weave.api.mapping.*

val gameInfo by lazy { GameInfo(gameVersion, gameClient) }
val command: String = System.getProperty("sun.java.command") ?: error("Could not find command")

val gameVersion: GameInfo.Version by lazy {
    """--version\s+(?:\S*?)?(\d+\.\d+(?:\.\d+)?)"""
        .toRegex()
        .find(command)?.groupValues
        ?.get(1)
        ?.let(GameInfo.Version::fromVersionName)
        ?: error("Could not find game version")
}

val gameClient: GameInfo.Client by lazy {
    val isLunar = "Genesis" in command
    val version = """--version\s+(\S+)""".toRegex().find(System.getProperty("sun.java.command"))
        ?.groupValues?.get(1)?.lowercase() ?: error("Failed to retrieve version from command line")

    when {
        isLunar -> LUNAR
        "forge" in version -> FORGE
        "labymod" in version -> LABYMOD
        else -> VANILLA
    }
}

val fullMappings by lazy {
    val resourceName = "weave/mappings/${gameVersion.versionName}-srg.tiny"
    loadMappings(
        ClassLoader.getSystemResourceAsStream(resourceName)
            ?.readBytes()?.decodeToString()?.trim()?.lines()
            ?: error("Could not find mappings for `$gameVersion` ($resourceName) on classpath")
    )
}

val mappings by lazy { if (gameClient == GameInfo.Client.LUNAR) EmptyMappings else fullMappings }

val fromNamespace = when {
    isEmptyMappings -> "none"
    else -> "named"
}

val toNamespace = when {
    isEmptyMappings -> "none"
    else -> when (gameClient) {
        FORGE -> "srg"
        else -> "official"
    }
}

//private fun bytesProvider(expectedNamespace: String): (String) -> ByteArray? {
//    val intermediateMapper = MappingsRemapper(mappings, "official", expectedNamespace, shouldRemapDesc = false) { null }
//
//    return (a@ { name ->
//        val actualName = vanillaMapper.map(name)
//        val unmappedBytes = vanillaMapper.javaClass.classLoader.getResourceAsStream("$actualName.class")
//            ?.readBytes() ?: return@a null
//
//        val reader = ClassReader(unmappedBytes)
//        val writer = ClassWriter(reader, 0)
//        reader.accept(LambdaAwareRemapper(writer, intermediateMapper), 0)
//
//        writer.toByteArray()
//    })
//}

// FIXME: in the future we might need a solid way of providing class bytes
private fun bytesProvider(@Suppress("UNUSED_PARAMETER") expectedNamespace: String): (String) -> ByteArray? = { null }

val mapper: MappingsRemapper by lazy {
    MappingsRemapper(mappings, fromNamespace, toNamespace, loader = bytesProvider(fromNamespace))
}

val demapper by lazy { mapper.reverse(bytesProvider(toNamespace)) }

val vanillaMapper by lazy { MappingsRemapper(fullMappings, "named", "official", loader = bytesProvider("named")) }
val namedMapper by lazy { vanillaMapper.reverse(bytesProvider("official")) }
val runtimeMapper by lazy { MappingsRemapper(fullMappings, "named", toNamespace, loader = bytesProvider("named")) }

val isEmptyMappings get() = mappings == EmptyMappings

fun getMappedClassEntry(name: String) = if (isEmptyMappings) MappedClass(
    names = listOf(name, name),
    comments = emptyList(),
    fields = emptyList(),
    methods = emptyList()
) else mappings.classes.find { it.names.first() == name }

fun getMappedClass(name: String) = mapper.map(name)

// these are dummy values, not the actual ones in the mappings: the mappings may be incomplete, but the api really
// wants these mapped field/method objects...
fun getMappedField(owner: String, name: String) = MappedField(
    names = listOf(name, if (isEmptyMappings) name else mapper.mapFieldName(owner, name, null)),
    comments = emptyList(),
    descriptor = null
)

fun getMappedMethod(owner: String, name: String, desc: String) = MappedMethod(
    names = listOf(name, if (isEmptyMappings) name else mapper.mapMethodName(owner, name, desc)),
    comments = emptyList(),
    variables = emptyList(),
    parameters = emptyList(),
    descriptor = mapper.mapMethodDesc(desc),
)

val Mapped.runtimeName get() = names.last()
