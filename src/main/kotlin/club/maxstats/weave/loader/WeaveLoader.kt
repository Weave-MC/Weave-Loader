package club.maxstats.weave.loader

import club.maxstats.weave.loader.hooks.ClassLoaderHackTransformer
import club.maxstats.weave.loader.hooks.HookManagerImpl
import club.maxstats.weave.loader.hooks.PreinitTransformer
import java.lang.instrument.Instrumentation
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile
import java.util.stream.Collectors
import kotlin.io.path.*

object WeaveLoader {
    val hookManager = HookManagerImpl()
    private lateinit var mods: List<Mod>

    @JvmStatic
    fun premain(opt: String?, inst: Instrumentation) {
        inst.addTransformer(PreinitTransformer(inst), false)
        inst.addTransformer(ClassLoaderHackTransformer(), true)
        inst.addTransformer(hookManager.Transformer(), true)
    }

    /** @see [club.maxstats.weave.loader.hooks.PreinitTransformer] */
    fun preinit(cl: ClassLoader) {
        assert(cl is URLClassLoader)

        this.mods = getOrCreateModDirectory()
            .listDirectoryEntries("*.jar")
            .filter { it.isRegularFile() }
            .map { Mod(JarFile(it.toFile()), cl as URLClassLoader) }
    }

    /** @see [club.maxstats.weave.loader.hooks.impl.InitHook] */
    fun init(cl: ClassLoader) {
        println("WEAVE INIT")
        println(mods.size)

        for (mod in this.mods) {
            mod.init()
        }
    }

    private fun getOrCreateModDirectory(): Path {
        val dir = Paths.get(System.getProperty("user.home"), "MaxStats", "Mods")
        if (dir.exists() && !dir.isDirectory()) {
            Files.delete(dir)
        }
        if (!dir.exists()) {
            dir.createDirectories()
        }
        return dir
    }
}
