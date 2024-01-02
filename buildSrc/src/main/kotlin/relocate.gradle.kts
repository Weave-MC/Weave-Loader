import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

val relocate = tasks.register("relocate") {
    val jarTask = tasks.named<Jar>("jar")
    dependsOn(jarTask)

    doLast {
        val originalOutputFile = jarTask.get().outputs.files.singleFile
        val path = File(originalOutputFile.parentFile, "${originalOutputFile.nameWithoutExtension}-final.jar")
        path.parentFile.mkdirs()
        path.delete()

        val mapping = mapOf(
            "org/objectweb/asm/" to "net/weavemc/asm/",
            "com/google/" to "net/weavemc/google/"
        )

        val remapPackages = setOf("net/weavemc/", "com/grappenmaker/mappings/")
        val mappingsExclusions = listOf("gson")
        val relocateExclusions = setOf("net/weavemc/loader/mixins/WeaveMixinService")

        fun findMapping(name: String) = mapping.entries.find { (k) -> name.startsWith(k) }
            ?.takeIf { mappingsExclusions.none { it in name } }

        fun remap(name: String) = findMapping(name)?.let { (k, v) -> name.replaceFirst(k, v) } ?: name
        fun shouldKeepOriginal(name: String) =
            name.startsWith("com/google/common/io")

        JarOutputStream(FileOutputStream(path)).use { output ->
            val artifact = JarFile(originalOutputFile)
            val entries = artifact.entries()

            val (classes, resources) =
                entries.asSequence().filterNot { it.isDirectory }.toList().partition { it.name.endsWith(".class") }

            classes.forEach { entry ->
                val originalBytes = artifact.read(entry)
                val toMap = findMapping(entry.name)
                val internalName = entry.name.removeSuffix(".class")
                val shouldRemap = toMap != null || remapPackages.any { it in internalName }
                val isExcluded = internalName in relocateExclusions

                if (shouldRemap && !isExcluded) {
                    val newName = if (toMap != null) entry.name.replaceFirst(toMap.key, toMap.value) else entry.name
                    if (toMap != null && shouldKeepOriginal(internalName)) output.writeEntry(entry.name, originalBytes)

                    val reader = ClassReader(originalBytes)
                    val writer = ClassWriter(reader, 0)
                    reader.accept(ClassRemapper(writer, object : Remapper() {
                        override fun map(name: String) = remap(name)
                    }), 0)

                    output.writeEntry(newName, writer.toByteArray())
                } else output.writeEntry(entry.name, originalBytes)
            }

            resources.forEach { output.writeEntry(it.name, artifact.read(it)) }
        }
    }
}

fun JarOutputStream.writeEntry(name: String, bytes: ByteArray) {
    putNextEntry(JarEntry(name))
    write(bytes)
    closeEntry()
}

fun JarFile.read(entry: JarEntry) = getInputStream(entry).readBytes()

tasks.named("assemble") { dependsOn(relocate) }
