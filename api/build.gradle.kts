plugins {
    id("com.github.weave-mc.weave-gradle") version "bcf6ab0279" apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
}
