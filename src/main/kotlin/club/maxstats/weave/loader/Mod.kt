package club.maxstats.weave.loader

import club.maxstats.weave.api.ModInitializer
import club.maxstats.weave.api.hook.HookManager
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.JarFile
import kotlin.system.exitProcess

class Mod(jar: JarFile, cl: URLClassLoader) {
    private val instance: ModInitializer

    init {
        val entry = jar.manifest.mainAttributes.getValue("Weave-Entry")
            ?: error("No entry defined in ${jar.name}")

        val addUrl = cl.javaClass.getDeclaredMethod("weave_addURL", URL::class.java)
        addUrl.invoke(cl, File(jar.name).toURI().toURL())

        instance = cl.loadClass(entry)
            .getConstructor()
            .newInstance() as? ModInitializer
            ?: error("Entry does not extend from ModInitializer")
    }

    fun preinit(hookManager: HookManager) {
        instance.preinit(hookManager)
    }

    fun init() {
        instance.init()
    }
}