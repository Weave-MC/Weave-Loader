package club.maxstats.weave.loader

import club.maxstats.weave.loader.api.CommandBus
import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.api.ModInitializer
import java.lang.instrument.Instrumentation

object WeaveLoader {
    private val hookManager = HookManager()

    /**
     * @see [club.maxstats.weave.loader.bootstrap.premain]
     */
    @JvmStatic
    fun preInit(inst: Instrumentation, classLoader: ClassLoader, modList: List<Class<*>>) {
        inst.addTransformer(hookManager.Transformer())
        CommandBus.init()

        modList
            .forEach { clazz ->
            println("[Weave] Loading ${clazz.name}")

                val instance = classLoader
                    .loadClass(clazz.name)
                    .getConstructor()
                    .newInstance()
                    as? ModInitializer
                    ?: error("${clazz.name} does not implement ModInitializer")

            instance.preInit(hookManager)
        }
    }
}
