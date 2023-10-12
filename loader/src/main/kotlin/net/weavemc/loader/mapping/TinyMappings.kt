package net.weavemc.loader.mapping

/**
 * Defines the Tiny v1 format in terms of an abstract Tiny mappings implementation
 */
data object TinyMappingsV1Format : MappingsFormat<TinyMappings> by TinyMappingsFormat(false)

/**
 * Defines the Tiny v2 format in terms of an abstract Tiny mappings implementation
 */
data object TinyMappingsV2Format : MappingsFormat<TinyMappings> by TinyMappingsFormat(true)

/**
 * Writes [TinyMappings] to a [List<String>] representing the tiny mappings file format
 */
fun TinyMappings.write() = (if (isV2) TinyMappingsV2Format else TinyMappingsV1Format).write(this)

/**
 * Abstract implementation of Tiny mappings (v1 and v2)
 */
class TinyMappingsFormat(private val isV2: Boolean) : MappingsFormat<TinyMappings> {

    // If v1: check if line starts with v1 and all other lines are prefixed correctly
    // If v2: check if line starts with tiny
    override fun detect(lines: List<String>) =
        lines.firstOrNull()?.parts()?.first() == (if (isV2) "tiny" else "v1") && (isV2 || lines.drop(1).all {
            it.startsWith("CLASS") || it.startsWith("FIELD") || it.startsWith("METHOD")
        })

    // Quirk: empty name means take the last name
    private fun List<String>.fixNames() = buildList {
        val (first, rest) = this@fixNames.splitFirst()
        require(first.isNotEmpty()) { "first namespaced name is not allowed to be empty in tiny!" }
        add(first)

        rest.forEach { add(it.ifEmpty { last() }) }
    }

    override fun parse(lines: List<String>): TinyMappings {
        require(detect(lines))

        // FIXME: Skip meta for now
        val info = (lines.firstOrNull() ?: error("Mappings are empty!")).parts()
        val namespaces = info.drop(if (isV2) 3 else 1)
        val mapLines = lines.drop(1).dropWhile { it.countStart() != 0 }.filter { it.isNotBlank() }

        return TinyMappings(namespaces, if (isV2) {
            var state: TinyV2State = MappingState()
            mapLines.forEach { state = state.update(it) }
            repeat(2) { state = state.end() }

            (state as? MappingState ?: error("Did not finish walking tree, parsing failed (ended in $state)")).classes
        } else {
            val parted = lines.map { it.parts() }
            val methods = parted.collect("METHOD") { entry ->
                val (desc, names) = entry.splitFirst()
                MappedMethod(
                    names = names.fixNames(),
                    comments = listOf(),
                    descriptor = desc,
                    parameters = listOf(),
                    variables = listOf()
                )
            }

            val fields = parted.collect("FIELD") { entry ->
                val (desc, names) = entry.splitFirst()
                MappedField(
                    names = names.fixNames(),
                    comments = listOf(),
                    descriptor = desc
                )
            }

            parted.filter { (type) -> type == "CLASS" }.map { entry ->
                MappedClass(
                    names = entry.drop(1).fixNames(),
                    comments = listOf(),
                    fields = fields[entry[1]] ?: listOf(),
                    methods = methods[entry[1]] ?: listOf(),
                )
            }
        }, isV2)
    }

    private inline fun <T> List<List<String>>.collect(type: String, mapper: (List<String>) -> T) =
        filter { (t) -> t == type }
            .groupBy { (_, unmappedOwner) -> unmappedOwner }
            .mapValues { (_, entries) -> entries.map { mapper(it.drop(2)) } }

    private sealed interface TinyV2State {

        fun update(line: String): TinyV2State
        fun end(): TinyV2State
    }

    private inner class MappingState : TinyV2State {

        val classes = mutableListOf<MappedClass>()

        override fun update(line: String): TinyV2State {
            val ident = line.countStart()
            val (type, parts) = line.prepare()

            require(ident == 0) { "Invalid indent top-level" }
            require(type == "c") { "Non-class found at top level: $type" }

            return ClassState(this, parts.fixNames())
        }

        override fun end() = this
    }

