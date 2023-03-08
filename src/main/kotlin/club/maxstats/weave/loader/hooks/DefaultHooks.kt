package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.HookManager

fun HookManager.registerDefaultHooks() {
    registerShutdownHook()
    registerInputHook()
    registerTickHook()
    registerGuiOpenHook()
    registerChatReceivedHook()
    registerRenderLivingHook()
}