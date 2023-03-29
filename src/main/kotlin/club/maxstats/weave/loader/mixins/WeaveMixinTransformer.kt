package club.maxstats.weave.loader.mixins

import club.maxstats.weave.loader.bootstrap.SafeTransformer
import org.spongepowered.asm.mixin.MixinEnvironment

internal object WeaveMixinTransformer: SafeTransformer {
    override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray? {
        return WeaveMixinService.transformer.transformClass(
            MixinEnvironment.getDefaultEnvironment(),
            className.replace('/', '.'),
            originalClass
        )
    }
}
