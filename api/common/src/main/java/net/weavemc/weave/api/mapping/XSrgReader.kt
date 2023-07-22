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
                        val (notchName, mcpName) = data.split(' ')
                        classMappings.add(ClassMapping(mcpName, notchName))
                    }

                    "MD" -> {
                        val (notch, _, mcp, _) = data.split(' ')
                        val (notchOwner, notchName) = notch.splitLast('/')
                        val (mcpOwner, mcpName) = mcp.splitLast('/')
                        methodMappings.add(MethodMapping(mcpOwner, mcpName, notchOwner, notchName))
                    }

                    "FD" -> {
                        val (notch, _, mcp, _) = data.split(' ')
                        val (notchOwner, notchName) = notch.splitLast('/')
                        val (mcpOwner, mcpName) = mcp.splitLast('/')
                        fieldMappings.add(FieldMapping(mcpOwner, mcpName, notchOwner, notchName))
                    }
                }
            }
    }

    data class ClassMapping(
        val mcpName: String,
        val notchName: String,
    )

    data class MethodMapping(
        val mcpOwner: String,
        val mcpName: String,
        val notchOwner: String,
        val notchName: String,
    )

    data class FieldMapping(
        val mcpOwner: String,
        val mcpName: String,
        val notchOwner: String,
        val notchName: String,
    )

    private fun CharSequence.splitLast(delimiter: Char): Pair<String, String> {
        val index = lastIndexOf(delimiter)
        return if (index == -1) "" to toString() else substring(0, index) to substring(index + 1)
    }
}
