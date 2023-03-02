package club.maxstats.weave.loader.util

import org.objectweb.asm.Type
import org.objectweb.asm.Handle
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaMethod

/**
 * Allows the creation of [AbstractInsnNode]s using a DSL.
 * They can be called similarly to the following
 *
 *     MethodNode.instructions.insert(asm {
 *         getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
 *         ldc("Hello World!")
 *         invokevirtual("java/io/PrintStream", "println", "(Ljava/lang/String;)V", false)
 *         return
 *     })
 *
 * @author Nils
 */
@Suppress("unused", "PropertyName", "FunctionName", "INAPPLICABLE_JVM_NAME")
sealed interface InsnBuilder {
    operator fun AbstractInsnNode.unaryPlus()
    operator fun InsnList.unaryPlus()

    fun getstatic(owner: String, name: String, desc: String)  = +FieldInsnNode(GETSTATIC, owner, name, desc)
    fun putstatic(owner: String, name: String, desc: String)  = +FieldInsnNode(PUTSTATIC, owner, name, desc)
    fun getfield(owner: String, name: String, desc: String)   = +FieldInsnNode(GETFIELD, owner, name, desc)
    fun putfield(owner: String, name: String, desc: String)   = +FieldInsnNode(PUTFIELD, owner, name, desc)

    fun iinc(`var`: Int, incr: Int)                           = +IincInsnNode(`var`, incr)

