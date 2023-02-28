package club.maxstats.weave.loader.api

interface HookManager {
    fun add(vararg hooks: Hook)
}