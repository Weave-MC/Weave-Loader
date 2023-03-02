package club.maxstats.weave.loader.util

import org.objectweb.asm.Handle
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

@Suppress("PropertyName", "unused", "FunctionName", "SpellCheckingInspection")
sealed interface InsnBuilder {
    operator fun AbstractInsnNode.unaryPlus()
    operator fun InsnList.unaryPlus()

    fun getstatic(owner: String, name: String, desc: String) = +FieldInsnNode(GETSTATIC, owner, name, desc)
    fun putstatic(owner: String, name: String, desc: String) = +FieldInsnNode(PUTSTATIC, owner, name, desc)
    fun getfield(owner: String, name: String, desc: String) = +FieldInsnNode(GETFIELD, owner, name, desc)
    fun putfield(owner: String, name: String, desc: String) = +FieldInsnNode(PUTFIELD, owner, name, desc)

    fun iinc(`var`: Int, incr: Int) = +IincInsnNode(`var`, incr)

    val nop get() = +InsnNode(NOP)
    val aconst_null get() = +InsnNode(ACONST_NULL)
    val iconst_m1 get() = +InsnNode(ICONST_M1)
    val iconst_0 get() = +InsnNode(ICONST_0)
    val iconst_1 get() = +InsnNode(ICONST_1)
    val iconst_2 get() = +InsnNode(ICONST_2)
    val iconst_3 get() = +InsnNode(ICONST_3)
    val iconst_4 get() = +InsnNode(ICONST_4)
    val iconst_5 get() = +InsnNode(ICONST_5)
    val lconst_0 get() = +InsnNode(LCONST_0)
    val lconst_1 get() = +InsnNode(LCONST_1)
    val fconst_0 get() = +InsnNode(FCONST_0)
    val fconst_1 get() = +InsnNode(FCONST_1)
    val fconst_2 get() = +InsnNode(FCONST_2)
    val dconst_0 get() = +InsnNode(DCONST_0)
    val dconst_1 get() = +InsnNode(DCONST_1)
    val iaload get() = +InsnNode(IALOAD)
    val laload get() = +InsnNode(LALOAD)
    val faload get() = +InsnNode(FALOAD)
    val daload get() = +InsnNode(DALOAD)
    val aaload get() = +InsnNode(AALOAD)
    val baload get() = +InsnNode(BALOAD)
    val caload get() = +InsnNode(CALOAD)
    val saload get() = +InsnNode(SALOAD)
    val iastore get() = +InsnNode(IASTORE)
    val lastore get() = +InsnNode(LASTORE)
    val fastore get() = +InsnNode(FASTORE)
    val dastore get() = +InsnNode(DASTORE)
    val aastore get() = +InsnNode(AASTORE)
    val bastore get() = +InsnNode(BASTORE)
    val castore get() = +InsnNode(CASTORE)
    val sastore get() = +InsnNode(SASTORE)
    val pop get() = +InsnNode(POP)
    val pop2 get() = +InsnNode(POP2)
    val dup get() = +InsnNode(DUP)
    val dup_x1 get() = +InsnNode(DUP_X1)
    val dup_x2 get() = +InsnNode(DUP_X2)
    val dup2 get() = +InsnNode(DUP2)
    val dup2_x1 get() = +InsnNode(DUP2_X1)
    val dup2_x2 get() = +InsnNode(DUP2_X2)
    val swap get() = +InsnNode(SWAP)
    val iadd get() = +InsnNode(IADD)
    val ladd get() = +InsnNode(LADD)
    val fadd get() = +InsnNode(FADD)
    val dadd get() = +InsnNode(DADD)
    val isub get() = +InsnNode(ISUB)
    val lsub get() = +InsnNode(LSUB)
    val fsub get() = +InsnNode(FSUB)
    val dsub get() = +InsnNode(DSUB)
    val imul get() = +InsnNode(IMUL)
    val lmul get() = +InsnNode(LMUL)
    val fmul get() = +InsnNode(FMUL)
    val dmul get() = +InsnNode(DMUL)
    val idiv get() = +InsnNode(IDIV)
    val ldiv get() = +InsnNode(LDIV)
    val fdiv get() = +InsnNode(FDIV)
    val ddiv get() = +InsnNode(DDIV)
    val irem get() = +InsnNode(IREM)
    val lrem get() = +InsnNode(LREM)
    val frem get() = +InsnNode(FREM)
    val drem get() = +InsnNode(DREM)
    val ineg get() = +InsnNode(INEG)
    val lneg get() = +InsnNode(LNEG)
    val fneg get() = +InsnNode(FNEG)
    val dneg get() = +InsnNode(DNEG)
    val ishl get() = +InsnNode(ISHL)
    val lshl get() = +InsnNode(LSHL)
    val ishr get() = +InsnNode(ISHR)
    val lshr get() = +InsnNode(LSHR)
    val iushr get() = +InsnNode(IUSHR)
    val lushr get() = +InsnNode(LUSHR)
    val iand get() = +InsnNode(IAND)
    val land get() = +InsnNode(LAND)
    val ior get() = +InsnNode(IOR)
    val lor get() = +InsnNode(LOR)
    val ixor get() = +InsnNode(IXOR)
    val lxor get() = +InsnNode(LXOR)
    val i2l get() = +InsnNode(I2L)
    val i2f get() = +InsnNode(I2F)
    val i2d get() = +InsnNode(I2D)
    val l2i get() = +InsnNode(L2I)
    val l2f get() = +InsnNode(L2F)
    val l2d get() = +InsnNode(L2D)
    val f2i get() = +InsnNode(F2I)
    val f2l get() = +InsnNode(F2L)
    val f2d get() = +InsnNode(F2D)
    val d2i get() = +InsnNode(D2I)
    val d2l get() = +InsnNode(D2L)
    val d2f get() = +InsnNode(D2F)
    val i2b get() = +InsnNode(I2B)
    val i2c get() = +InsnNode(I2C)
    val i2s get() = +InsnNode(I2S)
    val lcmp get() = +InsnNode(LCMP)
    val fcmpl get() = +InsnNode(FCMPL)
    val fcmpg get() = +InsnNode(FCMPG)
    val dcmpl get() = +InsnNode(DCMPL)
    val dcmpg get() = +InsnNode(DCMPG)
    val ireturn get() = +InsnNode(IRETURN)
    val lreturn get() = +InsnNode(LRETURN)
    val freturn get() = +InsnNode(FRETURN)
    val dreturn get() = +InsnNode(DRETURN)
    val areturn get() = +InsnNode(ARETURN)
    val _return get() = +InsnNode(RETURN)
    val arraylength get() = +InsnNode(ARRAYLENGTH)
    val athrow get() = +InsnNode(ATHROW)
    val monitorenter get() = +InsnNode(MONITORENTER)
    val monitorexit get() = +InsnNode(MONITOREXIT)

