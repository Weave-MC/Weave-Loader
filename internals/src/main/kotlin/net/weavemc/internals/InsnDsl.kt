package net.weavemc.internals

import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory
import kotlin.io.path.writeBytes
import kotlin.reflect.KClass

@Suppress(
    "PropertyName",
    "unused",
    "FunctionName",
    "SpellCheckingInspection",
    "MemberVisibilityCanBePrivate",
)
public sealed class InsnBuilder {
    public abstract operator fun AbstractInsnNode.unaryPlus(): Unit
    public abstract operator fun InsnList.unaryPlus(): Unit

    public fun getstatic(owner: String, name: String, desc: String): Unit = +FieldInsnNode(Opcodes.GETSTATIC, owner, name, desc)
    public fun putstatic(owner: String, name: String, desc: String): Unit = +FieldInsnNode(Opcodes.PUTSTATIC, owner, name, desc)
    public fun getfield(owner: String, name: String, desc: String): Unit = +FieldInsnNode(Opcodes.GETFIELD, owner, name, desc)
    public fun putfield(owner: String, name: String, desc: String): Unit = +FieldInsnNode(Opcodes.PUTFIELD, owner, name, desc)

    public fun iinc(`var`: Int, incr: Int): Unit = +IincInsnNode(`var`, incr)

