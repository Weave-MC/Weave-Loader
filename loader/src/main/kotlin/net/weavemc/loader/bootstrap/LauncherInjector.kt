package net.weavemc.loader.bootstrap

import net.weavemc.api.bytecode.asm
import net.weavemc.api.bytecode.internalNameOf
import net.weavemc.api.bytecode.named
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import java.lang.instrument.Instrumentation

class MultiMcInjector(private val inst: Instrumentation) : SafeTransformer {
    override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray? {
        if (className != "org/multimc/onesix/OneSixLauncher") {
            return null
        }

        try {
            val classReader = ClassReader(originalClass)
            val classNode = ClassNode()
            classReader.accept(classNode, 0)

            classNode.methods.named("launch").instructions.insert(asm {
                aload(1)
                invokestatic(internalNameOf<MultiMcInjector>(), ::processParamBucket.name, "(Ljava/lang/Object;)V")
            })

            val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES)
            classNode.accept(classWriter)

            return classWriter.toByteArray()
        } finally {
            inst.removeTransformer(this)
            inst.addTransformer(WeaveBootstrapEntryPoint(inst))
        }
    }

    companion object {
        @JvmStatic
        fun processParamBucket(paramBucketObject: Any) {
            val m_params = paramBucketObject::class.java.getDeclaredField("m_params")
            m_params.isAccessible = true

            val paramBucket = m_params.get(paramBucketObject) as MutableMap<String, List<String>>

            System.getProperties()["weave.extra.launch.params"] = paramBucket
        }
    }
}

class PrismLauncherInjector(private val inst: Instrumentation) : SafeTransformer {
    override fun transform(loader: ClassLoader, className: String, originalClass: ByteArray): ByteArray? {
        if (className != "org/prismlauncher/EntryPoint") {
            return null
        }

        try {
            val classReader = ClassReader(originalClass)
            val classNode = ClassNode()
            classReader.accept(classNode, 0)

            with(classNode.methods.named("listen")) {
                val parametersInstantiation = instructions
                    .filterIsInstance<MethodInsnNode>()
                    .first { it.opcode == Opcodes.INVOKESPECIAL && it.name == "<init>" && it.owner == "org/prismlauncher/utils/Parameters" }

                instructions.insert(parametersInstantiation, asm {
                    dup
                    invokestatic(internalNameOf<PrismLauncherInjector>(), ::processParameters.name, "(Ljava/lang/Object;)V")
                })
            }

            val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES)
            classNode.accept(classWriter)

            return classWriter.toByteArray()
        } finally {
            inst.removeTransformer(this)
            inst.addTransformer(WeaveBootstrapEntryPoint(inst))
        }
    }

    companion object {
        @JvmStatic
        fun processParameters(parametersObject: Any) {
            val map = parametersObject::class.java.getDeclaredField("map")
            map.isAccessible = true

            val parameters = map.get(parametersObject) as MutableMap<String, List<String>>

            System.getProperties()["weave.extra.launch.params"] = parameters
        }
    }
}