    @get:JvmName("nop") val nop get()                   = +InsnNode(NOP)
    @get:JvmName("aconst_null") val aconst_null get()   = +InsnNode(ACONST_NULL)
    @get:JvmName("iconst_m1") val iconst_m1 get()       = +InsnNode(ICONST_M1)
    @get:JvmName("iconst_0") val iconst_0 get()         = +InsnNode(ICONST_0)
    @get:JvmName("iconst_1") val iconst_1 get()         = +InsnNode(ICONST_1)
    @get:JvmName("iconst_2") val iconst_2 get()         = +InsnNode(ICONST_2)
    @get:JvmName("iconst_3") val iconst_3 get()         = +InsnNode(ICONST_3)
    @get:JvmName("iconst_4") val iconst_4 get()         = +InsnNode(ICONST_4)
    @get:JvmName("iconst_5") val iconst_5 get()         = +InsnNode(ICONST_5)
    @get:JvmName("lconst_0") val lconst_0 get()         = +InsnNode(LCONST_0)
    @get:JvmName("lconst_1") val lconst_1 get()         = +InsnNode(LCONST_1)
    @get:JvmName("fconst_0") val fconst_0 get()         = +InsnNode(FCONST_0)
    @get:JvmName("fconst_1") val fconst_1 get()         = +InsnNode(FCONST_1)
    @get:JvmName("fconst_2") val fconst_2 get()         = +InsnNode(FCONST_2)
    @get:JvmName("dconst_0") val dconst_0 get()         = +InsnNode(DCONST_0)
    @get:JvmName("dconst_1") val dconst_1 get()         = +InsnNode(DCONST_1)
    @get:JvmName("iaload") val iaload get()             = +InsnNode(IALOAD)
    @get:JvmName("laload") val laload get()             = +InsnNode(LALOAD)
    @get:JvmName("faload") val faload get()             = +InsnNode(FALOAD)
    @get:JvmName("daload") val daload get()             = +InsnNode(DALOAD)
    @get:JvmName("aaload") val aaload get()             = +InsnNode(AALOAD)
    @get:JvmName("baload") val baload get()             = +InsnNode(BALOAD)
    @get:JvmName("caload") val caload get()             = +InsnNode(CALOAD)
    @get:JvmName("saload") val saload get()             = +InsnNode(SALOAD)
    @get:JvmName("iastore") val iastore get()           = +InsnNode(IASTORE)
    @get:JvmName("lastore") val lastore get()           = +InsnNode(LASTORE)
    @get:JvmName("fastore") val fastore get()           = +InsnNode(FASTORE)
    @get:JvmName("dastore") val dastore get()           = +InsnNode(DASTORE)
    @get:JvmName("aastore") val aastore get()           = +InsnNode(AASTORE)
    @get:JvmName("bastore") val bastore get()           = +InsnNode(BASTORE)
    @get:JvmName("castore") val castore get()           = +InsnNode(CASTORE)
    @get:JvmName("sastore") val sastore get()           = +InsnNode(SASTORE)
    @get:JvmName("pop") val pop get()                   = +InsnNode(POP)
    @get:JvmName("pop2") val pop2 get()                 = +InsnNode(POP2)
    @get:JvmName("dup") val dup get()                   = +InsnNode(DUP)
    @get:JvmName("dup_x1") val dup_x1 get()             = +InsnNode(DUP_X1)
    @get:JvmName("dup_x2") val dup_x2 get()             = +InsnNode(DUP_X2)
    @get:JvmName("dup2") val dup2 get()                 = +InsnNode(DUP2)
    @get:JvmName("dup2_x1") val dup2_x1 get()           = +InsnNode(DUP2_X1)
    @get:JvmName("dup2_x2") val dup2_x2 get()           = +InsnNode(DUP2_X2)
    @get:JvmName("swap") val swap get()                 = +InsnNode(SWAP)
    @get:JvmName("iadd") val iadd get()                 = +InsnNode(IADD)
    @get:JvmName("ladd") val ladd get()                 = +InsnNode(LADD)
    @get:JvmName("fadd") val fadd get()                 = +InsnNode(FADD)
    @get:JvmName("dadd") val dadd get()                 = +InsnNode(DADD)
    @get:JvmName("isub") val isub get()                 = +InsnNode(ISUB)
    @get:JvmName("lsub") val lsub get()                 = +InsnNode(LSUB)
    @get:JvmName("fsub") val fsub get()                 = +InsnNode(FSUB)
    @get:JvmName("dsub") val dsub get()                 = +InsnNode(DSUB)
    @get:JvmName("imul") val imul get()                 = +InsnNode(IMUL)
    @get:JvmName("lmul") val lmul get()                 = +InsnNode(LMUL)
    @get:JvmName("fmul") val fmul get()                 = +InsnNode(FMUL)
    @get:JvmName("dmul") val dmul get()                 = +InsnNode(DMUL)
    @get:JvmName("idiv") val idiv get()                 = +InsnNode(IDIV)
    @get:JvmName("ldiv") val ldiv get()                 = +InsnNode(LDIV)
    @get:JvmName("fdiv") val fdiv get()                 = +InsnNode(FDIV)
    @get:JvmName("ddiv") val ddiv get()                 = +InsnNode(DDIV)
    @get:JvmName("irem") val irem get()                 = +InsnNode(IREM)
    @get:JvmName("lrem") val lrem get()                 = +InsnNode(LREM)
    @get:JvmName("frem") val frem get()                 = +InsnNode(FREM)
    @get:JvmName("drem") val drem get()                 = +InsnNode(DREM)
    @get:JvmName("ineg") val ineg get()                 = +InsnNode(INEG)
    @get:JvmName("lneg") val lneg get()                 = +InsnNode(LNEG)
    @get:JvmName("fneg") val fneg get()                 = +InsnNode(FNEG)
    @get:JvmName("dneg") val dneg get()                 = +InsnNode(DNEG)
    @get:JvmName("ishl") val ishl get()                 = +InsnNode(ISHL)
    @get:JvmName("lshl") val lshl get()                 = +InsnNode(LSHL)
    @get:JvmName("ishr") val ishr get()                 = +InsnNode(ISHR)
    @get:JvmName("lshr") val lshr get()                 = +InsnNode(LSHR)
    @get:JvmName("iushr") val iushr get()               = +InsnNode(IUSHR)
    @get:JvmName("lushr") val lushr get()               = +InsnNode(LUSHR)
    @get:JvmName("iand") val iand get()                 = +InsnNode(IAND)
    @get:JvmName("land") val land get()                 = +InsnNode(LAND)
    @get:JvmName("ior") val ior get()                   = +InsnNode(IOR)
    @get:JvmName("lor") val lor get()                   = +InsnNode(LOR)
    @get:JvmName("ixor") val ixor get()                 = +InsnNode(IXOR)
    @get:JvmName("lxor") val lxor get()                 = +InsnNode(LXOR)
    @get:JvmName("i2l") val i2l get()                   = +InsnNode(I2L)
    @get:JvmName("i2f") val i2f get()                   = +InsnNode(I2F)
    @get:JvmName("i2d") val i2d get()                   = +InsnNode(I2D)
    @get:JvmName("l2i") val l2i get()                   = +InsnNode(L2I)
    @get:JvmName("l2f") val l2f get()                   = +InsnNode(L2F)
    @get:JvmName("l2d") val l2d get()                   = +InsnNode(L2D)
    @get:JvmName("f2i") val f2i get()                   = +InsnNode(F2I)
    @get:JvmName("f2l") val f2l get()                   = +InsnNode(F2L)
    @get:JvmName("f2d") val f2d get()                   = +InsnNode(F2D)
    @get:JvmName("d2i") val d2i get()                   = +InsnNode(D2I)
    @get:JvmName("d2l") val d2l get()                   = +InsnNode(D2L)
    @get:JvmName("d2f") val d2f get()                   = +InsnNode(D2F)
    @get:JvmName("i2b") val i2b get()                   = +InsnNode(I2B)
    @get:JvmName("i2c") val i2c get()                   = +InsnNode(I2C)
    @get:JvmName("i2s") val i2s get()                   = +InsnNode(I2S)
    @get:JvmName("lcmp") val lcmp get()                 = +InsnNode(LCMP)
    @get:JvmName("fcmpl") val fcmpl get()               = +InsnNode(FCMPL)
    @get:JvmName("fcmpg") val fcmpg get()               = +InsnNode(FCMPG)
    @get:JvmName("dcmpl") val dcmpl get()               = +InsnNode(DCMPL)
    @get:JvmName("dcmpg") val dcmpg get()               = +InsnNode(DCMPG)
    @get:JvmName("ireturn") val ireturn get()           = +InsnNode(IRETURN)
    @get:JvmName("lreturn") val lreturn get()           = +InsnNode(LRETURN)
    @get:JvmName("freturn") val freturn get()           = +InsnNode(FRETURN)
    @get:JvmName("dreturn") val dreturn get()           = +InsnNode(DRETURN)
    @get:JvmName("areturn") val areturn get()           = +InsnNode(ARETURN)
    @get:JvmName("_return") val _return get()           = +InsnNode(RETURN)
    @get:JvmName("arraylength") val arraylength get()   = +InsnNode(ARRAYLENGTH)
    @get:JvmName("athrow") val athrow get()             = +InsnNode(ATHROW)
    @get:JvmName("monitorenter") val monitorenter get() = +InsnNode(MONITORENTER)
    @get:JvmName("monitorexit") val monitorexit get()   = +InsnNode(MONITOREXIT)

