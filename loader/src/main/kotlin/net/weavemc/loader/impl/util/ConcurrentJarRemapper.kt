package net.weavemc.loader.impl.util

import com.grappenmaker.mappings.ClasspathLoader
import com.grappenmaker.mappings.ClasspathLoaders
import com.grappenmaker.mappings.asASMMapping
import com.grappenmaker.mappings.format.EmptyMappings
import com.grappenmaker.mappings.format.Mappings
import com.grappenmaker.mappings.memoizedTo
import com.grappenmaker.mappings.remap.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.Remapper
import java.io.InputStream
import java.nio.file.Path
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

@ExperimentalJarRemapper
internal val signatureResourceVisitor = JarResourceVisitor { name, file ->
    if (name.endsWith(".RSA") || name.endsWith(".SF")) null else file
}

/**
 * A concurrent version of [com.grappenmaker.mappings.remap.JarRemapper]
 */
@ExperimentalJarRemapper
public class ConcurrentJarRemapper {
    /**
     * The [com.grappenmaker.mappings.format.Mappings] that will be used to remap classes in input jar files
     */
    public var mappings: Mappings = EmptyMappings
    private val tasks = mutableListOf<JarRemapTask>()

    /**
     * The [ClasspathLoader] that should be used to request class files from classpath / environment files,
     * that are not present in input jars. Note that this [ClasspathLoader] will be wrapped into a new one,
     * including input jars and a memoization layer. It is therefore not recommended to add memoization layers
     * in this [ClasspathLoader]. The loader should be thread-safe.
     */
    public var loader: ClasspathLoader = { null }

    private val classVisitors = mutableListOf<JarClassVisitor>()
    private val resourceVisitors = mutableListOf(signatureResourceVisitor)
    private val extensions = mutableListOf<RemapperExtension>()

    /**
     * Determines whether the [JarRemapper] will copy non-classfile resources from input jars into output jars
     */
    public var copyResources: Boolean = true

    /**
     * Determines whether the [JarRemapper] will normalize the constant pool, that is, delete unused constants
     *
     * Reminder to NOT abbreviate constant pool thank you very much
     */
    public var normalizeConstantPool: Boolean = true

    /**
     * Adds a new remapping task to the [JarRemapper]. The [input] jar file will be read, remapped, and written
     * to an [output] file. The [input] jar file is expected to be in the [fromNamespace], and will be remapped
     * into the [toNamespace].
     */
    public fun task(input: Path, output: Path, fromNamespace: String, toNamespace: String) {
        tasks += JarRemapTask(input, output, fromNamespace, toNamespace)
    }

    /**
     * Adds a [JarClassVisitor] to the pipeline of visitors that will be applied to each remapped class
     *
     * @see [JarClassVisitor]
     */
    public fun visitClasses(visitor: JarClassVisitor) {
        classVisitors += visitor
    }

    /**
     * Adds a [JarResourceVisitor] to the pipeline of visitors that will be applied to each copied resource
     *
     * @see [JarResourceVisitor]
     */
    public fun visitResources(visitor: JarResourceVisitor) {
        resourceVisitors += visitor
    }

    /**
     * Adds a [RemapperExtension] to the pipeline of extensions that will be applied to each task
     *
     * @see [RemapperExtension]
     */
    public fun extension(extension: RemapperExtension) {
        extensions += extension
    }

    /**
     * Performs all configured tasks
     */
    public suspend fun perform() {
        val commonMemo = Collections.synchronizedMap(hashMapOf<String, ByteArray?>())
        val commonLoader = loader.memoizedTo(commonMemo)

        // toList to copy, to ensure no unexpected concurrency weirdness
        val context = ConcurrentContext(
            mappings, tasks, commonLoader, classVisitors.toList(),
            resourceVisitors.toList(), extensions.toList(), copyResources, normalizeConstantPool
        )

        supervisorScope {
            with(context) { tasks.toList().forEach { launch { it.start() } } }
        }
    }

