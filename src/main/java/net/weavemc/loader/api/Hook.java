package net.weavemc.loader.api;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;

public abstract class Hook {
    public final String[] targets;

    public Hook() {
        this.targets = new String[0];
    }

    // Two arguments to maintain bytecode compatability with weave 0.2.3
    public Hook(String target, String... extraTargets) {
        this.targets = ArrayUtils.add(extraTargets, target);
    }

    public abstract void transform(@NotNull ClassNode node, @NotNull AssemblerConfig cfg);

    public interface AssemblerConfig {
        void computeFrames();
    }
}
