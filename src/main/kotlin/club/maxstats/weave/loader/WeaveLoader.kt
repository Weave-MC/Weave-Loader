package club.maxstats.weave.loader

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.ModInitializer
import kotlinx.serialization.ExperimentalSerializationApi
import java.lang.instrument.Instrumentation
import kotlin.io.path.*

public object WeaveLoader {
    private val hookManager = HookManager()

    /**
     * @see [club.maxstats.weave.loader.bootstrap.premain]
     */
    @JvmStatic
    @OptIn(ExperimentalSerializationApi::class)
    public fun preInit(inst: Instrumentation, entryPoints: List<String>) {
        println("[Weave] Initializing Weave")
        inst.addTransformer(hookManager.Transformer())

        entryPoints.forEach {
            val instance = WeaveLoader::class.java.classLoader.loadClass(it)
                .getConstructor()
                .newInstance() as? ModInitializer
                ?: error("[Weave] $it does not implement ModInitializer!")

            instance.preInit(hookManager)
        }

        println("[Weave] Initialized Weave")
    }
}


