package net.weavemc.weave.api.bytecode

import net.weavemc.weave.api.event.Event
import net.weavemc.weave.api.event.EventBus
import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import java.io.FileOutputStream

@Suppress(
    "PropertyName",
    "unused",
    "FunctionName",
    "SpellCheckingInspection",
    "NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING",
    "NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING",
    "MemberVisibilityCanBePrivate",
)
sealed class InsnBuilder {

    abstract operator fun AbstractInsnNode.unaryPlus()
    abstract operator fun InsnList.unaryPlus()

    fun getstatic(owner: String, name: String, desc: String) = +FieldInsnNode(Opcodes.GETSTATIC, owner, name, desc)
    fun putstatic(owner: String, name: String, desc: String) = +FieldInsnNode(Opcodes.PUTSTATIC, owner, name, desc)
    fun getfield(owner: String, name: String, desc: String) = +FieldInsnNode(Opcodes.GETFIELD, owner, name, desc)
    fun putfield(owner: String, name: String, desc: String) = +FieldInsnNode(Opcodes.PUTFIELD, owner, name, desc)

    fun iinc(`var`: Int, incr: Int) = +IincInsnNode(`var`, incr)

    @get:JvmName("nop") val nop get() = +InsnNode(Opcodes.NOP)
    @get:JvmName("aconst_null") val aconst_null get() = +InsnNode(Opcodes.ACONST_NULL)
    @get:JvmName("iconst_m1") val iconst_m1 get() = +InsnNode(Opcodes.ICONST_M1)
    @get:JvmName("iconst_0") val iconst_0 get() = +InsnNode(Opcodes.ICONST_0)
    @get:JvmName("iconst_1") val iconst_1 get() = +InsnNode(Opcodes.ICONST_1)
    @get:JvmName("iconst_2") val iconst_2 get() = +InsnNode(Opcodes.ICONST_2)
    @get:JvmName("iconst_3") val iconst_3 get() = +InsnNode(Opcodes.ICONST_3)
    @get:JvmName("iconst_4") val iconst_4 get() = +InsnNode(Opcodes.ICONST_4)
    @get:JvmName("iconst_5") val iconst_5 get() = +InsnNode(Opcodes.ICONST_5)
    @get:JvmName("lconst_0") val lconst_0 get() = +InsnNode(Opcodes.LCONST_0)
    @get:JvmName("lconst_1") val lconst_1 get() = +InsnNode(Opcodes.LCONST_1)
    @get:JvmName("fconst_0") val fconst_0 get() = +InsnNode(Opcodes.FCONST_0)
    @get:JvmName("fconst_1") val fconst_1 get() = +InsnNode(Opcodes.FCONST_1)
    @get:JvmName("fconst_2") val fconst_2 get() = +InsnNode(Opcodes.FCONST_2)
    @get:JvmName("dconst_0") val dconst_0 get() = +InsnNode(Opcodes.DCONST_0)
    @get:JvmName("dconst_1") val dconst_1 get() = +InsnNode(Opcodes.DCONST_1)
    @get:JvmName("iaload") val iaload get() = +InsnNode(Opcodes.IALOAD)
    @get:JvmName("laload") val laload get() = +InsnNode(Opcodes.LALOAD)
    @get:JvmName("faload") val faload get() = +InsnNode(Opcodes.FALOAD)
    @get:JvmName("daload") val daload get() = +InsnNode(Opcodes.DALOAD)
    @get:JvmName("aaload") val aaload get() = +InsnNode(Opcodes.AALOAD)
    @get:JvmName("baload") val baload get() = +InsnNode(Opcodes.BALOAD)
    @get:JvmName("caload") val caload get() = +InsnNode(Opcodes.CALOAD)
    @get:JvmName("saload") val saload get() = +InsnNode(Opcodes.SALOAD)
    @get:JvmName("iastore") val iastore get() = +InsnNode(Opcodes.IASTORE)
    @get:JvmName("lastore") val lastore get() = +InsnNode(Opcodes.LASTORE)
    @get:JvmName("fastore") val fastore get() = +InsnNode(Opcodes.FASTORE)
    @get:JvmName("dastore") val dastore get() = +InsnNode(Opcodes.DASTORE)
    @get:JvmName("aastore") val aastore get() = +InsnNode(Opcodes.AASTORE)
    @get:JvmName("bastore") val bastore get() = +InsnNode(Opcodes.BASTORE)
    @get:JvmName("castore") val castore get() = +InsnNode(Opcodes.CASTORE)
    @get:JvmName("sastore") val sastore get() = +InsnNode(Opcodes.SASTORE)
    @get:JvmName("pop") val pop get() = +InsnNode(Opcodes.POP)
    @get:JvmName("pop2") val pop2 get() = +InsnNode(Opcodes.POP2)
    @get:JvmName("dup") val dup get() = +InsnNode(Opcodes.DUP)
    @get:JvmName("dup_x1") val dup_x1 get() = +InsnNode(Opcodes.DUP_X1)
    @get:JvmName("dup_x2") val dup_x2 get() = +InsnNode(Opcodes.DUP_X2)
    @get:JvmName("dup2") val dup2 get() = +InsnNode(Opcodes.DUP2)
    @get:JvmName("dup2_x1") val dup2_x1 get() = +InsnNode(Opcodes.DUP2_X1)
    @get:JvmName("dup2_x2") val dup2_x2 get() = +InsnNode(Opcodes.DUP2_X2)
    @get:JvmName("swap") val swap get() = +InsnNode(Opcodes.SWAP)
    @get:JvmName("iadd") val iadd get() = +InsnNode(Opcodes.IADD)
    @get:JvmName("ladd") val ladd get() = +InsnNode(Opcodes.LADD)
    @get:JvmName("fadd") val fadd get() = +InsnNode(Opcodes.FADD)
    @get:JvmName("dadd") val dadd get() = +InsnNode(Opcodes.DADD)
    @get:JvmName("isub") val isub get() = +InsnNode(Opcodes.ISUB)
    @get:JvmName("lsub") val lsub get() = +InsnNode(Opcodes.LSUB)
    @get:JvmName("fsub") val fsub get() = +InsnNode(Opcodes.FSUB)
    @get:JvmName("dsub") val dsub get() = +InsnNode(Opcodes.DSUB)
    @get:JvmName("imul") val imul get() = +InsnNode(Opcodes.IMUL)
    @get:JvmName("lmul") val lmul get() = +InsnNode(Opcodes.LMUL)
    @get:JvmName("fmul") val fmul get() = +InsnNode(Opcodes.FMUL)
    @get:JvmName("dmul") val dmul get() = +InsnNode(Opcodes.DMUL)
    @get:JvmName("idiv") val idiv get() = +InsnNode(Opcodes.IDIV)
    @get:JvmName("ldiv") val ldiv get() = +InsnNode(Opcodes.LDIV)
    @get:JvmName("fdiv") val fdiv get() = +InsnNode(Opcodes.FDIV)
    @get:JvmName("ddiv") val ddiv get() = +InsnNode(Opcodes.DDIV)
    @get:JvmName("irem") val irem get() = +InsnNode(Opcodes.IREM)
    @get:JvmName("lrem") val lrem get() = +InsnNode(Opcodes.LREM)
    @get:JvmName("frem") val frem get() = +InsnNode(Opcodes.FREM)
    @get:JvmName("drem") val drem get() = +InsnNode(Opcodes.DREM)
    @get:JvmName("ineg") val ineg get() = +InsnNode(Opcodes.INEG)
    @get:JvmName("lneg") val lneg get() = +InsnNode(Opcodes.LNEG)
    @get:JvmName("fneg") val fneg get() = +InsnNode(Opcodes.FNEG)
    @get:JvmName("dneg") val dneg get() = +InsnNode(Opcodes.DNEG)
    @get:JvmName("ishl") val ishl get() = +InsnNode(Opcodes.ISHL)
    @get:JvmName("lshl") val lshl get() = +InsnNode(Opcodes.LSHL)
    @get:JvmName("ishr") val ishr get() = +InsnNode(Opcodes.ISHR)
    @get:JvmName("lshr") val lshr get() = +InsnNode(Opcodes.LSHR)
    @get:JvmName("iushr") val iushr get() = +InsnNode(Opcodes.IUSHR)
    @get:JvmName("lushr") val lushr get() = +InsnNode(Opcodes.LUSHR)
    @get:JvmName("iand") val iand get() = +InsnNode(Opcodes.IAND)
    @get:JvmName("land") val land get() = +InsnNode(Opcodes.LAND)
    @get:JvmName("ior") val ior get() = +InsnNode(Opcodes.IOR)
    @get:JvmName("lor") val lor get() = +InsnNode(Opcodes.LOR)
    @get:JvmName("ixor") val ixor get() = +InsnNode(Opcodes.IXOR)
    @get:JvmName("lxor") val lxor get() = +InsnNode(Opcodes.LXOR)
    @get:JvmName("i2l") val i2l get() = +InsnNode(Opcodes.I2L)
    @get:JvmName("i2f") val i2f get() = +InsnNode(Opcodes.I2F)
    @get:JvmName("i2d") val i2d get() = +InsnNode(Opcodes.I2D)
    @get:JvmName("l2i") val l2i get() = +InsnNode(Opcodes.L2I)
    @get:JvmName("l2f") val l2f get() = +InsnNode(Opcodes.L2F)
    @get:JvmName("l2d") val l2d get() = +InsnNode(Opcodes.L2D)
    @get:JvmName("f2i") val f2i get() = +InsnNode(Opcodes.F2I)
    @get:JvmName("f2l") val f2l get() = +InsnNode(Opcodes.F2L)
    @get:JvmName("f2d") val f2d get() = +InsnNode(Opcodes.F2D)
    @get:JvmName("d2i") val d2i get() = +InsnNode(Opcodes.D2I)
    @get:JvmName("d2l") val d2l get() = +InsnNode(Opcodes.D2L)
    @get:JvmName("d2f") val d2f get() = +InsnNode(Opcodes.D2F)
    @get:JvmName("i2b") val i2b get() = +InsnNode(Opcodes.I2B)
    @get:JvmName("i2c") val i2c get() = +InsnNode(Opcodes.I2C)
    @get:JvmName("i2s") val i2s get() = +InsnNode(Opcodes.I2S)
    @get:JvmName("lcmp") val lcmp get() = +InsnNode(Opcodes.LCMP)
    @get:JvmName("fcmpl") val fcmpl get() = +InsnNode(Opcodes.FCMPL)
    @get:JvmName("fcmpg") val fcmpg get() = +InsnNode(Opcodes.FCMPG)
    @get:JvmName("dcmpl") val dcmpl get() = +InsnNode(Opcodes.DCMPL)
    @get:JvmName("dcmpg") val dcmpg get() = +InsnNode(Opcodes.DCMPG)
    @get:JvmName("ireturn") val ireturn get() = +InsnNode(Opcodes.IRETURN)
    @get:JvmName("lreturn") val lreturn get() = +InsnNode(Opcodes.LRETURN)
    @get:JvmName("freturn") val freturn get() = +InsnNode(Opcodes.FRETURN)
    @get:JvmName("dreturn") val dreturn get() = +InsnNode(Opcodes.DRETURN)
    @get:JvmName("areturn") val areturn get() = +InsnNode(Opcodes.ARETURN)
    @get:JvmName("_return") val _return get() = +InsnNode(Opcodes.RETURN)
    @get:JvmName("arraylength") val arraylength get() = +InsnNode(Opcodes.ARRAYLENGTH)
    @get:JvmName("athrow") val athrow get() = +InsnNode(Opcodes.ATHROW)
    @get:JvmName("monitorenter") val monitorenter get() = +InsnNode(Opcodes.MONITORENTER)
    @get:JvmName("monitorexit") val monitorexit get() = +InsnNode(Opcodes.MONITOREXIT)

