package club.maxstats.weave.loader.util

import org.objectweb.asm.Handle
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

@Suppress("unused", "SpellCheckingInspection", "PropertyName", "FunctionName", "MemberVisibilityCanBePrivate")
abstract class InsnBuilder {

    abstract operator fun AbstractInsnNode.unaryPlus()
    abstract operator fun InsnList.unaryPlus()

    fun getstatic(owner: String, name: String, desc: String) = +FieldInsnNode(Opcodes.GETSTATIC, owner, name, desc)
    fun putstatic(owner: String, name: String, desc: String) = +FieldInsnNode(Opcodes.PUTSTATIC, owner, name, desc)
    fun getfield(owner: String, name: String, desc: String) = +FieldInsnNode(Opcodes.GETFIELD, owner, name, desc)
    fun putfield(owner: String, name: String, desc: String) = +FieldInsnNode(Opcodes.PUTFIELD, owner, name, desc)

    fun iinc(`var`: Int, incr: Int) = +IincInsnNode(`var`, incr)

    val nop get() = +InsnNode(Opcodes.NOP)
    val aconst_null get() = +InsnNode(Opcodes.ACONST_NULL)
    val iconst_m1 get() = +InsnNode(Opcodes.ICONST_M1)
    val iconst_0 get() = +InsnNode(Opcodes.ICONST_0)
    val iconst_1 get() = +InsnNode(Opcodes.ICONST_1)
    val iconst_2 get() = +InsnNode(Opcodes.ICONST_2)
    val iconst_3 get() = +InsnNode(Opcodes.ICONST_3)
    val iconst_4 get() = +InsnNode(Opcodes.ICONST_4)
    val iconst_5 get() = +InsnNode(Opcodes.ICONST_5)
    val lconst_0 get() = +InsnNode(Opcodes.LCONST_0)
    val lconst_1 get() = +InsnNode(Opcodes.LCONST_1)
    val fconst_0 get() = +InsnNode(Opcodes.FCONST_0)
    val fconst_1 get() = +InsnNode(Opcodes.FCONST_1)
    val fconst_2 get() = +InsnNode(Opcodes.FCONST_2)
    val dconst_0 get() = +InsnNode(Opcodes.DCONST_0)
    val dconst_1 get() = +InsnNode(Opcodes.DCONST_1)
    val iaload get() = +InsnNode(Opcodes.IALOAD)
    val laload get() = +InsnNode(Opcodes.LALOAD)
    val faload get() = +InsnNode(Opcodes.FALOAD)
    val daload get() = +InsnNode(Opcodes.DALOAD)
    val aaload get() = +InsnNode(Opcodes.AALOAD)
    val baload get() = +InsnNode(Opcodes.BALOAD)
    val caload get() = +InsnNode(Opcodes.CALOAD)
    val saload get() = +InsnNode(Opcodes.SALOAD)
    val iastore get() = +InsnNode(Opcodes.IASTORE)
    val lastore get() = +InsnNode(Opcodes.LASTORE)
    val fastore get() = +InsnNode(Opcodes.FASTORE)
    val dastore get() = +InsnNode(Opcodes.DASTORE)
    val aastore get() = +InsnNode(Opcodes.AASTORE)
    val bastore get() = +InsnNode(Opcodes.BASTORE)
    val castore get() = +InsnNode(Opcodes.CASTORE)
    val sastore get() = +InsnNode(Opcodes.SASTORE)
    val pop get() = +InsnNode(Opcodes.POP)
    val pop2 get() = +InsnNode(Opcodes.POP2)
    val dup get() = +InsnNode(Opcodes.DUP)
    val dup_x1 get() = +InsnNode(Opcodes.DUP_X1)
    val dup_x2 get() = +InsnNode(Opcodes.DUP_X2)
    val dup2 get() = +InsnNode(Opcodes.DUP2)
    val dup2_x1 get() = +InsnNode(Opcodes.DUP2_X1)
    val dup2_x2 get() = +InsnNode(Opcodes.DUP2_X2)
    val swap get() = +InsnNode(Opcodes.SWAP)
    val iadd get() = +InsnNode(Opcodes.IADD)
    val ladd get() = +InsnNode(Opcodes.LADD)
    val fadd get() = +InsnNode(Opcodes.FADD)
    val dadd get() = +InsnNode(Opcodes.DADD)
    val isub get() = +InsnNode(Opcodes.ISUB)
    val lsub get() = +InsnNode(Opcodes.LSUB)
    val fsub get() = +InsnNode(Opcodes.FSUB)
    val dsub get() = +InsnNode(Opcodes.DSUB)
    val imul get() = +InsnNode(Opcodes.IMUL)
    val lmul get() = +InsnNode(Opcodes.LMUL)
    val fmul get() = +InsnNode(Opcodes.FMUL)
    val dmul get() = +InsnNode(Opcodes.DMUL)
    val idiv get() = +InsnNode(Opcodes.IDIV)
    val ldiv get() = +InsnNode(Opcodes.LDIV)
    val fdiv get() = +InsnNode(Opcodes.FDIV)
    val ddiv get() = +InsnNode(Opcodes.DDIV)
    val irem get() = +InsnNode(Opcodes.IREM)
    val lrem get() = +InsnNode(Opcodes.LREM)
    val frem get() = +InsnNode(Opcodes.FREM)
    val drem get() = +InsnNode(Opcodes.DREM)
    val ineg get() = +InsnNode(Opcodes.INEG)
    val lneg get() = +InsnNode(Opcodes.LNEG)
    val fneg get() = +InsnNode(Opcodes.FNEG)
    val dneg get() = +InsnNode(Opcodes.DNEG)
    val ishl get() = +InsnNode(Opcodes.ISHL)
    val lshl get() = +InsnNode(Opcodes.LSHL)
    val ishr get() = +InsnNode(Opcodes.ISHR)
    val lshr get() = +InsnNode(Opcodes.LSHR)
    val iushr get() = +InsnNode(Opcodes.IUSHR)
    val lushr get() = +InsnNode(Opcodes.LUSHR)
    val iand get() = +InsnNode(Opcodes.IAND)
    val land get() = +InsnNode(Opcodes.LAND)
    val ior get() = +InsnNode(Opcodes.IOR)
    val lor get() = +InsnNode(Opcodes.LOR)
    val ixor get() = +InsnNode(Opcodes.IXOR)
    val lxor get() = +InsnNode(Opcodes.LXOR)
    val i2l get() = +InsnNode(Opcodes.I2L)
    val i2f get() = +InsnNode(Opcodes.I2F)
    val i2d get() = +InsnNode(Opcodes.I2D)
    val l2i get() = +InsnNode(Opcodes.L2I)
    val l2f get() = +InsnNode(Opcodes.L2F)
    val l2d get() = +InsnNode(Opcodes.L2D)
    val f2i get() = +InsnNode(Opcodes.F2I)
    val f2l get() = +InsnNode(Opcodes.F2L)
    val f2d get() = +InsnNode(Opcodes.F2D)
    val d2i get() = +InsnNode(Opcodes.D2I)
    val d2l get() = +InsnNode(Opcodes.D2L)
    val d2f get() = +InsnNode(Opcodes.D2F)
    val i2b get() = +InsnNode(Opcodes.I2B)
    val i2c get() = +InsnNode(Opcodes.I2C)
    val i2s get() = +InsnNode(Opcodes.I2S)
    val lcmp get() = +InsnNode(Opcodes.LCMP)
    val fcmpl get() = +InsnNode(Opcodes.FCMPL)
    val fcmpg get() = +InsnNode(Opcodes.FCMPG)
    val dcmpl get() = +InsnNode(Opcodes.DCMPL)
    val dcmpg get() = +InsnNode(Opcodes.DCMPG)
    val ireturn get() = +InsnNode(Opcodes.IRETURN)
    val lreturn get() = +InsnNode(Opcodes.LRETURN)
    val freturn get() = +InsnNode(Opcodes.FRETURN)
    val dreturn get() = +InsnNode(Opcodes.DRETURN)
    val areturn get() = +InsnNode(Opcodes.ARETURN)
    val _return get() = +InsnNode(Opcodes.RETURN)
    val arraylength get() = +InsnNode(Opcodes.ARRAYLENGTH)
    val athrow get() = +InsnNode(Opcodes.ATHROW)
    val monitorenter get() = +InsnNode(Opcodes.MONITORENTER)
    val monitorexit get() = +InsnNode(Opcodes.MONITOREXIT)

    fun bipush(n: Int) = +IntInsnNode(Opcodes.BIPUSH, n)
    fun sipush(n: Int) = +IntInsnNode(Opcodes.SIPUSH, n)
    fun newarray(type: Int) = +IntInsnNode(Opcodes.NEWARRAY, type)

    fun invokedynamic(name: String, desc: String, bsm: Handle, vararg bsmArgs: Any) =
        +InvokeDynamicInsnNode(name, desc, bsm, bsmArgs)

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

    fun ldc(cst: Any) = +LdcInsnNode(cst)

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

fun asm(f: InsnBuilder.() -> Unit): InsnList {
    val list = InsnList()

    object : InsnBuilder() {
        override fun AbstractInsnNode.unaryPlus() {
            list.add(this)
        }

        override fun InsnList.unaryPlus() {
            list.add(this)
        }
    }.f()

    return list
}

fun MethodVisitor.visitAsm(f: InsnBuilder.() -> Unit) {
    object : InsnBuilder() {
        override fun AbstractInsnNode.unaryPlus() {
            this.accept(this@visitAsm)
        }

        override fun InsnList.unaryPlus() {
            this.accept(this@visitAsm)
        }
    }.f()
}