    @get:JvmName("nop") public val nop: Unit get() = +InsnNode(Opcodes.NOP)
    @get:JvmName("aconst_null") public val aconst_null: Unit get() = +InsnNode(Opcodes.ACONST_NULL)
    @get:JvmName("iconst_m1") public val iconst_m1: Unit get() = +InsnNode(Opcodes.ICONST_M1)
    @get:JvmName("iconst_0") public val iconst_0: Unit get() = +InsnNode(Opcodes.ICONST_0)
    @get:JvmName("iconst_1") public val iconst_1: Unit get() = +InsnNode(Opcodes.ICONST_1)
    @get:JvmName("iconst_2") public val iconst_2: Unit get() = +InsnNode(Opcodes.ICONST_2)
    @get:JvmName("iconst_3") public val iconst_3: Unit get() = +InsnNode(Opcodes.ICONST_3)
    @get:JvmName("iconst_4") public val iconst_4: Unit get() = +InsnNode(Opcodes.ICONST_4)
    @get:JvmName("iconst_5") public val iconst_5: Unit get() = +InsnNode(Opcodes.ICONST_5)
    @get:JvmName("lconst_0") public val lconst_0: Unit get() = +InsnNode(Opcodes.LCONST_0)
    @get:JvmName("lconst_1") public val lconst_1: Unit get() = +InsnNode(Opcodes.LCONST_1)
    @get:JvmName("fconst_0") public val fconst_0: Unit get() = +InsnNode(Opcodes.FCONST_0)
    @get:JvmName("fconst_1") public val fconst_1: Unit get() = +InsnNode(Opcodes.FCONST_1)
    @get:JvmName("fconst_2") public val fconst_2: Unit get() = +InsnNode(Opcodes.FCONST_2)
    @get:JvmName("dconst_0") public val dconst_0: Unit get() = +InsnNode(Opcodes.DCONST_0)
    @get:JvmName("dconst_1") public val dconst_1: Unit get() = +InsnNode(Opcodes.DCONST_1)
    @get:JvmName("iaload") public val iaload: Unit get() = +InsnNode(Opcodes.IALOAD)
    @get:JvmName("laload") public val laload: Unit get() = +InsnNode(Opcodes.LALOAD)
    @get:JvmName("faload") public val faload: Unit get() = +InsnNode(Opcodes.FALOAD)
    @get:JvmName("daload") public val daload: Unit get() = +InsnNode(Opcodes.DALOAD)
    @get:JvmName("aaload") public val aaload: Unit get() = +InsnNode(Opcodes.AALOAD)
    @get:JvmName("baload") public val baload: Unit get() = +InsnNode(Opcodes.BALOAD)
    @get:JvmName("caload") public val caload: Unit get() = +InsnNode(Opcodes.CALOAD)
    @get:JvmName("saload") public val saload: Unit get() = +InsnNode(Opcodes.SALOAD)
    @get:JvmName("iastore") public val iastore: Unit get() = +InsnNode(Opcodes.IASTORE)
    @get:JvmName("lastore") public val lastore: Unit get() = +InsnNode(Opcodes.LASTORE)
    @get:JvmName("fastore") public val fastore: Unit get() = +InsnNode(Opcodes.FASTORE)
    @get:JvmName("dastore") public val dastore: Unit get() = +InsnNode(Opcodes.DASTORE)
    @get:JvmName("aastore") public val aastore: Unit get() = +InsnNode(Opcodes.AASTORE)
    @get:JvmName("bastore") public val bastore: Unit get() = +InsnNode(Opcodes.BASTORE)
    @get:JvmName("castore") public val castore: Unit get() = +InsnNode(Opcodes.CASTORE)
    @get:JvmName("sastore") public val sastore: Unit get() = +InsnNode(Opcodes.SASTORE)
    @get:JvmName("pop") public val pop: Unit get() = +InsnNode(Opcodes.POP)
    @get:JvmName("pop2") public val pop2: Unit get() = +InsnNode(Opcodes.POP2)
    @get:JvmName("dup") public val dup: Unit get() = +InsnNode(Opcodes.DUP)
    @get:JvmName("dup_x1") public val dup_x1: Unit get() = +InsnNode(Opcodes.DUP_X1)
    @get:JvmName("dup_x2") public val dup_x2: Unit get() = +InsnNode(Opcodes.DUP_X2)
    @get:JvmName("dup2") public val dup2: Unit get() = +InsnNode(Opcodes.DUP2)
    @get:JvmName("dup2_x1") public val dup2_x1: Unit get() = +InsnNode(Opcodes.DUP2_X1)
    @get:JvmName("dup2_x2") public val dup2_x2: Unit get() = +InsnNode(Opcodes.DUP2_X2)
    @get:JvmName("swap") public val swap: Unit get() = +InsnNode(Opcodes.SWAP)
    @get:JvmName("iadd") public val iadd: Unit get() = +InsnNode(Opcodes.IADD)
    @get:JvmName("ladd") public val ladd: Unit get() = +InsnNode(Opcodes.LADD)
    @get:JvmName("fadd") public val fadd: Unit get() = +InsnNode(Opcodes.FADD)
    @get:JvmName("dadd") public val dadd: Unit get() = +InsnNode(Opcodes.DADD)
    @get:JvmName("isub") public val isub: Unit get() = +InsnNode(Opcodes.ISUB)
    @get:JvmName("lsub") public val lsub: Unit get() = +InsnNode(Opcodes.LSUB)
    @get:JvmName("fsub") public val fsub: Unit get() = +InsnNode(Opcodes.FSUB)
    @get:JvmName("dsub") public val dsub: Unit get() = +InsnNode(Opcodes.DSUB)
    @get:JvmName("imul") public val imul: Unit get() = +InsnNode(Opcodes.IMUL)
    @get:JvmName("lmul") public val lmul: Unit get() = +InsnNode(Opcodes.LMUL)
    @get:JvmName("fmul") public val fmul: Unit get() = +InsnNode(Opcodes.FMUL)
    @get:JvmName("dmul") public val dmul: Unit get() = +InsnNode(Opcodes.DMUL)
    @get:JvmName("idiv") public val idiv: Unit get() = +InsnNode(Opcodes.IDIV)
    @get:JvmName("ldiv") public val ldiv: Unit get() = +InsnNode(Opcodes.LDIV)
    @get:JvmName("fdiv") public val fdiv: Unit get() = +InsnNode(Opcodes.FDIV)
    @get:JvmName("ddiv") public val ddiv: Unit get() = +InsnNode(Opcodes.DDIV)
    @get:JvmName("irem") public val irem: Unit get() = +InsnNode(Opcodes.IREM)
    @get:JvmName("lrem") public val lrem: Unit get() = +InsnNode(Opcodes.LREM)
    @get:JvmName("frem") public val frem: Unit get() = +InsnNode(Opcodes.FREM)
    @get:JvmName("drem") public val drem: Unit get() = +InsnNode(Opcodes.DREM)
    @get:JvmName("ineg") public val ineg: Unit get() = +InsnNode(Opcodes.INEG)
    @get:JvmName("lneg") public val lneg: Unit get() = +InsnNode(Opcodes.LNEG)
    @get:JvmName("fneg") public val fneg: Unit get() = +InsnNode(Opcodes.FNEG)
    @get:JvmName("dneg") public val dneg: Unit get() = +InsnNode(Opcodes.DNEG)
    @get:JvmName("ishl") public val ishl: Unit get() = +InsnNode(Opcodes.ISHL)
    @get:JvmName("lshl") public val lshl: Unit get() = +InsnNode(Opcodes.LSHL)
    @get:JvmName("ishr") public val ishr: Unit get() = +InsnNode(Opcodes.ISHR)
    @get:JvmName("lshr") public val lshr: Unit get() = +InsnNode(Opcodes.LSHR)
    @get:JvmName("iushr") public val iushr: Unit get() = +InsnNode(Opcodes.IUSHR)
    @get:JvmName("lushr") public val lushr: Unit get() = +InsnNode(Opcodes.LUSHR)
    @get:JvmName("iand") public val iand: Unit get() = +InsnNode(Opcodes.IAND)
    @get:JvmName("land") public val land: Unit get() = +InsnNode(Opcodes.LAND)
    @get:JvmName("ior") public val ior: Unit get() = +InsnNode(Opcodes.IOR)
    @get:JvmName("lor") public val lor: Unit get() = +InsnNode(Opcodes.LOR)
    @get:JvmName("ixor") public val ixor: Unit get() = +InsnNode(Opcodes.IXOR)
    @get:JvmName("lxor") public val lxor: Unit get() = +InsnNode(Opcodes.LXOR)
    @get:JvmName("i2l") public val i2l: Unit get() = +InsnNode(Opcodes.I2L)
    @get:JvmName("i2f") public val i2f: Unit get() = +InsnNode(Opcodes.I2F)
    @get:JvmName("i2d") public val i2d: Unit get() = +InsnNode(Opcodes.I2D)
    @get:JvmName("l2i") public val l2i: Unit get() = +InsnNode(Opcodes.L2I)
    @get:JvmName("l2f") public val l2f: Unit get() = +InsnNode(Opcodes.L2F)
    @get:JvmName("l2d") public val l2d: Unit get() = +InsnNode(Opcodes.L2D)
    @get:JvmName("f2i") public val f2i: Unit get() = +InsnNode(Opcodes.F2I)
    @get:JvmName("f2l") public val f2l: Unit get() = +InsnNode(Opcodes.F2L)
    @get:JvmName("f2d") public val f2d: Unit get() = +InsnNode(Opcodes.F2D)
    @get:JvmName("d2i") public val d2i: Unit get() = +InsnNode(Opcodes.D2I)
    @get:JvmName("d2l") public val d2l: Unit get() = +InsnNode(Opcodes.D2L)
    @get:JvmName("d2f") public val d2f: Unit get() = +InsnNode(Opcodes.D2F)
    @get:JvmName("i2b") public val i2b: Unit get() = +InsnNode(Opcodes.I2B)
    @get:JvmName("i2c") public val i2c: Unit get() = +InsnNode(Opcodes.I2C)
    @get:JvmName("i2s") public val i2s: Unit get() = +InsnNode(Opcodes.I2S)
    @get:JvmName("lcmp") public val lcmp: Unit get() = +InsnNode(Opcodes.LCMP)
    @get:JvmName("fcmpl") public val fcmpl: Unit get() = +InsnNode(Opcodes.FCMPL)
    @get:JvmName("fcmpg") public val fcmpg: Unit get() = +InsnNode(Opcodes.FCMPG)
    @get:JvmName("dcmpl") public val dcmpl: Unit get() = +InsnNode(Opcodes.DCMPL)
    @get:JvmName("dcmpg") public val dcmpg: Unit get() = +InsnNode(Opcodes.DCMPG)
    @get:JvmName("ireturn") public val ireturn: Unit get() = +InsnNode(Opcodes.IRETURN)
    @get:JvmName("lreturn") public val lreturn: Unit get() = +InsnNode(Opcodes.LRETURN)
    @get:JvmName("freturn") public val freturn: Unit get() = +InsnNode(Opcodes.FRETURN)
    @get:JvmName("dreturn") public val dreturn: Unit get() = +InsnNode(Opcodes.DRETURN)
    @get:JvmName("areturn") public val areturn: Unit get() = +InsnNode(Opcodes.ARETURN)
    @get:JvmName("_return") public val _return: Unit get() = +InsnNode(Opcodes.RETURN)
    @get:JvmName("arraylength") public val arraylength: Unit get() = +InsnNode(Opcodes.ARRAYLENGTH)
    @get:JvmName("athrow") public val athrow: Unit get() = +InsnNode(Opcodes.ATHROW)
    @get:JvmName("monitorenter") public val monitorenter: Unit get() = +InsnNode(Opcodes.MONITORENTER)
    @get:JvmName("monitorexit") public val monitorexit: Unit get() = +InsnNode(Opcodes.MONITOREXIT)

