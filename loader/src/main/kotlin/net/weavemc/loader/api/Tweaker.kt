package net.weavemc.loader.api

import java.lang.instrument.Instrumentation

/**
 * A hook is a type of weave mod intrinsic that allows modders to run code before the actual main method, similar
 * to a premain method. Implementors are expected to have a no-args public constructor
 */
public interface Tweaker {
    /**
     * This function will be called in premain()
     */
    public fun tweak(inst: Instrumentation)
}