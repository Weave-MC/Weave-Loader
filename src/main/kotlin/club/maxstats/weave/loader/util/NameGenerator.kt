package club.maxstats.weave.loader.util

import org.objectweb.asm.tree.ClassNode

fun ClassNode.generateMethodName() =
    generateSequence { generateName() }.first { name -> methods.none { it.name == name } }

fun ClassNode.generateFieldName() =
    generateSequence { generateName() }.first { name -> fields.none { it.name == name } }

private fun generateName() = CharArray(37) { ('a'..'z').random() }.concatToString()