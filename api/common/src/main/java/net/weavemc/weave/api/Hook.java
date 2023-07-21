package net.weavemc.weave.api;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;

public abstract class Hook {

    public final String[] targets;

    public Hook(String... targets) {
        this.targets = targets;
    }

    public abstract void transform(@NotNull ClassNode node, @NotNull AssemblerConfig cfg);

    public interface AssemblerConfig {
        void computeFrames();
    }

}
