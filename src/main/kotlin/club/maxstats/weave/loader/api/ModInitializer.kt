package club.maxstats.weave.loader.api

interface ModInitializer {
    fun preInit(hookManager: HookManager)
}
