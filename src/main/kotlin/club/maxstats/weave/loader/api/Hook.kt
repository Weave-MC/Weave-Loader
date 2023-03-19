package club.maxstats.weave.loader.api

import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import kotlin.reflect.KClass

abstract class Hook(val targetClassName: String) {

    constructor(clazz: Class<*>) : this(Type.getInternalName(clazz))
    constructor(clazz: KClass<*>) : this(clazz.java)

    abstract fun transform(node: ClassNode, cfg: AssemblerConfig)

    abstract class AssemblerConfig {
        abstract fun computeFrames()
    }
}