    fun bipush(n: Int) = +IntInsnNode(Opcodes.BIPUSH, n)
    fun sipush(n: Int) = +IntInsnNode(Opcodes.SIPUSH, n)
    fun newarray(type: Int) = +IntInsnNode(Opcodes.NEWARRAY, type)

    fun ldc(cst: Any) = +LdcInsnNode(cst)

    fun ifeq(label: LabelNode) = +JumpInsnNode(Opcodes.IFEQ, label)
    fun ifne(label: LabelNode) = +JumpInsnNode(Opcodes.IFNE, label)
    fun iflt(label: LabelNode) = +JumpInsnNode(Opcodes.IFLT, label)
    fun ifge(label: LabelNode) = +JumpInsnNode(Opcodes.IFGE, label)
    fun ifgt(label: LabelNode) = +JumpInsnNode(Opcodes.IFGT, label)
    fun ifle(label: LabelNode) = +JumpInsnNode(Opcodes.IFLE, label)
    fun if_icmpeq(label: LabelNode) = +JumpInsnNode(Opcodes.IF_ICMPEQ, label)
    fun if_icmpne(label: LabelNode) = +JumpInsnNode(Opcodes.IF_ICMPNE, label)
    fun if_icmplt(label: LabelNode) = +JumpInsnNode(Opcodes.IF_ICMPLT, label)
    fun if_icmpge(label: LabelNode) = +JumpInsnNode(Opcodes.IF_ICMPGE, label)
    fun if_icmpgt(label: LabelNode) = +JumpInsnNode(Opcodes.IF_ICMPGT, label)
    fun if_icmple(label: LabelNode) = +JumpInsnNode(Opcodes.IF_ICMPLE, label)
    fun if_acmpeq(label: LabelNode) = +JumpInsnNode(Opcodes.IF_ACMPEQ, label)
    fun if_acmpne(label: LabelNode) = +JumpInsnNode(Opcodes.IF_ACMPNE, label)
    fun goto(label: LabelNode) = +JumpInsnNode(Opcodes.GOTO, label)
    fun ifnull(label: LabelNode) = +JumpInsnNode(Opcodes.IFNULL, label)
    fun ifnonnull(label: LabelNode) = +JumpInsnNode(Opcodes.IFNONNULL, label)

