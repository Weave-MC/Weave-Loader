package club.maxstats.weave.loader.api;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;

@AllArgsConstructor
public abstract class Hook {
    public final String targetClassName;

    public abstract void transform(@NotNull ClassNode node, @NotNull AssemblerConfig cfg);

    public interface AssemblerConfig {
        void computeFrames();
    }
}
