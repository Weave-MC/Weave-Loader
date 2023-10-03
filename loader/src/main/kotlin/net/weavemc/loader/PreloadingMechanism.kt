package net.weavemc.loader

import kotlinx.serialization.json.Json
import java.io.File
import java.lang.instrument.Instrumentation
import java.util.jar.JarFile

/**
 * Contains the mechanism for loading mods, along with tweakers.
 */
public object PreloadingMechanism {

    private val json: Json = Json { ignoreUnknownKeys = true }

    /**
     * Initialization of the preloading mechanism, which later on
     */
    @JvmStatic
    public fun init(inst: Instrumentation, apiJar: File, modFiles: List<File>, loader: ClassLoader) {
        /* Add as a backup search path (mainly used for resources) */
        modFiles.forEach {
            inst.appendToSystemClassLoaderSearch(JarFile(it))
        }

        val modJars = findMods(modFiles, loader, inst)

        WeaveLoader.init(inst, apiJar, modJars)
    }

    private fun findMods(modJars: List<File>, loader: ClassLoader, inst: Instrumentation): List<Pair<JarFile, ModConfig>> {
        return modJars.map {
            JarFile(it)
        }.map {
            it to it.getWeaveConfig().also {
                if (it.tweaker != null) {
                    loader.loadClass(it.tweaker)
                        .getDeclaredMethod("tweak", Instrumentation::class.java)
                        .invoke(null, inst)
                }
            }
        }
    }

    private fun JarFile.getWeaveConfig(): ModConfig {
        return json.decodeFromString<ModConfig>(getInputStream(getEntry("weave.mod.json")
            ?: error("$name does not contain a weave.mod.json!")).readBytes().decodeToString())
    }

}
