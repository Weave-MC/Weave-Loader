package club.maxstats.weave.loader.api

import org.objectweb.asm.tree.ClassNode

abstract class Hook {
    val targetClassesName: List<String>?
    val check: ((ByteArray) -> Boolean)?

    constructor(targetClassName: String) {
        targetClassesName = listOf(targetClassName)
        check = null
    }

    constructor(targetClassesName: List<String>) {
        this.targetClassesName = targetClassesName
        check = null
    }

    constructor(check: (ByteArray) -> Boolean) {
        targetClassesName = null
        this.check = check
    }

    abstract fun transform(node: ClassNode, cfg: AssemblerConfig)

    abstract class AssemblerConfig {
        abstract fun computeFrames()
    }
}
