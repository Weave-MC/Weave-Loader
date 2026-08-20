package net.weavemc.gradle.dependency

import com.grappenmaker.mappings.ClasspathLoaders
import com.grappenmaker.mappings.remap.MappingsRemapper
import com.grappenmaker.mappings.remap.remapJar
import com.grappenmaker.mappings.remapping
import net.weavemc.gradle.ext
import net.weavemc.gradle.loadMergedMappings
import net.weavemc.gradle.util.minecraftJarCache
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.extra
import java.util.jar.JarFile

public fun DependencyHandler.remapImplementation(
    dependencyNotation: Any,
    from: String,
    to: String? = null
): Dependency? {
    val project = (this as? ExtensionAware)?.extra?.get("weaveProject") as? Project
        ?: error("Project reference missing")

    val targetNamespace = to ?: ext.configuration.get().namespace

    val detachedConfig = project.configurations.detachedConfiguration(project.dependencies.create(dependencyNotation))
    detachedConfig.isTransitive = false

    val rawJarFile = detachedConfig.singleFile

    val remappedJarFile = project.layout.buildDirectory.file(
        "weave/remapped/${rawJarFile.nameWithoutExtension}-$from-$targetNamespace.jar"
    ).get().asFile

    if (!remappedJarFile.exists()) {
        remappedJarFile.parentFile.mkdirs()

        val version = ext.version.get()
        val mappings = version.loadMergedMappings()
        JarFile(version.minecraftJarCache).use {
            val loader = ClasspathLoaders.compound(
                ClasspathLoaders.fromSystemLoader(),
                ClasspathLoaders.fromJar(it)
            )
            val mappedLoader = loader.remapping(
                MappingsRemapper(
                    mappings = mappings,
                    from = "official",
                    to = from,
                    loader = loader
                )
            )

            remapJar(
                mappings = mappings,
                input = rawJarFile,
                output = remappedJarFile,
                from = from,
                to = targetNamespace,
                loader = mappedLoader,
                visitor = { parent ->
                    KotlinMetadataClassVisitor(parent, MappingsRemapper(
                        mappings = mappings,
                        from = from,
                        to = targetNamespace,
                        loader = mappedLoader
                    ))
                }
            )
        }
    }

//    val javadocConfig = project.configurations.detachedConfiguration(
//        project.dependencies.create("$dependencyNotation:javadoc")
//    ).apply {
//        isTransitive = false
//    }

    val remappedFiles = project.files(
        remappedJarFile,
//        javadocConfig.singleFile
    )

    return project.dependencies.add("implementation", remappedFiles)
}