package net.weavemc.loader.bootstrap

import me.xtrm.klog.Level
import me.xtrm.klog.dsl.klog
import me.xtrm.klog.dsl.klogConfig
import net.weavemc.api.Tweaker
import net.weavemc.internals.ModConfig
import net.weavemc.loader.WeaveLoader
import net.weavemc.loader.bootstrap.transformer.ArgumentSanitizer
import net.weavemc.loader.bootstrap.transformer.ModInitializerHook
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderTransformer
import net.weavemc.loader.util.*
import java.awt.GraphicsEnvironment
import java.lang.instrument.Instrumentation
import java.util.jar.JarFile

private val logger by klog

/**
 * The JavaAgent's `premain()` method, this is where initialization of Weave Loader begins.
 * Weave Loader's initialization begins by instantiating [WeaveLoader]
 */
@Suppress("UNUSED_PARAMETER")
fun premain(opt: String?, inst: Instrumentation) {
    klogConfig {
        defaultLevel = Level.INFO
        appenders = mutableListOf(WeaveLogAppender)
    }

    logger.info("Attached Weave")

    setGameInfo()
    callTweakers(inst)

    inst.addTransformer(URLClassLoaderTransformer)
    inst.addTransformer(ModInitializerHook(inst))

    inst.addTransformer(ArgumentSanitizer, true)
    inst.tryRetransform("sun.management.RuntimeImpl")
    inst.removeTransformer(ArgumentSanitizer)

    // Prevent ichor prebake
    System.setProperty("ichor.prebakeClasses", "false")

    // Hack: sometimes the state is improperly initialized, which causes Swing to feel like it is headless?
    // Calling this solves the problem
    GraphicsEnvironment.isHeadless()

    // initialize bootstrap
    Bootstrap.bootstrap(inst)
}

private fun Instrumentation.tryRetransform(className: String) {
    val loadedClass = this.allLoadedClasses.firstOrNull { it.name == className }
    if (loadedClass == null) {
        Class.forName(className, false, ClassLoader.getSystemClassLoader())
    } else {
        this.retransformClasses(loadedClass)
    }
}

private fun callTweakers(inst: Instrumentation) {
    logger.info("Calling tweakers")

    val tweakers = FileManager
        .getMods()
        .map { JarFile(it.file) }
        .mapNotNull { runCatching { it.fetchModConfig(JSON) }.getOrNull() }
        .flatMap(ModConfig::tweakers)

    for (tweaker in tweakers) {
        logger.trace("Calling tweaker: $tweaker")
        instantiate<Tweaker>(tweaker).tweak(inst)
    }
}

fun main() {
    fatalError("This is not how you use Weave! Please refer to the readme for instructions.")
}