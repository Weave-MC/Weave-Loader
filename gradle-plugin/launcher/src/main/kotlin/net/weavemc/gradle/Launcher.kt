package net.weavemc.gradle

import net.weavemc.internals.*
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

fun main(args: Array<String>) {
    System.setProperty("terminal.ansi", "true")

    val version = args.firstOrNull() ?: error("Specify a version to launch!")
    val versionInfo = fetchVersionManifest()?.fetchVersion(version) ?: error("Could not fetch version $version!")

    val gameDir = getMinecraftDir().also { it.createDirectories() }
    val gamePath = gameDir.resolve("versions").resolve(version).resolve("$version.jar")
    if (!gamePath.exists()) error("Minecraft JAR does not exist?")

    val librariesDir = gameDir.resolve("libraries")
    val libraryPaths = mutableListOf<Path>(gamePath)

    // TODO: extract to function?
    for (lib in versionInfo.relevantLibraries) for (download in lib.downloads.allDownloads) {
        val target = download.path.split('/').fold(librariesDir) { acc, curr -> acc.resolve(curr) }
        libraryPaths.add(target)
        if (!target.exists()) DownloadUtil.download(URL(download.url), target)
    }

    val gameArgs = buildList {
        fun addArgument(name: String, value: String) {
            val prefix = "--$name"
            if (prefix !in this) {
                add(prefix)
                add(value)
            }
        }

        addAll(args.drop(1))
        addArgument("accessToken", "0")
        addArgument("version", versionInfo.id)
        addArgument("assetIndex", versionInfo.assetIndex.id)
        addArgument("assetsDir", gameDir.resolve("assets").absolutePathString())
    }.toTypedArray()

    val urls = libraryPaths.mapToArray { it.toUri().toURL() }
    DevLauncherClassLoader(urls).loadClass(versionInfo.mainClass)
        .getMethod("main", gameArgs::class.java)(null, gameArgs)
}

class DevLauncherClassLoader(urls: Array<URL>) : URLClassLoader(urls) {
    private val disallowedReloading = listOf(
        "java.", "javax.", "org.xml.", "org.w3c.", "sun.", "jdk.", "com.sun.management.",
        "kotlin.", "kotlinx.", "org.slf4j."
    )

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        findLoadedClass(name)?.let { return it }
        if (disallowedReloading.any { name.startsWith(it) }) return super.loadClass(name, resolve)

        val clazz = findClass(name)
        if (resolve) resolveClass(clazz)
        return clazz
    }
}

private inline fun <A, reified B> Collection<A>.mapToArray(block: (A) -> B): Array<B> {
    val iterator = iterator()
    return Array(size) { block(iterator.next()) }
}