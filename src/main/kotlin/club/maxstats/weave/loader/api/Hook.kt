package club.maxstats.weave.loader.api

import org.objectweb.asm.tree.ClassNode

public abstract class Hook(public val targetClassName: String) {
    public abstract fun transform(node: ClassNode, cfg: AssemblerConfig)

    public abstract class AssemblerConfig {
        public abstract fun computeFrames()
    }
}
