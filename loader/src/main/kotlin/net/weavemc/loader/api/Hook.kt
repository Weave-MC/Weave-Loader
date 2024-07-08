package net.weavemc.loader.api

import org.objectweb.asm.tree.ClassNode

/**
 * A hook is a type of weave mod intrinsic that allows modders to change the bytecode of any class.
 * Implementors are expected to have a no-args public constructor
 *
 * @property targets the internal names of the targets this [Hook] wants to modify, in the namespace of the mod
 */
public abstract class Hook(public vararg val targets: String) {
    /**
     * Receives all transformed [ClassNode]s in the namespace of the mod that match [targets].
     * The class writer parameters can be altered using the [AssemblerConfig].
     */
    public abstract fun transform(node: ClassNode, cfg: AssemblerConfig)

    /**
     * Allows modders to alter class writer parameters
     */
    public interface AssemblerConfig {
        /**
         * Enables [org.objectweb.asm.ClassWriter.COMPUTE_FRAMES]
         */
        public fun computeFrames()
    }
}
