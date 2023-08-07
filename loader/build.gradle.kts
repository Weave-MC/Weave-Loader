import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    kotlin("plugin.lombok")
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
    implementation(libs.mixin)
    implementation(libs.kxSer)
    implementation("com.google.guava:guava:32.1.2-jre")
    api(libs.bundles.asm)
    api(project(":api:common"))
}

tasks.jar {
    from(configurations["runtimeClasspath"].map { if (it.isDirectory) it else zipTree(it) }) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        exclude(
            "**/module-info.class",
            "META-INF/*.SF",
            "META-INF/*.DSA",
            "META-INF/*.RSA",
        )
    }

    manifest.attributes(
        "Premain-Class" to "net.weavemc.loader.bootstrap.AgentKt"
    )
}

tasks.build {
    doLast {
        val path = buildDir.resolve("libs").resolve("loader-bundle.jar")

        val jarOut = JarOutputStream(FileOutputStream(path))
        val output = JarFile(tasks.jar.get().outputs.files.singleFile)
        val entries = output.entries()

        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()

            // only modify classes
            if (!entry.isDirectory) {
                when {
                    entry.name.startsWith("org/objectweb/asm") || (entry.name.startsWith("net/weavemc") && !entry.name.contains("WeaveMixinService")) -> {
                        var entryName = entry.name

                        if (entry.name.startsWith("org/objectweb/asm")) {
                            entryName = entry.name.replaceFirst("org/objectweb", "net/weavemc")
                            jarOut.putNextEntry(JarEntry(entry.name))
                            jarOut.write(output.getInputStream(entry).readBytes())
                            jarOut.closeEntry()
                        }

                        val bytes = output.getInputStream(entry).readBytes()

                        val cr = ClassReader(bytes)
                        val cw = ClassWriter(cr, 0)
                        cr.accept(ClassRemapper(cw, object : Remapper() {
                            override fun map(internalName: String): String = internalName.replaceFirst("org/objectweb", "net/weavemc")
                        }), 0)

                        jarOut.putNextEntry(JarEntry(entryName))
                        jarOut.write(cw.toByteArray())
                        jarOut.closeEntry()
                    }

                    else -> writeEntryToFile(output, jarOut, entry, entry.name)
                }
            }
        }

        jarOut.close()
    }
}

fun writeEntryToFile(
    file: JarFile,
    outStream: JarOutputStream,
    entry: JarEntry,
    entryName: String
) {
    outStream.putNextEntry(JarEntry(entryName))
    outStream.write(file.getInputStream(entry).readBytes())
    outStream.closeEntry()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