    fun ldc(cst: Any)                                         = +LdcInsnNode(cst)

    fun bipush(n: Int)                                        = +IntInsnNode(BIPUSH, n)
    fun sipush(n: Int)                                        = +IntInsnNode(SIPUSH, n)
    fun newarray(type: Int)                                   = +IntInsnNode(NEWARRAY, type)

    fun ifeq(label: LabelNode)                                = +JumpInsnNode(IFEQ, label)
    fun ifne(label: LabelNode)                                = +JumpInsnNode(IFNE, label)
    fun iflt(label: LabelNode)                                = +JumpInsnNode(IFLT, label)
    fun ifge(label: LabelNode)                                = +JumpInsnNode(IFGE, label)
    fun ifgt(label: LabelNode)                                = +JumpInsnNode(IFGT, label)
    fun ifle(label: LabelNode)                                = +JumpInsnNode(IFLE, label)
    fun if_icmpeq(label: LabelNode)                           = +JumpInsnNode(IF_ICMPEQ, label)
    fun if_icmpne(label: LabelNode)                           = +JumpInsnNode(IF_ICMPNE, label)
    fun if_icmplt(label: LabelNode)                           = +JumpInsnNode(IF_ICMPLT, label)
    fun if_icmpge(label: LabelNode)                           = +JumpInsnNode(IF_ICMPGE, label)
    fun if_icmpgt(label: LabelNode)                           = +JumpInsnNode(IF_ICMPGT, label)
    fun if_icmple(label: LabelNode)                           = +JumpInsnNode(IF_ICMPLE, label)
    fun if_acmpeq(label: LabelNode)                           = +JumpInsnNode(IF_ACMPEQ, label)
    fun if_acmpne(label: LabelNode)                           = +JumpInsnNode(IF_ACMPNE, label)
    fun goto(label: LabelNode)                                = +JumpInsnNode(GOTO, label)
    fun ifnull(label: LabelNode)                              = +JumpInsnNode(IFNULL, label)
    fun ifnonnull(label: LabelNode)                           = +JumpInsnNode(IFNONNULL, label)

