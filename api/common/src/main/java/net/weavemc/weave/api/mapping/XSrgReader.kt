package net.weavemc.weave.api.mapping

class XSrgReader(srgResourcePath: String) {
    val classMappings = mutableListOf<ClassMapping>()

    val methodMappings = mutableListOf<MethodMapping>()

    val fieldMappings = mutableListOf<FieldMapping>()

    init {
        val mappings = javaClass.getResourceAsStream(srgResourcePath)
            ?.bufferedReader()
            ?.readLines()
            ?: error("Could not find resource $srgResourcePath")

        mappings
            .map { it.split(": ") }
            .forEach { (type, data) ->
                when (type) {
                    "CL" -> {
                        val (firstName, secondName) = data.split(' ')
                        classMappings.add(ClassMapping(firstName, secondName))
                    }

                    "MD" -> {
                        val (firstPath, firstDescriptor, secondPath, secondDescriptor) = data.split(' ')
                        val (firstOwner, firstName) = firstPath.splitLast('/')
                        val (secondOwner, secondName) = secondPath.splitLast('/')
                        methodMappings.add(MethodMapping(
                            firstOwner, firstName, firstDescriptor,
                            secondOwner, secondName, secondDescriptor
                        ))
                    }

                    "FD" -> {
                        val (firstPath, firstDescriptor, secondPath, secondDescriptor) = data.split(' ')
                        val (firstOwner, firstName) = firstPath.splitLast('/')
                        val (secondOwner, secondName) = secondPath.splitLast('/')
                        fieldMappings.add(FieldMapping(firstOwner, firstName, secondOwner, secondName))
                    }
                }
            }
    }

    data class ClassMapping(
        val firstName: String,
        val secondName: String,
    )

    data class MethodMapping(
        val firstOwner: String,
        val firstName: String,
        val firstDescriptor: String,
        val secondOwner: String,
        val secondName: String,
        val secondDescriptor: String
    )

    data class FieldMapping(
        val firstOwner: String,
        val firstName: String,
        val secondOwner: String,
        val secondName: String,
    )

    private fun CharSequence.splitLast(delimiter: Char): Pair<String, String> {
        val index = lastIndexOf(delimiter)
        return if (index == -1) "" to toString() else substring(0, index) to substring(index + 1)
    }
}
