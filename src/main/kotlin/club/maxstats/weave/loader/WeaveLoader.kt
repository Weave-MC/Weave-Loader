package club.maxstats.weave.loader

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.hooks.ClassLoaderHackTransformer
import club.maxstats.weave.loader.hooks.SafeTransformer
import java.lang.instrument.Instrumentation
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile
import kotlin.io.path.*

object WeaveLoader {

    private val hookManager = HookManager()
    private lateinit var mods: List<Mod>

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

        mods = getOrCreateModDirectory()
            .listDirectoryEntries("*.jar")
            .filter { it.isRegularFile() }
            .map { Mod(JarFile(it.toFile()), cl) }
            .onEach { it.preinit(hookManager) }
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
