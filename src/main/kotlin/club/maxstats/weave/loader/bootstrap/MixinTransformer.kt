package club.maxstats.weave.loader.bootstrap

import club.maxstats.weave.loader.WeaveLoader
import club.maxstats.weave.loader.api.util.asm
import club.maxstats.weave.loader.util.getSingleton
import club.maxstats.weave.loader.util.internalNameOf
import club.maxstats.weave.loader.util.named
import club.maxstats.weave.loader.util.next
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

internal object MixinTransformer : SafeTransformer {
    override fun transform(
        loader: ClassLoader,
        className: String,
        originalClass: ByteArray
    ) = when (className) {
        "com/moonsworth/lunar/genesis/Genesis" -> originalClass.modify { node ->
            val method = node.methods.named("main")
            val ldc = method.instructions.filterIsInstance<LdcInsnNode>().first { it.cst == "prebake.cache" }
            val exists = ldc.next<MethodInsnNode> { it.name == "exists" }!!
            method.instructions.insert(exists, asm {
                pop
                iconst_0
            })

            val prebake = node.methods.named("prebake")
            prebake.instructions = asm { _return }
            prebake.exceptions.clear()
            prebake.tryCatchBlocks.clear()
            prebake.localVariables.clear()
        }
        "org/spongepowered/asm/mixin/transformer/MixinProxyImpl" -> originalClass.modify { node ->
            val method = node.methods.named("<init>")
            val superCall = method.instructions.first.next<MethodInsnNode> { it.name == "<init>" }

            method.instructions.insert(superCall, asm {
                aload(1)
                ldc("EXTERNAL_MIXIN")
                invokevirtual("java/lang/String", "equals", "(Ljava/lang/Object;)Z")

                val label = LabelNode()
                ifeq(label)

                aload(0)
                getSingleton<WeaveLoader>()
                invokevirtual(internalNameOf<WeaveLoader>(), "getMixins", "()Ljava/util/List;")
                invokevirtual(
                    "org/spongepowered/asm/mixin/transformer/MixinProxyImpl",
                    "registerMixins",
                    "(Ljava/util/List;)V"
                )

                +label
                f_full(
                    numLocal = 2,
                    local = arrayOf("org/spongepowered/asm/mixin/transformer/MixinProxyImpl", "java/lang/String"),
                    numStack = 0,
                    stack = arrayOf()
                )
            })
        }

        else -> null
    }

    private fun ByteArray.modify(handler: (node: ClassNode) -> Unit): ByteArray {
        val reader = ClassReader(this)
        val node = ClassNode().also { reader.accept(it, 0) }.also(handler)
        val writer = ClassWriter(reader, 0)
        node.accept(writer)
        return writer.toByteArray()
    }
}