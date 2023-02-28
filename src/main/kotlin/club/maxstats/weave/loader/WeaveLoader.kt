package club.maxstats.weave.loader

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.hooks.ClassLoaderHackTransformer
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.ProtectionDomain
import java.util.jar.JarFile
import kotlin.io.path.*

object WeaveLoader {
    private val hookManager = HookManager()
    private lateinit var mods: List<Mod>

    @JvmStatic
    fun premain(opt: String?, inst: Instrumentation) {
        inst.addPreinitHook()
        inst.addTransformer(ClassLoaderHackTransformer(), true)
        inst.addTransformer(hookManager.Transformer(), true)
    }

    /** @see [addPreinitHook] */
    fun preinit(cl: ClassLoader) {
        assert(cl is URLClassLoader)

        this.mods = getOrCreateModDirectory()
            .listDirectoryEntries("*.jar")
            .filter { it.isRegularFile() }
            .map { Mod(JarFile(it.toFile()), cl as URLClassLoader) }
            .onEach { it.preinit(hookManager) }
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

    private fun Instrumentation.addPreinitHook() {
        addTransformer(object : ClassFileTransformer {
            override fun transform(
                loader: ClassLoader,
                className: String,
                classBeingRedefined: Class<*>?,
                protectionDomain: ProtectionDomain?,
                classfileBuffer: ByteArray?
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