    public fun bipush(n: Int): Unit = +IntInsnNode(Opcodes.BIPUSH, n)
    public fun sipush(n: Int): Unit = +IntInsnNode(Opcodes.SIPUSH, n)
    public fun newarray(type: Int): Unit = +IntInsnNode(Opcodes.NEWARRAY, type)

    public fun ldc(cst: Any): Unit = +LdcInsnNode(cst)

    public fun ifeq(label: LabelNode): Unit = +JumpInsnNode(Opcodes.IFEQ, label)
    public fun ifne(label: LabelNode): Unit = +JumpInsnNode(Opcodes.IFNE, label)
    public fun iflt(label: LabelNode): Unit = +JumpInsnNode(Opcodes.IFLT, label)
    public fun ifge(label: LabelNode): Unit = +JumpInsnNode(Opcodes.IFGE, label)
    public fun ifgt(label: LabelNode): Unit = +JumpInsnNode(Opcodes.IFGT, label)
    public fun ifle(label: LabelNode): Unit = +JumpInsnNode(Opcodes.IFLE, label)
    public fun if_icmpeq(label: LabelNode): Unit = +JumpInsnNode(Opcodes.IF_ICMPEQ, label)
    public fun if_icmpne(label: LabelNode): Unit = +JumpInsnNode(Opcodes.IF_ICMPNE, label)
    public fun if_icmplt(label: LabelNode): Unit = +JumpInsnNode(Opcodes.IF_ICMPLT, label)
    public fun if_icmpge(label: LabelNode): Unit = +JumpInsnNode(Opcodes.IF_ICMPGE, label)
    public fun if_icmpgt(label: LabelNode): Unit = +JumpInsnNode(Opcodes.IF_ICMPGT, label)
    public fun if_icmple(label: LabelNode): Unit = +JumpInsnNode(Opcodes.IF_ICMPLE, label)
    public fun if_acmpeq(label: LabelNode): Unit = +JumpInsnNode(Opcodes.IF_ACMPEQ, label)
    public fun if_acmpne(label: LabelNode): Unit = +JumpInsnNode(Opcodes.IF_ACMPNE, label)
    public fun goto(label: LabelNode): Unit = +JumpInsnNode(Opcodes.GOTO, label)
    public fun ifnull(label: LabelNode): Unit = +JumpInsnNode(Opcodes.IFNULL, label)
    public fun ifnonnull(label: LabelNode): Unit = +JumpInsnNode(Opcodes.IFNONNULL, label)

