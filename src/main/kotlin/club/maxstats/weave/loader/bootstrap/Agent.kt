package club.maxstats.weave.loader.bootstrap

import club.maxstats.weave.loader.api.Priorities
import club.maxstats.weave.loader.api.Priority
import java.lang.instrument.Instrumentation
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile
import kotlin.io.path.*

private var initialised = false
private lateinit var modList: List<Class<*>>

@Suppress("UNUSED_PARAMETER")
fun premain(opt: String?, inst: Instrumentation) {
    inst.addTransformer(object : SafeTransformer {
        override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray? {
            if (!initialised) {
                modList = getModList(inst, loader)
                initialised = true
            }

            if (className.startsWith("net/minecraft/")) {
                inst.removeTransformer(this)

                /*
                 * Load the rest of the loader using Lunar's own class loader.
                 * This allows us to access to Minecraft's classes throughout the project.
                 */

                loader.loadClass("club.maxstats.weave.loader.WeaveLoader")
                    .getDeclaredMethod("preInit", Instrumentation::class.java, ClassLoader::class.java, List::class.java)
                    .invoke(null, inst, loader, modList)
            }

            return null
        }
    })
}

private fun getModList(inst: Instrumentation, classLoader: ClassLoader) =
    getOrCreateModDirectory()
        .listDirectoryEntries("*.jar")
        .asSequence()
        .filter { it.isRegularFile() }
        .map { it.toFile() }
        .map { modFile ->
            println("[Weave] Preloading ${modFile.name}")
            val jar = JarFile(modFile)

            val premainClass = jar.manifest.mainAttributes.getValue("Weave-Premain-Class")

            inst.appendToSystemClassLoaderSearch(jar)

            if (premainClass != null) {
                try {
                    classLoader.loadClass(premainClass)
                        .getDeclaredMethod("premain", Instrumentation::class.java)
                        .invoke(null, inst)
                } catch (e: Exception) {
                    error("Failed to load premainClass $premainClass: ${e.message}")
                }
            }

            val entry = jar.manifest.mainAttributes
                .getValue("Weave-Entry")
                ?: if (premainClass == null) error("Weave-Entry not defined in ${modFile.name}") else return@map null

            val entryClass = classLoader.loadClass(entry) ?: error("$entry does not exist")

            val loadPriority = entryClass
                .getAnnotation(Priority::class.java)
                ?.priority
                ?: Priorities.NORMAL

            entryClass to loadPriority
        }
        .filterNotNull()
        .sortedByDescending { it.second.getPriority() }
        .map { it.first }
        .toList()

private fun getOrCreateModDirectory(): Path {
    val dir = Paths.get(System.getProperty("user.home"), ".lunarclient", "mods")
    if (dir.exists() && !dir.isDirectory()) Files.delete(dir)
    if (!dir.exists()) dir.createDirectory()
    return dir
}