    fun bipush(n: Int) = +IntInsnNode(BIPUSH, n)
    fun sipush(n: Int) = +IntInsnNode(SIPUSH, n)
    fun newarray(type: Int) = +IntInsnNode(NEWARRAY, type)

    fun invokedynamic(name: String, desc: String, bsm: Handle, vararg bsmArgs: Any) =
        +InvokeDynamicInsnNode(name, desc, bsm, bsmArgs)

    fun ifeq(label: LabelNode) = +JumpInsnNode(IFEQ, label)
    fun ifne(label: LabelNode) = +JumpInsnNode(IFNE, label)
    fun iflt(label: LabelNode) = +JumpInsnNode(IFLT, label)
    fun ifge(label: LabelNode) = +JumpInsnNode(IFGE, label)
    fun ifgt(label: LabelNode) = +JumpInsnNode(IFGT, label)
    fun ifle(label: LabelNode) = +JumpInsnNode(IFLE, label)
    fun if_icmpeq(label: LabelNode) = +JumpInsnNode(IF_ICMPEQ, label)
    fun if_icmpne(label: LabelNode) = +JumpInsnNode(IF_ICMPNE, label)
    fun if_icmplt(label: LabelNode) = +JumpInsnNode(IF_ICMPLT, label)
    fun if_icmpge(label: LabelNode) = +JumpInsnNode(IF_ICMPGE, label)
    fun if_icmpgt(label: LabelNode) = +JumpInsnNode(IF_ICMPGT, label)
    fun if_icmple(label: LabelNode) = +JumpInsnNode(IF_ICMPLE, label)
    fun if_acmpeq(label: LabelNode) = +JumpInsnNode(IF_ACMPEQ, label)
    fun if_acmpne(label: LabelNode) = +JumpInsnNode(IF_ACMPNE, label)
    fun goto(label: LabelNode) = +JumpInsnNode(GOTO, label)
    fun ifnull(label: LabelNode) = +JumpInsnNode(IFNULL, label)
    fun ifnonnull(label: LabelNode) = +JumpInsnNode(IFNONNULL, label)

