package net.weavemc.loader.mapping

import com.grappenmaker.mappings.*
import net.weavemc.loader.WeaveLoader
import net.weavemc.weave.api.*

val fullMappings by lazy {
    MappingsLoader.loadMappings(MappingsManager.getOrCreateBundledMappings().readLines())
}

val mojangMappings by lazy {
    val file = MappingsManager.getMojangMappings()
    if (file == null) {
        println("Failed to specifically retrieve Mojang mappings for ${gameVersion.mappingName}")
        return@lazy EmptyMappings
    }

    MappingsLoader.loadMappings(file.readLines())
}

val srgMappings by lazy {
    val stream = MappingsManager.getSrgMappings()
    if (stream == null) {
        println("Failed to specifically retrieve SRG mappings for ${gameVersion.mappingName}")
        return@lazy EmptyMappings
    }

    MappingsLoader.loadMappings(stream)
}

val yarnMappings by lazy {
    val stream = MappingsManager.getYarnMappings()
    if (stream == null) {
        println("Failed to specifically retrieve Yarn mappings for ${gameVersion.mappingName}")
        return@lazy EmptyMappings
    }

    MappingsLoader.loadMappings(stream)
}

val environmentMappings by lazy {
    when (gameClient) {
        GameInfo.Client.LUNAR -> mojangMappings
        GameInfo.Client.FORGE -> srgMappings
        else -> yarnMappings // not the safest
    }
}

val isEmptyMappings get() = environmentMappings == EmptyMappings

val fromNamespace = when {
    isEmptyMappings -> "none"
    else -> "named"
}

val toNamespace = when {
    isEmptyMappings -> "none"
    else -> when (gameClient) {
        GameInfo.Client.FORGE -> "srg"
        else -> "official"
    }
}

private fun bytesProvider(expectedNamespace: String): (String) -> ByteArray? {
    val names = if (expectedNamespace != "official") fullMappings.asASMMapping(
        from = expectedNamespace,
        to = "official",
        includeMethods = false,
        includeFields = false
    ) else emptyMap()

    return { name ->
        WeaveLoader.javaClass.classLoader.getResourceAsStream("${names[name] ?: name}.class")?.readBytes()
            ?: throw ClassNotFoundException("Failed to retrieve class from resources: $name")
    }
}

val environmentMapper: MappingsRemapper by lazy {
    MappingsRemapper(environmentMappings, fromNamespace, toNamespace, loader = bytesProvider(fromNamespace))
}

val demapper by lazy { environmentMapper.reverse(bytesProvider(toNamespace)) }

val fullMapper by lazy { MappingsRemapper(fullMappings, "named", "official", loader = bytesProvider("named")) }
val emptyMapper by lazy { MappingsRemapper(EmptyMappings, "named", "official", loader = bytesProvider("named"))}

val yarnMapper by lazy { MappingsRemapper(yarnMappings, "official", "named", loader = bytesProvider("official")) }
val reverseYarnMapper by lazy { yarnMapper.reverse(bytesProvider("named")) }

val mojangMapper by lazy { MappingsRemapper(mojangMappings, "official", "named", loader = bytesProvider("official")) }
val reverseMojangMapper by lazy { mojangMapper.reverse(bytesProvider("named")) }

val srgMapper by lazy { MappingsRemapper(srgMappings, "named", "official", loader = bytesProvider("official")) }
val reverseSRGMapper by lazy { srgMapper.reverse(bytesProvider("named")) }

fun findMapper(name: String) = when (name) {
    "srg" -> srgMapper
    "yarn" -> yarnMapper
    "mojang" -> mojangMapper
    else -> error("Invalid mapping id $name!")
}

val MappingsRemapper.name get() = when (this) {
    mojangMapper -> "mojang"
    yarnMapper -> "yarn"
    srgMapper -> "srg"
    else -> error("Mapper $this is not a mapper known to Weave Loader!")
}