    public fun invokedynamic(name: String, desc: String, bsm: Handle, vararg bsmArgs: Any): Unit =
        +InvokeDynamicInsnNode(name, desc, bsm, *bsmArgs)

    public fun invokevirtual(owner: String, name: String, desc: String): Unit =
        +MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner, name, desc, false)

    public fun invokespecial(owner: String, name: String, desc: String): Unit =
        +MethodInsnNode(Opcodes.INVOKESPECIAL, owner, name, desc, false)

    public fun invokestatic(owner: String, name: String, desc: String): Unit =
        +MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, desc, false)

    public fun invokeinterface(owner: String, name: String, desc: String): Unit =
        +MethodInsnNode(Opcodes.INVOKEINTERFACE, owner, name, desc, true)

    public fun new(type: String): Unit = +TypeInsnNode(Opcodes.NEW, type)
    public fun anewarray(type: String): Unit = +TypeInsnNode(Opcodes.ANEWARRAY, type)
    public fun checkcast(type: String): Unit = +TypeInsnNode(Opcodes.CHECKCAST, type)
    public fun instanceof(type: String): Unit = +TypeInsnNode(Opcodes.INSTANCEOF, type)

    public fun iload(`var`: Int): Unit = +VarInsnNode(Opcodes.ILOAD, `var`)
    public fun lload(`var`: Int): Unit = +VarInsnNode(Opcodes.LLOAD, `var`)
    public fun fload(`var`: Int): Unit = +VarInsnNode(Opcodes.FLOAD, `var`)
    public fun dload(`var`: Int): Unit = +VarInsnNode(Opcodes.DLOAD, `var`)
    public fun aload(`var`: Int): Unit = +VarInsnNode(Opcodes.ALOAD, `var`)
    public fun istore(`var`: Int): Unit = +VarInsnNode(Opcodes.ISTORE, `var`)
    public fun lstore(`var`: Int): Unit = +VarInsnNode(Opcodes.LSTORE, `var`)
    public fun fstore(`var`: Int): Unit = +VarInsnNode(Opcodes.FSTORE, `var`)
    public fun dstore(`var`: Int): Unit = +VarInsnNode(Opcodes.DSTORE, `var`)
    public fun astore(`var`: Int): Unit = +VarInsnNode(Opcodes.ASTORE, `var`)

    public fun f_new(numLocal: Int, local: Array<Any>?, numStack: Int, stack: Array<Any>?): Unit =
        +FrameNode(Opcodes.F_NEW, numLocal, local, numStack, stack)

    public fun f_full(numLocal: Int, local: Array<Any>?, numStack: Int, stack: Array<Any>?): Unit =
        +FrameNode(Opcodes.F_FULL, numLocal, local, numStack, stack)

    public fun f_append(numLocal: Int, local: Array<Any>): Unit =
        +FrameNode(Opcodes.F_APPEND, numLocal, local, 0, null)

    public fun f_chop(numLocal: Int): Unit =
        +FrameNode(Opcodes.F_CHOP, numLocal, null, 0, null)

    public fun f_same(): Unit =
        +FrameNode(Opcodes.F_SAME, 0, null, 0, null)

    public fun f_same1(stack: Any): Unit =
        +FrameNode(Opcodes.F_SAME1, 0, null, 1, arrayOf(stack))

    public fun int(n: Int): Unit = when (n) {
        in -1..5 -> +InsnNode(Opcodes.ICONST_0 + n)
        in Byte.MIN_VALUE..Byte.MAX_VALUE -> bipush(n)
        in Short.MIN_VALUE..Short.MAX_VALUE -> sipush(n)
        else -> ldc(n)
    }
}

