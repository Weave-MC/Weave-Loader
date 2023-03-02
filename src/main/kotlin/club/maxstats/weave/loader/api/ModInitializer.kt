package club.maxstats.weave.loader.api

interface ModInitializer {
    fun preinit(hookManager: HookManager)
}