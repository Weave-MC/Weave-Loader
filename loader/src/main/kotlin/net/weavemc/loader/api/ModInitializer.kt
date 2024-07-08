package net.weavemc.loader.api

import java.lang.instrument.Instrumentation

/**
 * When Weave is loading mods, it will create a new instance of the class that implements this interface.
 * This occurs before the game loads.
 *
 * Override [preInit()][preInit] to initialize your mod.
 * Implementors are expected to have a no-args public constructor
 */
@JvmDefaultWithCompatibility
public interface ModInitializer {
    /**
     * Invoked before Minecraft is initialized.
     *
     * @param inst Instrumentation object which can be used to register custom transformers
     * @since Weave-Loader 1.0.0-beta.1
     */
    @Deprecated("", replaceWith = ReplaceWith("init()"))
    public fun preInit(inst: Instrumentation) {}

    /**
     * Invoked when Minecraft is initialized as to prevent premature class loading of Minecraft related classes.
     *
     * Should be overridden to initialize your mod.
     * @since Weave-Loader 1.0.0-beta.2
     */
    public fun init() {}
}