package net.weavemc.loader.bootstrap

import net.weavemc.loader.*
import net.weavemc.loader.bootstrap.transformer.*
import net.weavemc.loader.util.fatalError
import java.awt.GraphicsEnvironment
import java.lang.instrument.Instrumentation

/**
 * The JavaAgent's `premain()` method, this is where initialization of Weave Loader begins.
 * Weave Loader's initialization begins by instantiating [WeaveLoader]
 */
@Suppress("UNUSED_PARAMETER")
fun premain(opt: String?, inst: Instrumentation) {
    println("[Weave] Attached Weave")

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

fun main() {
    fatalError("This is not how you use Weave! Please refer to the readme for instructions.")
}