    fun invokedynamic(name: String, desc: String, bsm: Handle, vararg bsmArgs: Any) =
        +InvokeDynamicInsnNode(name, desc, bsm, bsmArgs)

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

    fun iload(`var`: Int)                                     = +VarInsnNode(ILOAD, `var`)
    fun lload(`var`: Int)                                     = +VarInsnNode(LLOAD, `var`)
    fun fload(`var`: Int)                                     = +VarInsnNode(FLOAD, `var`)
    fun dload(`var`: Int)                                     = +VarInsnNode(DLOAD, `var`)
    fun aload(`var`: Int)                                     = +VarInsnNode(ALOAD, `var`)
    fun istore(`var`: Int)                                    = +VarInsnNode(ISTORE, `var`)
    fun lstore(`var`: Int)                                    = +VarInsnNode(LSTORE, `var`)
    fun fstore(`var`: Int)                                    = +VarInsnNode(FSTORE, `var`)
    fun dstore(`var`: Int)                                    = +VarInsnNode(DSTORE, `var`)
    fun astore(`var`: Int)                                    = +VarInsnNode(ASTORE, `var`)

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

class InsnListBuilder : InsnBuilder {
    val list = InsnList()
    override fun AbstractInsnNode.unaryPlus() = list.add(this)
    override fun InsnList.unaryPlus() = list.add(this)
}

class VisitorInsnBuilder(private val parent: MethodVisitor) : InsnBuilder {
    override fun AbstractInsnNode.unaryPlus() = accept(parent)
    override fun InsnList.unaryPlus() = accept(parent)
}

fun asm(block: InsnBuilder.() -> Unit) = InsnListBuilder().apply(block).list
fun MethodVisitor.visitASM(block: InsnBuilder.() -> Unit) = VisitorInsnBuilder(this).apply(block)

fun InsnBuilder.invokeMethod(method: Method) = +MethodInsnNode(
    when {
        method.isPrivate -> INVOKESPECIAL
        method.declaringClass.isInterface -> INVOKEINTERFACE
        method.isStatic -> INVOKESTATIC
        else -> INVOKEVIRTUAL
    },
    method.declaringClass.name.replace('.', '/'),
    method.name,
    Type.getMethodDescriptor(method),
    method.declaringClass.isInterface
)

val KFunction<*>.java get() = javaMethod ?: error("No Java method available for function $this")
val KFunction<*>.javaCtor get() = javaConstructor ?: error("No Java constructor available for function $this")

fun InsnBuilder.invokeMethod(func: KFunction<*>) = invokeMethod(func.java)
inline fun <reified T : Any> InsnBuilder.getObject() =
    getstatic(internalNameOf<T>(), "INSTANCE", "L${internalNameOf<T>()};")

inline fun InsnBuilder.construct(constructor: Constructor<*>, init: InsnBuilder.() -> Unit) {
    val internalName = constructor.declaringClass.name.replace('.', '/')

    +TypeInsnNode(NEW, internalName)
    +InsnNode(DUP)
    init()
    invokespecial(internalName, "<init>", Type.getConstructorDescriptor(constructor))
}

inline fun InsnBuilder.construct(func: KFunction<*>, init: InsnBuilder.() -> Unit) = construct(func.javaCtor, init)