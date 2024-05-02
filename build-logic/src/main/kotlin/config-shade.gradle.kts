import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import gradle.kotlin.dsl.accessors._d73e0aad27c892ff134862f94ec3182c.build
import gradle.kotlin.dsl.accessors._d73e0aad27c892ff134862f94ec3182c.implementation
import gradle.kotlin.dsl.accessors._d73e0aad27c892ff134862f94ec3182c.jar
import org.gradle.kotlin.dsl.creating

plugins {
    id("com.github.johnrengelman.shadow")
}

val shade: Configuration by configurations.creating
configurations.implementation { extendsFrom(shade) }

tasks {
    val shadowJar by getting(ShadowJar::class) {
        archiveClassifier.set("all")

        configurations = listOf(shade)
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "OSGI-INF/**", "*.profile", "module-info.class", "ant_tasks/**")

        listOf(
            "com.google",
            "org.objectweb.asm",
            "org.spongepowered",
            "com.grappenmaker.mappings",
        ).forEach { pkg ->
            val relocated = "net.weavemc.loader.shaded.${pkg.substringAfterLast(".")}"
            relocate(pkg, relocated)
        }

        mergeServiceFiles()

        afterEvaluate {
            this@getting.manifest.inheritFrom(tasks.jar.get().manifest)
        }
    }

    build.get().dependsOn(shadowJar)
}
