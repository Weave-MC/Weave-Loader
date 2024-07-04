package net.weavemc.loader.bootstrap

import net.weavemc.api.Tweaker
import net.weavemc.internals.GameInfo
import net.weavemc.internals.MinecraftVersion
import net.weavemc.internals.ModConfig
import net.weavemc.loader.WeaveLoader
import net.weavemc.loader.bootstrap.transformer.ArgumentSanitizer
import net.weavemc.loader.bootstrap.transformer.ModInitializerHook
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderTransformer
import net.weavemc.loader.util.*
import java.awt.GraphicsEnvironment
import java.io.File
import java.lang.instrument.Instrumentation
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.JarFile

/**
 * The JavaAgent's `premain()` method, this is where initialization of Weave Loader begins.
 * Weave Loader's initialization begins by instantiating [WeaveLoader]
 */
@Suppress("UNUSED_PARAMETER")
fun premain(opt: String?, inst: Instrumentation) {
    println("[Weave] Attached Weave")

    setGameInfo()

    val mods = retrieveMods()
    callTweakers(inst, mods)

    inst.addTransformer(URLClassLoaderTransformer)
    inst.addTransformer(ModInitializerHook)

    inst.addTransformer(ArgumentSanitizer, true)
    inst.retransformClasses(Class.forName("sun.management.RuntimeImpl", false, ClassLoader.getSystemClassLoader()))
    inst.removeTransformer(ArgumentSanitizer)

    // Prevent ichor prebake
    System.setProperty("ichor.prebakeClasses", "false")

    // Hack: sometimes the state is improperly initialized, which causes Swing to feel like it is headless?
    // Calling this solves the problem
    GraphicsEnvironment.isHeadless()

    // initialize bootstrap
    Bootstrap.bootstrap(inst, mods)
}

class TweakerClassLoader(urls: List<URL>) : URLClassLoader(urls.toTypedArray(), ClassLoader.getSystemClassLoader())

private fun callTweakers(inst: Instrumentation, mods: List<File>) {
    println("[Weave] Calling tweakers")

    val tweakers = mods
        .mapNotNull { runCatching { JarFile(it).use { it.fetchModConfig(JSON) } }.getOrNull() }
        .flatMap(ModConfig::tweakers)

    // TODO: could tweakers have dependencies on other mods?
    val loader = TweakerClassLoader(mods.map { it.toURI().toURL() })

    for (tweaker in tweakers) {
        println("[Weave] Calling tweaker: $tweaker")
        instantiate<Tweaker>(tweaker, loader).tweak(inst)
    }
}

private fun FileManager.ModJar.parseAndMap(): File {
    val fileName = file.name.substringBeforeLast('.')

    return JarFile(file).use {
        val config = it.configOrFatal()
        val compiledFor = config.compiledFor

        if (compiledFor != null && GameInfo.version != MinecraftVersion.fromVersionName(compiledFor)) {
            val extra = if (!isSpecific) {
                " Hint: this mod was placed in the general mods folder. Consider putting mods in a version-specific mods folder"
            } else ""

            fatalError(
                "Mod ${config.modId} was compiled for version $compiledFor, current version is ${GameInfo.version.versionName}.$extra"
            )
        }

        if (!MappingsHandler.isNamespaceAvailable(config.namespace)) {
            fatalError("Mod ${config.modId} was mapped in namespace ${config.namespace}, which is not available!")
        }

        file.createRemappedTemp(fileName, config)
    }
}

private fun retrieveMods() = FileManager.getMods().map { it.parseAndMap() }

fun main() {
    fatalError("This is not how you use Weave! Please refer to the readme for instructions.")
}