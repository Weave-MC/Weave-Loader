package net.weavemc.loader.mapping

import net.weavemc.weave.api.GameInfo
import net.weavemc.weave.api.gameClient
import net.weavemc.weave.api.gameVersion
import org.objectweb.asm.commons.SimpleRemapper
import java.lang.IllegalStateException

/**
 * Represents anything that can have mapped names
 */
sealed interface Mapped {
    val names: List<String>
}

/**
 * Represents anything that can have comments
 */
sealed interface Commented {
    val comments: List<String>
}

/**
 * Represents a class entry inside of mappings
 */
data class MappedClass(
    override val names: List<String>,
    override val comments: List<String>,
    val fields: List<MappedField>,
    val methods: List<MappedMethod>
) : Mapped, Commented

/**
 * Represents a method entry inside of mappings
 */
data class MappedMethod(
    override val names: List<String>,
    override val comments: List<String>,
    val descriptor: String,
    val parameters: List<MappedParameter>,
    val variables: List<MappedLocal>,
) : Mapped, Commented

/**
 * Represents a local variable mapping for a method inside of mappings
 */
data class MappedLocal(
    val index: Int,
    val startOffset: Int,
    val lvtIndex: Int,
    override val names: List<String>
) : Mapped

/**
 * Represents a parameter mapping for a method inside of mappings
 */
data class MappedParameter(
    override val names: List<String>,
    val index: Int,
) : Mapped

/**
 * Represents a field entry inside of mappings
 */
data class MappedField(
    override val names: List<String>,
    override val comments: List<String>,
    val descriptor: String?,
) : Mapped, Commented

/**
 * Represents any type of mappings
 */
sealed interface Mappings {
    val namespaces: List<String>
    val classes: List<MappedClass>
}

/**
 * Structure that can be used as an intermediate for applying operations on mappings
 */
data class GenericMappings(override val namespaces: List<String>, override val classes: List<MappedClass>) : Mappings

/**
 * Find the index of a given namespace [name], throw [IllegalStateException] when not found
 */
fun Mappings.namespace(name: String) = namespaces.indexOf(name).takeIf { it != -1 } ?: error("Invalid namespace $name")

/**
 * Converts [Mappings] to an ASM [SimpleRemapper]
 * Warning: this may not be a valid remapper for remapping any class
 */
fun Mappings.asSimpleRemapper(from: String, to: String) = SimpleRemapper(asASMMapping(from, to))

private fun MappedField.index(owner: String, namespace: Int) = "$owner.${names[namespace]}"
private fun MappedMethod.index(owner: String, namespace: Int) = "$owner.${names[namespace]}$descriptor"

/**
 * Converts [Mappings] to a name table consumed by [SimpleRemapper]
 */
fun Mappings.asASMMapping(from: String, to: String) = if (this == EmptyMappings) emptyMap() else buildMap {
    val fromIndex = namespaces.indexOf(from)
    val toIndex = namespaces.indexOf(to)

    require(fromIndex >= 0) { "Namespace $from does not exist!" }
    require(toIndex >= 0) { "Namespace $to does not exist!" }

    classes.forEach { clz ->
        val owner = clz.names[fromIndex]
        put(owner, clz.names[toIndex])
        clz.fields.forEach { put(it.index(owner, fromIndex), it.names[toIndex]) }
        clz.methods.forEach { put(it.index(owner, fromIndex), it.names[toIndex]) }
    }
}

/**
 * Represents a serializable mappings format
 */
sealed interface MappingsFormat<T : Mappings> {
    fun detect(lines: List<String>): Boolean
    fun parse(lines: List<String>): T
    fun write(mappings: T): List<String>
}

/**
 * All available mapping formats
 */
val allMappingsFormats = listOf(
    TinyMappingsV1Format, TinyMappingsV2Format,
    SRGMappingsFormat, XSRGMappingsFormat,
    ProguardMappingsFormat
)

/**
 * Loads mappings from a list of lines
 *
 * Note: it is important that there are no leading or trailing empty lines
 * I could've added a filter for this, I suppose
 */
fun loadMappings(lines: List<String>) = allMappingsFormats.find { it.detect(lines) }?.parse(lines)
    ?: error("No format was found for mappings")

/**
 * Loads mappings from a string
 */
fun loadMappings(file: String) = loadMappings(file.trim().lines())

/**
 * Represents mappings without any data
 */
data object EmptyMappings : Mappings {
    override val namespaces: List<String> = emptyList()
    override val classes: List<MappedClass> = emptyList()
}

/**
 * Stub implementation of [MappingsFormat] for [EmptyMappings], for correctness' sake
 */
data object EmptyMappingsFormat : MappingsFormat<EmptyMappings> {
    override fun detect(lines: List<String>) = error("The Empty mapping format should not be used to detect mappings")
    override fun parse(lines: List<String>) = EmptyMappings
    override fun write(mappings: EmptyMappings) = error("Cannot write Empty Mappings")
}

val fullMappings by lazy {
    loadMappings(MappingsManager.getOrCreateBundledMappings().readLines())
}
val mojangMappings by lazy {
    val file = MappingsManager.getMojangMappings()
    if (file == null) {
        println("Failed to specifically retrieve Mojang mappings for ${gameVersion.mappingName}")
        return@lazy EmptyMappings
    }

    loadMappings(file.readLines())
}

val srgMappings by lazy {
    val stream = MappingsManager.getSrgMappings()
    if (stream == null) {
        println("Failed to specifically retrieve SRG mappings for ${gameVersion.mappingName}")
        return@lazy EmptyMappings
    }

    loadMappings(stream)
}
val yarnMappings by lazy {
    val stream = MappingsManager.getYarnMappings()
    if (stream == null) {
        println("Failed to specifically retrieve Yarn mappings for ${gameVersion.mappingName}")
        return@lazy EmptyMappings
    }

    loadMappings(stream)
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

// FIXME: in the future we might need a solid way of providing class bytes
private fun bytesProvider(@Suppress("UNUSED_PARAMETER") expectedNamespace: String): (String) -> ByteArray? = { null }

val environmentMapper: MappingsRemapper by lazy {
    MappingsRemapper("namespaceMapper", environmentMappings, fromNamespace, toNamespace, loader = bytesProvider(fromNamespace))
}
val demapper by lazy { environmentMapper.reverse(bytesProvider(toNamespace)) }

val fullMapper by lazy { MappingsRemapper("full", fullMappings, "named", "official", loader = bytesProvider("named")) }
val emptyMapper by lazy { MappingsRemapper("empty", EmptyMappings, "named", "official", loader = bytesProvider("named"))}

val yarnMapper by lazy { MappingsRemapper("yarn", yarnMappings, "official", "named", loader = bytesProvider("official")) }
val reverseYarnMapper by lazy { yarnMapper.reverse(bytesProvider("named")) }

val mojangMapper by lazy { MappingsRemapper("mojang", mojangMappings, "official", "named", loader = bytesProvider("official")) }
val reverseMojangMapper by lazy { mojangMapper.reverse(bytesProvider("named")) }

val srgMapper by lazy { MappingsRemapper("srg", srgMappings, "named", "official", loader = bytesProvider("official")) }
val reverseSRGMapper by lazy { srgMapper.reverse(bytesProvider("named")) }