package club.maxstats.weave.loader

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.ModInitializer
import club.maxstats.weave.loader.hooks.ClassLoaderHackTransformer
import club.maxstats.weave.loader.hooks.SafeTransformer
import club.maxstats.weave.loader.util.addURL
import java.lang.instrument.Instrumentation
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile
import kotlin.io.path.*

object WeaveLoader {

    private val hookManager = HookManager()

    @JvmStatic
    fun premain(opt: String?, inst: Instrumentation) {
        inst.addPreinitHook()
        inst.addTransformer(ClassLoaderHackTransformer())
        inst.addTransformer(hookManager.Transformer())
    }

    /**
     * @see [addPreinitHook]
     */
    fun preinit(cl: ClassLoader) {
        require(cl is URLClassLoader) { "Non-URLClassLoader is not supported by Weave!" }

        getOrCreateModDirectory()
            .listDirectoryEntries("*.jar")
            .filter { it.isRegularFile() }
            .map { it.toFile() }
            .forEach { mod ->
                val entry = JarFile(mod).manifest.mainAttributes.getValue("Weave-Entry")
                    ?: error("Weave-Entry not defined in ${mod.path}")

                cl.addURL(mod.toURI().toURL())

                val instance = cl.loadClass(entry)
                    .getConstructor()
                    .newInstance() as? ModInitializer
                    ?: error("$entry does not implement ModInitializer")

                instance.preinit(hookManager)
            }
    }

    private fun getOrCreateModDirectory(): Path {
        val dir = Paths.get(System.getProperty("user.home"), "MaxStats", "Mods")
        if (dir.exists() && !dir.isDirectory()) Files.delete(dir)
        if (!dir.exists()) dir.createDirectories()
        return dir
    }

    private fun Instrumentation.addPreinitHook() {
        addTransformer(object : SafeTransformer() {
            override fun transform(
                loader: ClassLoader,
                className: String,
                originalClass: ByteArray
            ): ByteArray? {
                if (className.startsWith("net/minecraft/")) {
                    preinit(loader)
                    removeTransformer(this)
                }

                return null
            }
        })
    }

}
