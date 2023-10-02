package net.weavemc.loader.mixins

import net.weavemc.loader.WeaveLoader
import net.weavemc.loader.bootstrap.SafeTransformer
import net.weavemc.weave.api.namedMapper
import net.weavemc.weave.api.runtimeMapper
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import java.io.PrintWriter
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes

/**
 * Transformer which delegates the Mixin configuration and application
 * process to the [WeaveMixinService].
 */
internal object WeaveMixinTransformer : SafeTransformer {
    /**
     * @param loader        The classloader to use to load the class.
     * @param className     The name of the class to load.
     * @param originalClass The original class' bytes.
     * @return The transformed class' bytes from `className`.
     */
    override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray) = runCatching {
        val originalInterfaces = ClassReader(originalClass).interfaces.toSet()

        // TODO: remove
        WeaveLoader.mixinSandbox.transform(
            namedMapper.map(className).replace('/', '.'),
            WeaveLoader.mixinSandboxLoader.remapMixedInClass(originalClass, namedMapper)
        )?.let { mixedIn ->
            WeaveLoader.mixinSandboxLoader.remapMixedInClass(mixedIn, runtimeMapper) { parent ->
                object : ClassVisitor(Opcodes.ASM9, parent) {
                    override fun visit(
                        version: Int,
                        access: Int,
                        name: String,
                        signature: String?,
                        superName: String?,
                        interfaces: Array<String>?
                    ) = super.visit(
                        version, access, name, signature, superName,
                        interfaces?.let { itf -> (itf.toSet() + originalInterfaces).toTypedArray() }
                    )
                }
            }
        }
    }.onFailure {
        println("Failed to apply mixin to $className:")
        it.printStackTrace()
    }.getOrNull()
}
