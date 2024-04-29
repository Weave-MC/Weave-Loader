package net.weavemc.loader.bootstrap

import net.weavemc.internals.GameInfo
import net.weavemc.internals.GameInfo.commandLineArgs
import net.weavemc.loader.bootstrap.transformer.SafeTransformer
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderAccessor
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderTransformer
import net.weavemc.loader.util.JSON
import net.weavemc.loader.util.MultiMCInstance
import net.weavemc.loader.util.fatalError
import java.lang.instrument.Instrumentation
import kotlin.io.path.Path
import kotlin.io.path.pathString

object Bootstrap {
    fun bootstrap(inst: Instrumentation) {
        inst.addTransformer(object: SafeTransformer {
            override fun transform(loader: ClassLoader?, className: String, originalClass: ByteArray): ByteArray? {
                if (className.startsWith("net/minecraft/client")) {
                    setGameInfo()
                    printBootstrap(loader)

                    // remove bootstrap transformers
                    inst.removeTransformer(this)
                    inst.removeTransformer(URLClassLoaderTransformer)
//                    inst.removeTransformer(ApplicationWrapper)

                    val clAccessor = if (loader is URLClassLoaderAccessor) loader
                    else fatalError("Failed to transform URLClassLoader to implement URLClassLoaderAccessor. Impossible to recover")

                    runCatching {
                        clAccessor.addWeaveURL(javaClass.protectionDomain.codeSource.location)
                    }.onFailure {
                        it.printStackTrace()
                        fatalError("Failed to deliberately add Weave to the target classloader")
                    }

                    println("[Weave] Bootstrapping complete.")

                    /**
                     * Start the Weave Loader initialization phase
                     */
                    val wlc = loader.loadClass("net.weavemc.loader.WeaveLoader")
                    wlc.getConstructor(
                        URLClassLoaderAccessor::class.java,
                        Instrumentation::class.java
                    ).newInstance(clAccessor, inst)
                }

                return null
            }
        })
    }

    private fun printBootstrap(loader: ClassLoader?) {
        println(
            """
[Weave] Bootstrapping...
    - Version: ${GameInfo.version.versionName}
    - Client: ${GameInfo.client.clientName}
    - Loader: $loader
            """.trim()
        )
    }

    private fun setGameInfo() {
        val cwd = Path(System.getProperty("user.dir"))
        val version: String = if (cwd.pathString.contains("instances")) {
            val instance = cwd.parent
            val instanceData = JSON.decodeFromString<MultiMCInstance>(instance.resolve("mmc-pack.json").toFile().readText())

            instanceData.components.find { it.uid == "net.minecraft" }?.version
                ?: fatalError("Failed to find \"Minecraft\" component in ${instance.pathString}'s mmc-pack.json")
        } else {
            """--version\s+(\S+)""".toRegex()
                .find(System.getProperty("sun.java.command"))
                ?.groupValues?.get(1) ?: fatalError("Could not parse version from command line arguments")
        }

        fun classExists(name: String): Boolean =
            GameInfo::class.java.classLoader.getResourceAsStream("${name.replace('.', '/')}.class") != null

        val client = when {
            classExists("com.moonsworth.lunar.genesis.Genesis") -> "lunar client"
            classExists("net.minecraftforge.fml.common.Loader") -> "forge"
            commandLineArgs.contains("labymod") -> "labymod"
            else -> "vanilla"
        }

        System.getProperties()["weave.game.info"] = mapOf(
            "version" to version,
            "client" to client
        )
    }
}