private class InsnListBuilder : InsnBuilder() {
    public val list = InsnList()
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

public fun List<MethodNode>.named(name: String): MethodNode = find { it.name == name } ?: error("Method '$name' not found")
public fun List<MethodNode>.search(name: String, desc: String): MethodNode = find { it.name == name && it.desc == desc } ?: error("Method '$name' with descriptor '$desc' not found")
public fun List<MethodNode>.search(name: String, returnType: String, vararg args: String): MethodNode = find { it.name == name && it.desc == "(${args.joinToString("")})$returnType" } ?: error("Method '$name' with descriptor '(${args.joinToString("")})$returnType' not found")
public fun List<FieldNode>.named(name: String): FieldNode = find { it.name == name } ?: error("Field '$name' not found")
public fun List<FieldNode>.search(name: String, type: String): FieldNode = find { it.name == name && it.desc == type } ?: error("Field '$name' with type '$type' not found")

public inline fun <reified T : Any> internalNameOf(): String = Type.getInternalName(T::class.java)
public fun internalNameOf(javaClass: KClass<*>): String = Type.getInternalName(javaClass.java)

public inline fun <reified T : AbstractInsnNode> AbstractInsnNode.next(p: (T) -> Boolean = { true }): T? {
    return generateSequence(next) { it.next }.filterIsInstance<T>().find(p)
}

public inline fun <reified T : AbstractInsnNode> AbstractInsnNode.prev(p: (T) -> Boolean = { true }): T? {
    return generateSequence(previous) { it.previous }.filterIsInstance<T>().find(p)
}

public fun ByteArray.dump(fileOrDir: String, className: String? = null): Unit {
    var targetPath = Paths.get(fileOrDir)

    if (Files.exists(targetPath)) {
        if (targetPath.isDirectory()) {
            val name = className ?: error("Cannot dump to a directory without a class name specification.")

            targetPath = targetPath.resolve(name)
        }
    } else {
        targetPath.parent?.createDirectories()
    }

    targetPath.writeBytes(this)
}

public fun ClassNode.dump(file: String): Unit {
    val cw = ClassWriter(0)
    accept(cw)
    cw.toByteArray().dump(file, name.replace('/', '.') + ".class")
}

public inline fun <reified T : Any> InsnBuilder.getSingleton(): Unit =
    getstatic(internalNameOf<T>(), "INSTANCE", "L${internalNameOf<T>()};")

public fun InsnBuilder.println(): Unit {
    getstatic("java/lang/System", "out", "Ljava/io/PrintStream;")
    swap
    invokevirtual("java/io/PrintStream", "println", "(Ljava/lang/Object;)V")
}
