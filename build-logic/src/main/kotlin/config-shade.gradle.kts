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

fun ShadowJar.configStandard() {
    configurations = listOf(shade)
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "OSGI-INF/**", "*.profile", "module-info.class", "ant_tasks/**")
    mergeServiceFiles()
}

tasks {
    val shadowJar by getting(ShadowJar::class) {
        configStandard()
        archiveClassifier.set("all")

        arrayOf(
            "com.google",
            "org.objectweb.asm",
            "org.spongepowered",
            "com.grappenmaker.mappings",
        ).forEach { pkg ->
            val lastPackageIndex = pkg.lastIndexOf('.')
            val lastPackage = if (lastPackageIndex != -1) pkg.substring(lastPackageIndex + 1) else pkg
            val relocated = "net.weavemc.loader.shaded.${lastPackage}"
            relocate(pkg, relocated)
        }

        afterEvaluate {
            this@getting.manifest.inheritFrom(tasks.jar.get().manifest)
        }
    }

    build.get().dependsOn(shadowJar)
}