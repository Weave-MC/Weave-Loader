import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow")
}

val shade: Configuration by configurations.creating
val api: Configuration by configurations.getting
api.extendsFrom(shade)

val shadedPackage = "net.weavemc.loader.shaded"

tasks {
    val shadowJar by getting(ShadowJar::class) {
        archiveClassifier.set("all")

        configurations = listOf(shade)
        exclude(
            // Unwanted libraries
            "org/intellij/lang/annotations/**", "org/jetbrains/annotations/**",

            // Signature files
            "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA",
            // OSGI-related metadata
            "OSGI-INF/**", "*.profile",

            // Mixin external services
            "META-INF/services/org.spongepowered.*",
            "META-INF/services/cpw.mods.modlauncher.*",

            /// Remove classes built with J9+ so that Legacy Forge doesn't spam logs
            // Module info + Multi-version classes
            "module-info.class", "META-INF/versions/**",
            // Mixin classes for integrating with ModLauncher 9+
            "org/spongepowered/asm/launch/MixinLaunchPlugin.class",
            "org/spongepowered/asm/launch/MixinTransformationService.class",
            "org/spongepowered/asm/launch/platform/container/ContainerHandleModLauncherEx.class",
            "org/spongepowered/asm/launch/platform/container/ContainerHandleModLauncherEx\$SecureJarResource.class",
        )

        listOf(
            "com.grappenmaker.",
            "org.objectweb.asm.",
            "org.spongepowered.",
            "kotlin.",
            "kotlinx.",
        ).forEach { pkg ->
            val packageName = pkg.substringBeforeLast(".").substringAfterLast(".")
            val relocated = "$shadedPackage.$packageName."
            relocate(pkg, relocated)
        }

        mergeServiceFiles()

        afterEvaluate {
            val jarTask = tasks.getByName<Jar>("jar")
            manifest.inheritFrom(jarTask.manifest)

            // Fix mixin whining about a missing version
            manifest.attributes(mapOf(
                "Implementation-Version" to "9.7"
            ), "${shadedPackage.replace('.', '/')}/asm/")
        }
    }

    val build by getting {
        dependsOn(shadowJar)
    }
}
