package club.maxstats.weave.loader.api;

import org.objectweb.asm.tree.ClassNode;

public abstract class Hook {
    public final String targetClassName;

    public Hook(String targetClassName) {
        this.targetClassName = targetClassName;
    }

    public abstract void transform(ClassNode node, AssemblerConfig cfg);

    public static abstract class AssemblerConfig {
        public abstract void computeFrames();
    }
}