    @OptIn(ExperimentalJarRemapper::class)
    internal class ConcurrentContext(
        val mappings: Mappings,
        tasks: List<JarRemapTask>,
        val loader: ClasspathLoader,
        val classVisitors: List<JarClassVisitor>,
        val resourceVisitors: List<JarResourceVisitor>,
        val extensions: List<RemapperExtension>,
        val copyResources: Boolean,
        val normalizeConstantPool: Boolean,
    ) {
        init {
            for (task in tasks) {
                fun String.checkNs() = check(this in mappings.namespaces) { "Namespace $this not found for task $task" }

                task.fromNamespace.checkNs()
                task.toNamespace.checkNs()
            }
        }

        val sharedMaps = tasks
            .mapTo(hashSetOf()) { it.fromNamespace to it.toNamespace }
            .associateWith { (f, t) -> mappings.asASMMapping(f, t) }

        private fun ByteArray.remap(
            remapper: Remapper,
            extraVisitors: List<JarClassVisitor>
        ): Pair<ByteArray, String> {
            val reader = ClassReader(this)
            val writer = ClassWriter(if (normalizeConstantPool) null else reader, 0)

            val originalName = reader.className
            var writtenName = remapper.map(originalName)
            val inner: ClassVisitor = object : ClassVisitor(Opcodes.ASM9, writer) {
                override fun visit(
                    version: Int,
                    access: Int,
                    name: String,
                    signature: String?,
                    superName: String?,
                    interfaces: Array<String>?
                ) {
                    writtenName = name
                    super.visit(version, access, name, signature, superName, interfaces)
                }
            }

            val outer = classVisitors.fold(inner) { acc, curr -> curr.visit(originalName, acc) ?: acc }
            val outerWithExtra = extraVisitors.fold(outer) { acc, curr -> curr.visit(originalName, acc) ?: acc }
            reader.accept(LambdaAwareRemapper(outerWithExtra, remapper), 0)

            return writer.toByteArray() to writtenName
        }

        suspend fun JarRemapTask.start() {
            val lookup = hashMapOf<String, ByteArray>()
            withContext(Dispatchers.IO) {
                JarInputStream(input.inputStream(), false).use { stream ->
                    JarOutputStream(output.outputStream()).use { out ->
                        while (true) {
                            val entry = stream.nextJarEntry ?: break
                            if (entry.name.endsWith(".class")) {
                                lookup[entry.name.dropLast(6)] = stream.readBytes()
                                continue
                            }

                            if (!copyResources) continue

                            var resource: InputStream = stream
                            for (visitor in resourceVisitors) resource = visitor.visit(entry.name, resource) ?: continue

                            out.putNextEntry(JarEntry(entry.name))
                            resource.copyTo(out)
                        }

                        val map = sharedMaps.getValue(fromNamespace to toNamespace)
                        val totalLoader = ClasspathLoaders.compound(ClasspathLoaders.fromLookup(lookup), loader)
                        val remapper = LoaderSimpleRemapper(map, totalLoader)

                        coroutineScope {
                            val resultChannel = Channel<Pair<ByteArray, String>>(capacity = Channel.BUFFERED)

                            val writerJob = launch(Dispatchers.IO) {
                                for ((bytes, writtenName) in resultChannel) {
                                    out.putNextEntry(JarEntry("$writtenName.class"))
                                    out.write(bytes)
                                }
                            }

                            launch {
                                val workerJobs = lookup.map { (_, original) ->
                                    launch(Dispatchers.Default) {
                                        val extraVisitors = extensions.map { it.createVisitor(totalLoader, remapper) }
                                        val result = original.remap(remapper, extraVisitors)
                                        resultChannel.send(result)
                                    }
                                }

                                workerJobs.joinAll()
                                resultChannel.close()
                            }

                            writerJob.join()
                        }
                    }
                }
            }
        }
    }
}

@ExperimentalJarRemapper
internal suspend inline fun performConcurrentRemap(builder: ConcurrentJarRemapper.() -> Unit) {
    ConcurrentJarRemapper().also(builder).perform()
}