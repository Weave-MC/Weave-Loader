package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.HookManager
import club.maxstats.weave.loader.hooks.impl.InputEventHook

internal fun HookManager.registerDefaultHooks() {
    register(InputEventHook())
}