package net.weavemc.internals

internal fun String.splitAround(c: Char): Pair<String,String> =
    substringBefore(c) to substringAfter(c, "")