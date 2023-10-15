package net.weavemc.loader.bootstrap

import net.weavemc.loader.FileManager
import net.weavemc.loader.WeaveLoader
import net.weavemc.weave.api.GameInfo
import net.weavemc.weave.api.GameInfo.Version.*
import net.weavemc.weave.api.gameClient
import net.weavemc.weave.api.gameVersion
import java.io.File
import java.lang.instrument.Instrumentation

/**
 * The JavaAgent's `premain()` method, this is where initialization of Weave Loader begins.
 * Weave Loader's initialization begins by calling [WeaveLoader.init], which is loaded through Genesis.
 */
@Suppress("UNUSED_PARAMETER")
fun premain(opt: String?, inst: Instrumentation) {
    val version = gameVersion
    if (version !in arrayOf(V1_7_10, V1_8_9, V1_12_2, V1_20_1)) {
        println("[Weave] $version not supported, disabling...")
        return
    }

    println("[Weave] Detected Minecraft version: $version")

    inst.addTransformer(URLClassLoaderTransformer)
    inst.addTransformer(AntiLunarCache)
    inst.addTransformer(object : SafeTransformer {
        override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray? {
            // Initialize Weave once the first Minecraft class is loaded into LaunchClassLoader (or main classloader for Minecraft)
            if (
                (gameClient != GameInfo.Client.FORGE && className.startsWith("net/minecraft/client/")) ||
                (gameClient == GameInfo.Client.FORGE && className == "net/minecraftforge/fml/common/Loader")
            ) {
                inst.removeTransformer(AntiLunarCache)
                inst.removeTransformer(this)

                require(loader is URLClassLoaderAccessor) {
                    "ClassLoader was not transformed to implement URLClassLoaderAccessor interface. Report to Developers."
                }

                val versionApi = FileManager.getVersionApi()
                val mods = FileManager.getMods()

                loader.addWeaveURL(FileManager.getCommonApi().toURI().toURL())
                if (versionApi != null)
                    loader.addWeaveURL(versionApi.toURI().toURL())

                mods.forEach { loader.addWeaveURL(it.file.toURI().toURL()) }

                /*
                Load the rest of the loader using Minecraft's class loader.
                This allows us to access Minecraft's classes throughout the project.
                */
                loader.loadClass("net.weavemc.loader.WeaveLoader")
                    .getDeclaredMethod("init", Instrumentation::class.java, File::class.java, List::class.java)
                    .invoke(null, inst, versionApi, mods.map { it.file })
            }

            return null
        }
    })
}
