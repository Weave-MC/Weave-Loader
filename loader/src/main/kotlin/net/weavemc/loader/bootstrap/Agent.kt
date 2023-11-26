package net.weavemc.loader.bootstrap

import net.weavemc.api.GameInfo
import net.weavemc.api.gameLauncher
import net.weavemc.loader.WeaveLoader
import java.lang.instrument.Instrumentation

/**
 * The JavaAgent's `premain()` method, this is where initialization of Weave Loader begins.
 * Weave Loader's initialization begins by calling [WeaveLoader.init], which is loaded through Genesis.
 */
@Suppress("UNUSED_PARAMETER")
fun premain(opt: String?, inst: Instrumentation) {
    println("[Weave] Bootstrapping Weave")

    inst.addTransformer(URLClassLoaderTransformer)
    inst.addTransformer(AntiLunarCache)

    inst.addTransformer(when (gameLauncher) {
        GameInfo.Launcher.MULTIMC -> {
            println("[Weave] Detected MultiMC")
            MultiMcInjector(inst)
        }

        GameInfo.Launcher.PRISM -> {
            println("[Weave] Detected Prism")
            PrismLauncherInjector(inst)
        }

        GameInfo.Launcher.OTHER -> {
            WeaveBootstrapEntryPoint(inst)
        }
    })

    println("[Weave] Bootstrapped Weave")
}
