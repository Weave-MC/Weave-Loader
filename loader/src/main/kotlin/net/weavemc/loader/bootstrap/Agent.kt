package net.weavemc.loader.bootstrap

import net.weavemc.loader.WeaveLoader
import net.weavemc.loader.bootstrap.transformer.AntiCacheTransformer
import net.weavemc.loader.bootstrap.transformer.GameInfoTransformer
import net.weavemc.loader.bootstrap.transformer.URLClassLoaderTransformer
import java.lang.instrument.Instrumentation

/**
 * The JavaAgent's `premain()` method, this is where initialization of Weave Loader begins.
 * Weave Loader's initialization begins by calling [WeaveLoader.init], which is loaded through Genesis.
 */
@Suppress("UNUSED_PARAMETER")
fun premain(opt: String?, inst: Instrumentation) {
    println("[Weave] Bootstrapping Weave")

    inst.addTransformer(URLClassLoaderTransformer)
    inst.addTransformer(AntiCacheTransformer)
    inst.addTransformer(GameInfoTransformer)

    inst.addTransformer(Bootstrap(inst))

    println("[Weave] Bootstrapped Weave")
}
