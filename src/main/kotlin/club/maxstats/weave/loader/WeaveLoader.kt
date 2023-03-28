package club.maxstats.weave.loader

import club.maxstats.weave.loader.api.Hook
import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.ModInitializer
import kotlinx.serialization.ExperimentalSerializationApi
import java.lang.instrument.Instrumentation
import kotlin.io.path.*

public object WeaveLoader {
    private val hookManager = HookManager()
    private val entryPoints = mutableListOf<String>()

    /**
     * @see [club.maxstats.weave.loader.bootstrap.premain]
     */
    @JvmStatic
    @OptIn(ExperimentalSerializationApi::class)
    public fun preInit(inst: Instrumentation, entryPoints: List<String>, hooks: List<String>) {
        println("[Weave] Initializing Weave")

        inst.addTransformer(hookManager.Transformer())
        hooks.forEach {
            val instance = WeaveLoader::class.java.classLoader.loadClass(it)
                .getConstructor()
                .newInstance() as? Hook
                ?: error("[Weave] $it does not implement Hook!")

            hookManager.register(instance)
        }
        WeaveLoader.entryPoints.addAll(entryPoints)

        println("[Weave] Initialized Weave")
    }

    @JvmStatic
    public fun initMods() {
        println("[Weave] Initializing Mods")

        entryPoints.forEach {
            val instance = WeaveLoader::class.java.classLoader.loadClass(it)
                .getConstructor()
                .newInstance() as? ModInitializer
                ?: error("[Weave] $it does not implement ModInitializer!")

            instance.init()
        }

        println("[Weave] Initialized Mods")
    }
}


