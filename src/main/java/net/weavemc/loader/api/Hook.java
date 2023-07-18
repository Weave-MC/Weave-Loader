package net.weavemc.loader.api;

import org.apache.commons.lang3.ArrayUtils;
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
