package club.maxstats.weave.loader.hooks

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode

abstract class Hook(val name: String) {

    abstract fun transform(cn: ClassNode)

    val cwFlags: Int get() = ClassWriter.COMPUTE_MAXS

}