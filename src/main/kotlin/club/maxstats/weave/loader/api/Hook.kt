package club.maxstats.weave.loader.api

import org.objectweb.asm.tree.ClassNode

abstract class Hook(val targetClassName: String) {
    abstract fun transform(cn: ClassNode, callback: Callback)

    class Callback {
        internal var computeFrames = false
            private set

        fun computeFrames() {
            computeFrames = true
        }
    }
}