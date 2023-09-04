package net.weavemc.weave.api

/**
 * All Weave Loader mods need to implement this class. When Weave is loading mods,
 * it will create a new instance of the class that implements this interface. This occurs before the game loads.
 *
 * Override [preInit()][preInit] to initialize your mod.
 */
interface ModInitializer {

    /**
     * This is called before the game loads, and should be overridden to initialize your mod.
     *
     * @since Weave-Loader 2.0.0
     */
    fun preInit()

}
