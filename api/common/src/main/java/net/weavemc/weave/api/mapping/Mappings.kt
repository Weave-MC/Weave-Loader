package net.weavemc.weave.api.mapping

import org.objectweb.asm.commons.SimpleRemapper

sealed interface Mapped {
    val names: List<String>
}

sealed interface Commented {
    val comments: List<String>
}

data class MappedClass(
    override val names: List<String>,
    override val comments: List<String>,
    val fields: List<MappedField>,
    val methods: List<MappedMethod>
) : Mapped, Commented

data class MappedMethod(
    override val names: List<String>,
    override val comments: List<String>,
    val descriptor: String,
    val parameters: List<MappedParameter>,
    val variables: List<MappedLocal>,
) : Mapped, Commented

data class MappedLocal(
    val index: Int,
    val startOffset: Int,
    val lvtIndex: Int,
    override val names: List<String>
) : Mapped

data class MappedParameter(
    override val names: List<String>,
    val index: Int,
) : Mapped

data class MappedField(
    override val names: List<String>,
    override val comments: List<String>,
    val descriptor: String?,
) : Mapped, Commented

sealed interface Mappings {
    val namespaces: List<String>
    val classes: List<MappedClass>
}

data class GenericMappings(override val namespaces: List<String>, override val classes: List<MappedClass>) : Mappings

fun Mappings.namespace(name: String) = namespaces.indexOf(name).takeIf { it != -1 } ?: error("Invalid namespace $name")
fun Mappings.asSimpleRemapper(from: String, to: String) = SimpleRemapper(asASMMapping(from, to))

fun MappedField.index(owner: String, namespace: Int) = "$owner.${names[namespace]}"
fun MappedMethod.index(owner: String, namespace: Int) = "$owner.${names[namespace]}$descriptor"

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

sealed interface MappingsFormat<T : Mappings> {
    fun detect(lines: List<String>): Boolean
    fun parse(lines: List<String>): T
    fun write(mappings: T): List<String>
}

val allMappingsFormats = listOf(TinyMappingsV1Format, TinyMappingsV2Format, SRGMappingsFormat, XSRGMappingsFormat)
fun loadMappings(lines: List<String>) = allMappingsFormats.find { it.detect(lines) }?.parse(lines)
    ?: error("No format was found for mappings")

data object EmptyMappings : Mappings {
    override val namespaces: List<String> = emptyList()
    override val classes: List<MappedClass> = emptyList()
}

data object EmptyMappingsFormat : MappingsFormat<EmptyMappings> {
    override fun detect(lines: List<String>) = error("The Empty mapping format should not be used to detect mappings")
    override fun parse(lines: List<String>) = EmptyMappings
    override fun write(mappings: EmptyMappings) = error("Cannot write Empty Mappings")
}
