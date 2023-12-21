package net.weavemc.loader.bootstrap.transformer

import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.api.bytecode.named
import net.weavemc.loader.asClassNode
import net.weavemc.loader.asClassReader
import org.objectweb.asm.ClassWriter

object GameInfoTransformer: SafeTransformer {
    override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray? {
        if (className != "net/minecraft/client/main/Main") return null

        val reader = originalClass.asClassReader()
        val node = reader.asClassNode()

        with(node.methods.named("main")) {
            instructions.insert(asm {
                aload(1)
                invokestatic(internalNameOf<GameInfoTransformer>(), "setGameInfo", "([Ljava/lang/String;)V")
            })
        }

        return ClassWriter(reader, ClassWriter.COMPUTE_MAXS).also { node.accept(it) }.toByteArray()
    }

    @JvmStatic
    fun setGameInfo(args: Array<String>) {
        System.getProperties()["weave.main.args"] = args.toList().chunked(2)
            .associate { (a, b) -> a.removePrefix("--") to b }
    }
}