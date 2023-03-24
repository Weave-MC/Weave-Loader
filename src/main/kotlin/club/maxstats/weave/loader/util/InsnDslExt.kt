package club.maxstats.weave.loader.util

import club.maxstats.weave.loader.api.event.Event
import club.maxstats.weave.loader.api.event.EventBus
import club.maxstats.weave.loader.api.util.InsnBuilder
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.InsnNode

internal inline fun <reified T : Any> InsnBuilder.getSingleton() =
    getstatic(internalNameOf<T>(), "INSTANCE", "L${internalNameOf<T>()};")

internal fun InsnBuilder.callEvent() {
    getSingleton<EventBus>()
    swap
    invokevirtual(
        internalNameOf<EventBus>(),
        "callEvent",
        "(L${internalNameOf<Event>()};)V"
    )
}

internal fun InsnBuilder.returnCorrect(desc: String) {
    val returnType = Type.getReturnType(desc)
    +InsnNode(returnType.stubLoadInsn)
}

private val Type.stubLoadInsn: Int
    get() = when (sort) {
        Type.VOID -> Opcodes.NOP
        Type.BOOLEAN, Type.BYTE, Type.SHORT, Type.CHAR, Type.INT -> Opcodes.ICONST_0
        Type.FLOAT -> Opcodes.FCONST_0
        Type.DOUBLE -> Opcodes.DCONST_0
        Type.LONG -> Opcodes.LCONST_0
        Type.OBJECT, Type.ARRAY -> Opcodes.ACONST_NULL
        else -> error("Invalid non-value type")
    }

internal fun InsnBuilder.println() {
    getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
    swap
    invokevirtual("java/io/PrintStream", "println", "(Ljava/lang/Object;)V")
}
