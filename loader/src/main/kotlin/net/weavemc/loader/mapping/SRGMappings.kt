package net.weavemc.loader.mapping

import org.objectweb.asm.commons.SimpleRemapper

/**
 * Represents mappings in the Surge format
 */
data class SRGMappings(override val classes: List<MappedClass>, val isExtended: Boolean) : Mappings {
    override val namespaces = listOf("official", "named")
}

/**
 * Writes [SRGMappings] to a [List<String>] representing the SRG file format
 */
fun SRGMappings.write() = (if (isExtended) XSRGMappingsFormat else SRGMappingsFormat).write(this)

/**
 * Converts [SRGMappings] to a [SimpleRemapper]
 */
fun SRGMappings.asSimpleRemapper() = asSimpleRemapper(namespaces[0], namespaces[1])

/**
 * Defines the SRG format in terms of an abstract SRG implementation
 */
data object SRGMappingsFormat : MappingsFormat<SRGMappings> by BasicSRGParser(false)

/**
 * Defines the XSRG format in terms of an abstract SRG implementation
 */
data object XSRGMappingsFormat : MappingsFormat<SRGMappings> by BasicSRGParser(true)

/**
 * Abstract implementation of (X)SRG
 */
private class BasicSRGParser(private val isExtended: Boolean) : MappingsFormat<SRGMappings> {
    private val entryTypes = setOf("CL", "FD", "MD", "PK")
    override fun detect(lines: List<String>): Boolean {
        if (!lines.all { it.substringBefore(':') in entryTypes }) return false
        return lines.find { it.startsWith("FD:") }?.let { l -> l.split(" ").size > 3 == isExtended } ?: true
    }

    override fun parse(lines: List<String>): SRGMappings {
        val parted = lines.map { it.split(" ") }
        val fields = parted.collect("FD") { parts ->
            val from = parts[0]
            val to = parts[if (isExtended) 2 else 1]

            MappedField(
                names = listOf(from.substringAfterLast('/'), to.substringAfterLast('/')),
                comments = listOf(),
                descriptor = if (isExtended) parts[1] else null
            )
        }

        val methods = parted.collect("MD") { (from, fromDesc, to) ->
            MappedMethod(
                names = listOf(from.substringAfterLast('/'), to.substringAfterLast('/')),
                comments = listOf(),
                descriptor = fromDesc,
                parameters = listOf(),
                variables = listOf()
            )
        }

        val classEntries = parted.filter { (type) -> type == "CL:" }

        // Make sure we do not forget about orphaned ones
        // (sometimes mappings do not specify mappings for the class but they do for some entries)
        val missingClasses = methods.keys + fields.keys - classEntries.map { (_, from) -> from }.toSet()
        val classes = classEntries.map { (_, from, to) ->
            MappedClass(
                names = listOf(from, to),
                comments = listOf(),
                fields = fields[from] ?: listOf(),
                methods = methods[from] ?: listOf()
            )
        } + missingClasses.map { name ->
            MappedClass(
                names = listOf(name, name),
                comments = listOf(),
                fields = fields[name] ?: listOf(),
                methods = methods[name] ?: listOf()
            )
        }

        return SRGMappings(classes, isExtended)
    }

    private inline fun <T> List<List<String>>.collect(type: String, mapper: (List<String>) -> T) =
        filter { (t) -> t == "$type:" }
            .groupBy { (_, from) -> from.substringBeforeLast('/') }
            .mapValues { (_, entries) -> entries.map { mapper(it.drop(1)) } }

    override fun write(mappings: SRGMappings): List<String> {
        require(mappings.isExtended == isExtended) { "Cannot write XSRG as SRG, or SRG as XSRG" }

        val classesPart = mappings.classes.map { "CL: ${it.names.first()} ${it.names.last()}" }
        val fieldsPart = mappings.classes.flatMap { c ->
            c.fields.map {
                val ext = if (isExtended) " ${it.descriptor}" else ""
                "FD: ${c.names.first()}/${it.names.first()}$ext ${c.names.last()}/${it.names.last()} ${it.descriptor}"
            }
        }

        val methodsParts = mappings.classes.flatMap { c ->
            c.methods.map {
                "MD: ${c.names.first()}/${it.names.first()} ${it.descriptor} " +
                    "${c.names.last()}/${it.names.last()} ${it.descriptor}"
            }
        }

        return classesPart + fieldsPart + methodsParts
    }
}
