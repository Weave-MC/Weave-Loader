package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.Hook
import org.objectweb.asm.tree.ClassNode

class PlayerEventHook : Hook("net/minecraft/entity/player/EntityPlayer") {

    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        /* Placeholder */
    }

}