package net.weavemc.loader.mixins

import net.weavemc.loader.WeaveLoader
import net.weavemc.loader.bootstrap.SafeTransformer
import net.weavemc.weave.api.demapper
import net.weavemc.weave.api.mapper

/**
 * Transformer which delegates the Mixin configuration and application
 * process to the [WeaveMixinService].
 */
internal object WeaveMixinTransformer: SafeTransformer {
    /**
     * @param loader        The classloader to use to load the class.
     * @param className     The name of the class to load.
     * @param originalClass The original class' bytes.
     * @return The transformed class' bytes from `className`.
     */
    override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray) = runCatching {
        WeaveLoader.mixinSandbox.transform(
            className.replace('/', '.'),
            WeaveLoader.mixinSandboxLoader.remapMixedInClass(originalClass, demapper)
        )?.let { WeaveLoader.mixinSandboxLoader.remapMixedInClass(it, mapper) }
    }.onFailure {
        println("Failed to apply mixin to $className:")
        it.printStackTrace()
    }.getOrNull()
}
