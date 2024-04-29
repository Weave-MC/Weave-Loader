package net.weavemc.loader.bootstrap

import net.weavemc.api.Tweaker
import net.weavemc.internals.GameInfo
import net.weavemc.internals.ModConfig
import net.weavemc.loader.WeaveLoader
import net.weavemc.loader.bootstrap.transformer.ArgumentSanitizer
import net.weavemc.loader.bootstrap.transformer.ModInitializerHook
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderTransformer
import net.weavemc.loader.util.*
import net.weavemc.loader.util.FileManager.ModJar
import java.awt.GraphicsEnvironment
import java.lang.instrument.Instrumentation
import java.util.jar.JarFile
import kotlin.io.path.Path
import kotlin.io.path.pathString

/**
 * The JavaAgent's `premain()` method, this is where initialization of Weave Loader begins.
 * Weave Loader's initialization begins by instantiating [WeaveLoader]
 */
@Suppress("UNUSED_PARAMETER")
fun premain(opt: String?, inst: Instrumentation) {
    println("[Weave] Attached Weave")

    setGameInfo()
    callTweakers(inst)

    inst.addTransformer(URLClassLoaderTransformer)
    inst.addTransformer(ModInitializerHook(inst))
//    inst.addTransformer(ApplicationWrapper)

    inst.addTransformer(ArgumentSanitizer, true)
    inst.retransformClasses(Class.forName("sun.management.RuntimeImpl", false, ClassLoader.getSystemClassLoader()))
    inst.removeTransformer(ArgumentSanitizer)

    // Prevent ichor prebake
    System.setProperty("ichor.prebakeClasses", "false")

    // Hack: sometimes the state is improperly initialized, which causes Swing to feel like it is headless?
    // Calling this solves the problem
    GraphicsEnvironment.isHeadless()

    // initialize bootstrap
    Bootstrap.bootstrap(inst)
}

private fun setGameInfo() {
    val cwd = Path(System.getProperty("user.dir"))
    val version = System.getProperty("weave.environment.version")
        ?: if (cwd.pathString.contains("instances")) {
            val instance = cwd.parent
            val instanceData =
                JSON.decodeFromString<MultiMCInstance>(instance.resolve("mmc-pack.json").toFile().readText())

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
        GameInfo.commandLineArgs.contains("labymod") -> "labymod"
        else -> "vanilla"
    }

    System.getProperties()["weave.game.info"] = mapOf(
        "version" to version,
        "client" to client
    )
}

private fun callTweakers(inst: Instrumentation) {
    println("[Weave] Calling tweakers")

    val tweakers = FileManager
        .getMods()
        .map(ModJar::file)
        .map(::JarFile)
        .map(JarFile::configOrFatal)
        .flatMap(ModConfig::tweakers)

    for (tweaker in tweakers) {
        instantiate<Tweaker>(tweaker).tweak(inst)
    }
}

fun main() {
    fatalError("This is not how you use Weave! Please refer to the readme for instructions.")
}