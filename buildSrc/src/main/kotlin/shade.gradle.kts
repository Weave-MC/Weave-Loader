afterEvaluate {
    tasks.withType<Jar> {
        from(configurations["runtimeClasspath"].map { if (it.isDirectory) it else zipTree(it) }) {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE

            exclude(
                "**/module-info.class",
                "META-INF/*.SF",
                "META-INF/*.DSA",
                "META-INF/*.RSA",
            )
        }
    }
}