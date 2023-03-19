package club.maxstats.weave.loader.hooks

import club.maxstats.weave.loader.api.Hook
import club.maxstats.weave.loader.api.event.StartGameEvent
import club.maxstats.weave.loader.util.asm
import club.maxstats.weave.loader.util.callEvent
import club.maxstats.weave.loader.util.getSingleton
import club.maxstats.weave.loader.util.named
import net.minecraft.client.Minecraft
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

class StartGameEventHook : Hook(Minecraft::class) {
    override fun transform(node: ClassNode, cfg: AssemblerConfig) {
        val preInsn = asm {
            getSingleton<StartGameEvent.Pre>()
            callEvent()
        }

        val postInsn = asm {
            getSingleton<StartGameEvent.Post>()
            callEvent()
        }

        val mn = node.methods.named("startGame")
        mn.instructions.insert(preInsn)
        mn.instructions.insertBefore(mn.instructions.find { it.opcode == Opcodes.RETURN }, postInsn)
    }
}
