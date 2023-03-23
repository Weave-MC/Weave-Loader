package club.maxstats.weave.loader

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.ModInitializer
import java.lang.instrument.Instrumentation
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile
import kotlin.io.path.*

public object WeaveLoader {
    private val hookManager = HookManager()

    /**
     * @see [club.maxstats.weave.loader.bootstrap.premain]
     */
    @JvmStatic
    public fun preInit(inst: Instrumentation, classLoader: ClassLoader) {
        inst.addTransformer(hookManager.Transformer())

        getOrCreateModDirectory()
            .listDirectoryEntries("*.jar")
            .filter { it.isRegularFile() }
            .map { it.toFile() }
            .forEach { modFile ->
                println("[Weave] Loading ${modFile.name}")
                val jar = JarFile(modFile)

                val entry = jar.manifest.mainAttributes.getValue("Weave-Entry")
                    ?: error("Weave-Entry not defined in ${modFile.name}")

                inst.appendToSystemClassLoaderSearch(jar)

                val instance = classLoader.loadClass(entry)
                    .getConstructor()
                    .newInstance() as? ModInitializer
                    ?: error("$entry does not implement ModInitializer")

                instance.preInit(hookManager)
            }
    }

    private fun getOrCreateModDirectory(): Path {
        val dir = Paths.get(System.getProperty("user.home"), ".lunarclient", "mods")
        if (dir.exists() && !dir.isDirectory()) Files.delete(dir)
        if (!dir.exists()) dir.createDirectory()
        return dir
    }
}
