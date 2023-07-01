package net.weavemc.loader.api

/**
 * All Weave Loader mods need to implement this class. When Weave is loading mods,
 * it will create a new instance of the class that implements this interface. This occurs before the game loads.
 *
 * Override [preInit()][preInit] to initialize your mod.
 */
public interface ModInitializer {

    /**
     * This is called before the game loads, and should be overridden to initialize your mod.
     *
     * @since Weave-Loader 2.0.0
     */
    public fun preInit() {}

    /**
     * This is the old init(), before Weave-Loader 2.0.0.
     * Please use [preInit()][preInit] to initialize your mod,
     * and [StartGameEvent][net.weavemc.loader.api.event.StartGameEvent] as a replacement for init().
     *
     * **If your mod uses this, please migrate to the replacements linked above.**
     */
    @Deprecated(
        "init() is deprecated since Weave-Loader 2.0.0.",
        ReplaceWith("preInit()"),
        DeprecationLevel.HIDDEN
    )
    public fun init() {}
}
