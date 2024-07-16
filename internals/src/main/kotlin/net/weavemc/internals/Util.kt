package net.weavemc.internals

import kotlin.io.path.Path

internal fun String.splitAround(c: Char): Pair<String, String> =
    substringBefore(c) to substringAfter(c, "")

fun getMinecraftDir() = Path(System.getProperty("user.home", System.getenv("HOME") ?: System.getenv("USERPROFILE")))
    .resolve(with(System.getProperty("os.name").lowercase()) {
        when {
            contains("win") -> Path("AppData", "Roaming", ".minecraft")
            contains("mac") -> Path("Library", "Application Support", "minecraft")
            contains("nix") || contains ("nux") || contains("aix") -> Path(".minecraft")
            else -> error("Unsupported platform ($this)")
        }
    })