package net.weavemc.weave.api.mapping

import org.objectweb.asm.commons.SimpleRemapper

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
