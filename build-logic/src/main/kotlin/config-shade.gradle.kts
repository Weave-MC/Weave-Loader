import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.creating

plugins {
    id("com.github.johnrengelman.shadow")
}

val shade: Configuration by configurations.creating
val api: Configuration by configurations.getting
api.extendsFrom(shade)

tasks {
    val shadowJar by getting(ShadowJar::class) {
        archiveClassifier.set("all")

        configurations = listOf(shade)
        exclude(
            "META-INF/*.SF",
            "META-INF/*.DSA",
            "META-INF/*.RSA",
            "OSGI-INF/**",
            "*.profile",
            "module-info.class",
            "META-INF/versions/**",
            "ant_tasks/**"
        )

        listOf(
            "com.google.",
            "org.objectweb.asm.",
            "org.spongepowered.",
        ).forEach { pkg ->
            val packageName = pkg.substringBeforeLast(".").substringAfterLast(".")
            val relocated = "net.weavemc.loader.shaded.$packageName."
            relocate(pkg, relocated)
        }

        mergeServiceFiles()

        afterEvaluate {
            val jarTask = tasks.getByName<Jar>("jar")
            this@getting.manifest.inheritFrom(jarTask.manifest)
        }
    }

    val build by getting {
        dependsOn(shadowJar)
    }
}