    private inner class ClassState(val owner: MappingState, val names: List<String>) : TinyV2State {

        val comments = mutableListOf<String>()
        val fields = mutableListOf<MappedField>()
        val methods = mutableListOf<MappedMethod>()

        override fun update(line: String): TinyV2State {
            val ident = line.countStart()
            if (ident < 1) {
                end()
                return owner.update(line)
            }

            val (type, parts) = line.prepare()

            return when (type) {
                "f" -> FieldState(this, parts.first(), parts.drop(1).fixNames())
                "m" -> MethodState(this, parts.first(), parts.drop(1).fixNames())
                "c" -> {
                    comments += parts.joinToString("\t")
                    this
                }

                else -> error("Invalid class member type $type")
            }
        }

        override fun end(): TinyV2State {
            owner.classes += MappedClass(names, comments, fields, methods)
            return owner
        }
    }

    private inner class FieldState(val owner: ClassState, val desc: String, val names: List<String>) : TinyV2State {

        val comments = mutableListOf<String>()

        override fun update(line: String): TinyV2State {
            val ident = line.countStart()
            if (ident < 2) {
                end()
                return owner.update(line)
            }

            val (type, parts) = line.prepare()
            require(type == "c") { "fields can only have comments, found type $type!" }
            comments += parts.joinToString("\t")

            return this
        }

        override fun end(): TinyV2State {
            owner.fields += MappedField(names, comments, desc)
            return owner
        }
    }

    private inner class MethodState(val owner: ClassState, val desc: String, val names: List<String>) : TinyV2State {

        val comments = mutableListOf<String>()
        val parameters = mutableListOf<MappedParameter>()
        val locals = mutableListOf<MappedLocal>()

        override fun update(line: String): TinyV2State {
            val ident = line.countStart()
            if (ident < 2) {
                end()
                return owner.update(line)
            }

            val (type, parts) = line.prepare()
            when (type) {
                "c" -> comments += parts.joinToString("\t")
                "p" -> {
                    val (index, names) = parts.splitFirst()
                    parameters += MappedParameter(names, index.toIntOrNull() ?: error("Invalid index $index"))
                }

                "v" -> {
                    val (idx, offset, lvtIndex) = parts.take(3).map {
                        it.toIntOrNull() ?: error("Invalid index $it for local")
                    }

                    locals += MappedLocal(idx, offset, lvtIndex, parts.drop(3))
                }

                else -> error("Illegal type in method $type")
            }

            return this
        }

        override fun end(): TinyV2State {
            owner.methods += MappedMethod(names, comments, desc, parameters, locals)
            return owner
        }
    }

    private fun String.prepare() = trimIndent().parts().splitFirst()

    private fun <T> List<T>.splitFirst() = first() to drop(1)
    private fun String.countStart(sequence: String = "\t") =
        windowedSequence(sequence.length, sequence.length).takeWhile { it == sequence }.count()

    private fun String.parts() = split('\t')
    private fun List<String>.indent() = map { "\t" + it }
    private fun List<String>.join() = joinToString("\t")

    override fun write(mappings: TinyMappings): List<String> {
        require(mappings.isV2 == isV2) { "tiny mappings versions do not match" }

        val header = (if (isV2) "tiny\t2\t0" else "v1") + "\t${mappings.namespaces.join()}"
        return listOf(header) + if (!isV2) {
            val classesPart = mappings.classes.map { "CLASS\t${it.names.join()}" }
            val methodsPart = mappings.classes.flatMap { c ->
                c.methods.map {
                    "METHOD\t${c.names.first()}\t${it.descriptor}\t${it.names.join()}"
                }
            }

            val fieldsPart = mappings.classes.flatMap { c ->
                c.fields.map {
                    "FIELD\t${c.names.first()}\t${it.descriptor}\t${it.names.join()}"
                }
            }

            classesPart + methodsPart + fieldsPart
        } else mappings.classes.flatMap { c ->
            listOf("c\t${c.names.join()}") + (c.methods.flatMap { m ->
                listOf("m\t${m.descriptor}\t${m.names.join()}") + (m.parameters.map {
                    "p\t${it.index}\t${it.names.join()}"
                } + m.variables.map {
                    "v\t${it.index}\t${it.startOffset}\t${it.lvtIndex}\t${it.names.join()}"
                }).indent()
            } + c.fields.map {
                "f\t${it.descriptor!!}\t${it.names.join()}"
            }).indent()
        }
    }
}

data class TinyMappings(
    override val namespaces: List<String>,
    override val classes: List<MappedClass>,
    val isV2: Boolean
) : Mappings
