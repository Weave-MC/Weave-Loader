package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.HookManager

fun HookManager.registerDefaultHooks() {
    registerStartGameHook()
    registerShutdownHook()
    registerKeyboardHook()
    registerTickHook()
    registerGuiOpenHook()
    registerRenderGameOverlayHook()
    registerEntityListAddHook()
    registerEntityListRemoveHook()
    registerPlayerListEventHook()
    registerChatReceivedHook()
    registerChatSentHook()
    registerRenderLivingHook()
    registerRenderWorldHook()
    registerRenderHandHook()
    registerMouseHook()
}