    fun ldc(cst: Any) = +LdcInsnNode(cst)

    fun invokevirtual(owner: String, name: String, desc: String) =
        +MethodInsnNode(INVOKEVIRTUAL, owner, name, desc, false)

    fun invokespecial(owner: String, name: String, desc: String) =
        +MethodInsnNode(INVOKESPECIAL, owner, name, desc, false)

    fun invokestatic(owner: String, name: String, desc: String) =
        +MethodInsnNode(INVOKESTATIC, owner, name, desc, false)

    fun invokeinterface(owner: String, name: String, desc: String) =
        +MethodInsnNode(INVOKEINTERFACE, owner, name, desc, true)

    fun new(type: String) = +TypeInsnNode(NEW, type)
    fun anewarray(type: String) = +TypeInsnNode(ANEWARRAY, type)
    fun checkcast(type: String) = +TypeInsnNode(CHECKCAST, type)
    fun instanceof(type: String) = +TypeInsnNode(INSTANCEOF, type)

    fun iload(`var`: Int) = +VarInsnNode(ILOAD, `var`)
    fun lload(`var`: Int) = +VarInsnNode(LLOAD, `var`)
    fun fload(`var`: Int) = +VarInsnNode(FLOAD, `var`)
    fun dload(`var`: Int) = +VarInsnNode(DLOAD, `var`)
    fun aload(`var`: Int) = +VarInsnNode(ALOAD, `var`)
    fun istore(`var`: Int) = +VarInsnNode(ISTORE, `var`)
    fun lstore(`var`: Int) = +VarInsnNode(LSTORE, `var`)
    fun fstore(`var`: Int) = +VarInsnNode(FSTORE, `var`)
    fun dstore(`var`: Int) = +VarInsnNode(DSTORE, `var`)
    fun astore(`var`: Int) = +VarInsnNode(ASTORE, `var`)

    fun f_new(numLocal: Int, local: Array<Any>?, numStack: Int, stack: Array<Any>?) =
        +FrameNode(F_NEW, numLocal, local, numStack, stack)

    fun f_full(numLocal: Int, local: Array<Any>?, numStack: Int, stack: Array<Any>?) =
        +FrameNode(F_FULL, numLocal, local, numStack, stack)

    fun f_append(numLocal: Int, local: Array<Any>) =
        +FrameNode(F_APPEND, numLocal, local, 0, null)

    fun f_chop(numLocal: Int) =
        +FrameNode(F_CHOP, numLocal, null, 0, null)

    fun f_same() =
        +FrameNode(F_SAME, 0, null, 0, null)

    fun f_same1(stack: Any) =
        +FrameNode(F_SAME1, 0, null, 1, arrayOf(stack))

    fun int(n: Int) = when (n) {
        in -1..5 -> +InsnNode(ICONST_0 + n)
        in Byte.MIN_VALUE..Byte.MAX_VALUE -> bipush(n)
        in Short.MIN_VALUE..Short.MAX_VALUE -> sipush(n)
        else -> ldc(n)
    }
}

private class InsnListBuilder : InsnBuilder {
    val list = InsnList()
    override fun AbstractInsnNode.unaryPlus() = list.add(this)
    override fun InsnList.unaryPlus() = list.add(this)
}

private class VisitorInsnBuilder(private val parent: MethodVisitor) : InsnBuilder {
    override fun AbstractInsnNode.unaryPlus() = accept(parent)
    override fun InsnList.unaryPlus() = accept(parent)
}

fun asm(block: InsnBuilder.() -> Unit) = InsnListBuilder().apply(block).list
fun MethodVisitor.visitAsm(block: InsnBuilder.() -> Unit) = VisitorInsnBuilder(this).run(block)

inline fun <reified T : Any> InsnBuilder.getSingleton() =
    getstatic(internalNameOf<T>(), "INSTANCE", "L${internalNameOf<T>()};")