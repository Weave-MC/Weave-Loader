package club.maxstats.weave.loader.hooks.impl

import club.maxstats.weave.loader.WeaveLoader
import club.maxstats.weave.loader.hooks.Hook
import club.maxstats.weave.loader.util.asm
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode

class InitHook : Hook("net/minecraft/client/Minecraft") {
    override fun transform(cn: ClassNode) {
        val startGame = cn.methods.find { it.name == "startGame" }!!

        startGame.instructions.insertBefore(
            startGame.instructions.findLast { it.opcode == RETURN },
            asm {
                getstatic(
                    "club/maxstats/weave/loader/WeaveLoader",
                    "INSTANCE",
                    "Lclub/maxstats/weave/loader/WeaveLoader;"
                )

                aload(0)
                invokevirtual("java/lang/Object", "getClass", "()Ljava/lang/Class;")
                invokevirtual("java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;")

                invokevirtual("club/maxstats/weave/loader/WeaveLoader", "init", "(Ljava/lang/ClassLoader;)V")
            }
        )
    }
}