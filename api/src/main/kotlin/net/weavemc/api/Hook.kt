package net.weavemc.api

import org.objectweb.asm.tree.ClassNode

abstract class Hook(vararg val targets: String) {
    abstract fun transform(node: ClassNode, cfg: AssemblerConfig)

    interface AssemblerConfig {
        fun computeFrames()
    }
}
