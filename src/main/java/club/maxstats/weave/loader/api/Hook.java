package club.maxstats.weave.loader.api;

import lombok.AllArgsConstructor;
import org.objectweb.asm.tree.ClassNode;

@AllArgsConstructor
public abstract class Hook {
    public final String targetClassName;

    public abstract void transform(ClassNode node, AssemblerConfig cfg);

    public interface AssemblerConfig {
        void computeFrames();
    }
}
