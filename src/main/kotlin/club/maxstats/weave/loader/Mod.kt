package club.maxstats.weave.loader

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.util.addURL
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarFile

class Mod(jar: JarFile, cl: URLClassLoader) {
    private val instance: ModInitializer

    init {
        val entry = jar.manifest.mainAttributes.getValue("Weave-Entry")
            ?: error("No entry defined in ${jar.name}")

//      cl.javaClass.getDeclaredMethod("weave_addURL", URL::class.java)(cl, File(jar.name).toURI().toURL())
        cl.addURL(File(jar.name).toURI().toURL())

        instance = cl.loadClass(entry)
            .getConstructor()
            .newInstance() as? ModInitializer
            ?: error("Entry does not extend from ModInitializer")
    }

    fun preinit(hookManager: HookManager) {
        instance.preinit(hookManager)
    }
}

interface ModInitializer {
    fun preinit(hookManager: HookManager)
}