    fun invokedynamic(name: String, desc: String, bsm: Handle, vararg bsmArgs: Any) =
        +InvokeDynamicInsnNode(name, desc, bsm, bsmArgs)

    fun invokevirtual(owner: String, name: String, desc: String) =
        +MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner, name, desc, false)

    fun invokespecial(owner: String, name: String, desc: String) =
        +MethodInsnNode(Opcodes.INVOKESPECIAL, owner, name, desc, false)

    fun invokestatic(owner: String, name: String, desc: String) =
        +MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, desc, false)

    fun invokeinterface(owner: String, name: String, desc: String) =
        +MethodInsnNode(Opcodes.INVOKEINTERFACE, owner, name, desc, true)

    fun new(type: String) = +TypeInsnNode(Opcodes.NEW, type)
    fun anewarray(type: String) = +TypeInsnNode(Opcodes.ANEWARRAY, type)
    fun checkcast(type: String) = +TypeInsnNode(Opcodes.CHECKCAST, type)
    fun instanceof(type: String) = +TypeInsnNode(Opcodes.INSTANCEOF, type)

    fun iload(`var`: Int) = +VarInsnNode(Opcodes.ILOAD, `var`)
    fun lload(`var`: Int) = +VarInsnNode(Opcodes.LLOAD, `var`)
    fun fload(`var`: Int) = +VarInsnNode(Opcodes.FLOAD, `var`)
    fun dload(`var`: Int) = +VarInsnNode(Opcodes.DLOAD, `var`)
    fun aload(`var`: Int) = +VarInsnNode(Opcodes.ALOAD, `var`)
    fun istore(`var`: Int) = +VarInsnNode(Opcodes.ISTORE, `var`)
    fun lstore(`var`: Int) = +VarInsnNode(Opcodes.LSTORE, `var`)
    fun fstore(`var`: Int) = +VarInsnNode(Opcodes.FSTORE, `var`)
    fun dstore(`var`: Int) = +VarInsnNode(Opcodes.DSTORE, `var`)
    fun astore(`var`: Int) = +VarInsnNode(Opcodes.ASTORE, `var`)

    fun f_new(numLocal: Int, local: Array<Any>?, numStack: Int, stack: Array<Any>?) =
        +FrameNode(Opcodes.F_NEW, numLocal, local, numStack, stack)

    fun f_full(numLocal: Int, local: Array<Any>?, numStack: Int, stack: Array<Any>?) =
        +FrameNode(Opcodes.F_FULL, numLocal, local, numStack, stack)

    fun f_append(numLocal: Int, local: Array<Any>) =
        +FrameNode(Opcodes.F_APPEND, numLocal, local, 0, null)

    fun f_chop(numLocal: Int) =
        +FrameNode(Opcodes.F_CHOP, numLocal, null, 0, null)

    fun f_same() =
        +FrameNode(Opcodes.F_SAME, 0, null, 0, null)

    fun f_same1(stack: Any) =
        +FrameNode(Opcodes.F_SAME1, 0, null, 1, arrayOf(stack))

    fun int(n: Int) = when (n) {
        in -1..5 -> +InsnNode(Opcodes.ICONST_0 + n)
        in Byte.MIN_VALUE..Byte.MAX_VALUE -> bipush(n)
        in Short.MIN_VALUE..Short.MAX_VALUE -> sipush(n)
        else -> ldc(n)
    }
}

