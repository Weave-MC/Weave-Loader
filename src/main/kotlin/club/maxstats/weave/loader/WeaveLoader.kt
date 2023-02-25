package club.maxstats.weave.loader

import club.maxstats.weave.loader.hooks.HookTransformer
import java.lang.instrument.Instrumentation

object WeaveLoader {

    @JvmStatic
    fun premain(opt: String?, inst: Instrumentation) {
        inst.addTransformer(HookTransformer(), true)
    }

    /**
     * @see [club.maxstats.weave.loader.hooks.impl.InitHook]
     */
    fun init(cl: ClassLoader) {

    }

}
