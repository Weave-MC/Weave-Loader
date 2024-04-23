package net.weavemc.loader.bootstrap

import net.weavemc.loader.*
import net.weavemc.loader.bootstrap.transformer.*
import java.awt.GraphicsEnvironment
import java.lang.instrument.Instrumentation
import javax.swing.JOptionPane
import kotlin.system.exitProcess

/**
 * The JavaAgent's `premain()` method, this is where initialization of Weave Loader begins.
 * Weave Loader's initialization begins by instantiating [WeaveLoader]
 */
@Suppress("UNUSED_PARAMETER")
fun premain(opt: String?, inst: Instrumentation) {
    println("[Weave] Attached Weave")

    inst.addTransformer(URLClassLoaderTransformer)
    inst.addTransformer(MixinRelocator)
    inst.addTransformer(ApplicationWrapper)

    inst.addTransformer(ArgumentSanitizer, true)
    inst.retransformClasses(Class.forName("sun.management.RuntimeImpl", false, ClassLoader.getSystemClassLoader()))
    inst.removeTransformer(ArgumentSanitizer)

    // Prevent ichor prebake
    System.setProperty("ichor.prebakeClasses", "false")

    // Hack: sometimes the state is improperly initialized, which causes Swing to feel like it is headless?
    // Calling this solves the problem
    GraphicsEnvironment.isHeadless()

    // initialize bootstrap
    BootstrapContainer.offerInstrumentation(inst)
}

fun main() {
    JOptionPane.showMessageDialog(
        null,
        "This is not how you use Weave! Please refer to the readme for instructions.",
        "Weave Loader Error",
        JOptionPane.ERROR_MESSAGE
    )

    exitProcess(-1)
}