private class InsnListBuilder : InsnBuilder() {

    val list = InsnList()
    override fun AbstractInsnNode.unaryPlus() = list.add(this)
    override fun InsnList.unaryPlus() = list.add(this)
}

private class VisitorInsnBuilder(private val parent: MethodVisitor) : InsnBuilder() {

    override fun AbstractInsnNode.unaryPlus() = accept(parent)
    override fun InsnList.unaryPlus() = accept(parent)
}

public fun asm(block: InsnBuilder.() -> Unit): InsnList =
    InsnListBuilder().apply(block).list

public fun MethodVisitor.visitAsm(block: InsnBuilder.() -> Unit) {
    VisitorInsnBuilder(this).run(block)
}

public fun List<MethodNode>.named(name: String) = find { it.name == name }!!
public fun List<MethodNode>.search(name: String, desc: String) = find { it.name == name && it.desc == desc }!!
public fun List<MethodNode>.search(name: String, returnType: String, vararg args: String) = find { it.name == name && it.desc == "(${args.joinToString("")})$returnType" }!!
public fun List<FieldNode>.named(name: String) = find { it.name == name }!!
public fun List<FieldNode>.search(name: String, type: String) = find { it.name == name && it.desc == type }!!

public inline fun <reified T : Any> internalNameOf(): String = Type.getInternalName(T::class.java)

public inline fun <reified T : AbstractInsnNode> AbstractInsnNode.next(p: (T) -> Boolean = { true }): T? {
    return generateSequence(next) { it.next }.filterIsInstance<T>().find(p)
}

public inline fun <reified T : AbstractInsnNode> AbstractInsnNode.prev(p: (T) -> Boolean = { true }): T? {
    return generateSequence(previous) { it.previous }.filterIsInstance<T>().find(p)
}

public fun ClassNode.dump(file: String) {
    val cw = ClassWriter(0)
    accept(cw)
    FileOutputStream(file).use { it.write(cw.toByteArray()) }
}

public inline fun <reified T : Any> InsnBuilder.getSingleton() =
    getstatic(internalNameOf<T>(), "INSTANCE", "L${internalNameOf<T>()};")

public fun InsnBuilder.callEvent() {
    invokestatic(
        internalNameOf<EventBus>(),
        "callEvent",
        "(L${internalNameOf<Event>()};)V"
    )
}

public fun InsnBuilder.println() {
    getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
    swap
    invokevirtual("java/io/PrintStream", "println", "(Ljava/lang/Object;)V")
}
