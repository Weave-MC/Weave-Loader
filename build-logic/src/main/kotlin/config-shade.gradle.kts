import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow")
}

repositories {
    mavenCentral()
}

val shade: Configuration by configurations.creating
val api: Configuration by configurations.getting
api.extendsFrom(shade)

val targetPackage = "net.weavemc.loader.impl.shaded"
val packagesList = listOf(
    "com.grappenmaker.mappings",
    "org.objectweb.asm",
    "org.spongepowered",
    "kotlin",
    "kotlinx",
)

tasks {
    val createRelocationData by creating(CreateRelocationData::class) {
        shadedPackage = targetPackage
        relocationList.addAll(packagesList)
    }

    val shadowJar by getting(ShadowJar::class) {
        archiveClassifier.set("all")

        configurations = listOf(shade)
        exclude(
            // Unwanted libraries
            "org/intellij/lang/annotations/**", "org/jetbrains/annotations/**",

            // Unwanted metadata files
            "META-INF/maven/**", "META-INF/proguard/**", "META-INF/com.android.tools/**",

            // Kotlin metadata files. It is essentially useless since we relocate the entire stdlib.
            // The only drawback is that kotlin-reflect will not work in certain cases.
            "META-INF/**/*.kotlin_module", "**/*.kotlin_builtins",

            // Vague license files from other libraries
            // Note: This is just to not have random licenses not linked to their
            // project in the final jar. The acknowledgements for the used libraries
            // will be provided somewhere else.
            "LICENSE.txt", "LICENSE", "NOTICE.txt", "NOTICE",

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

        from(createRelocationData)

        packagesList.forEach { pkg ->
            val packageName = pkg.substringAfterLast(".")
            val relocated = "$targetPackage.$packageName."
            relocate("$pkg.", relocated)
        }

        mergeServiceFiles()

        afterEvaluate {
            val jarTask = tasks.getByName<Jar>("jar")
            manifest.inheritFrom(jarTask.manifest)

            // Fix mixin whining about a missing version
            manifest.attributes(mapOf(
                "Implementation-Version" to "9.7"
            ), "${targetPackage.replace('.', '/')}/asm/")
        }
    }

    val build by getting {
        dependsOn(shadowJar)
    }
}
