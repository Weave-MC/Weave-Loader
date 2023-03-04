# Weave-Loader

Weave-Loader is a tool designed to simplify the process of modding Minecraft. It allows developers to easily create mods by providing a simple and intuitive API for interacting with Minecraft's codebase. Weave-Loader is written in Kotlin and uses Java Agents to attach to Minecraft processes and modify the game's classes at runtime.

## Getting Started

To use Weave-Loader, you will need to have a working installation of Minecraft and the Java Development Kit (JDK) installed on your system.

### Installation

To install Weave-Loader, you can either download a pre-built release from the [releases page](https://github.com/Weave-MC/Weave-Loader/releases), or build it yourself from source.

To build from source, clone the repository and run the following command in the root directory:

`./gradlew build`

This will compile the code and generate a JAR file in the `build/libs` directory.

### Usage

You can use Weave-Loader as a dependency in your project by implementing it as a repository. To do this, add the following code to your `build.gradle` file:

```gradle
repositories {
    maven {
        url 'https://jitpack.io'
    }
}

dependencies {
    implementation 'com.github.Weave-MC:Weave-Loader:VERSION'
}
```

Replace `VERSION` with the version of Weave-Loader you wish to use (e.g. `1.0.0`).

Alternatively, you can download the JAR file from the [releases page](https://github.com/Weave-MC/Weave-Loader/releases) and add it to your project's classpath.

When you launch Minecraft, Weave-Loader will automatically attach to the game's process and modify its classes to provide modding capabilities. You can then use the provided API to create your own mods.

For more information on how to use Weave-Loader, see the [wiki](https://github.com/Weave-MC/Weave-Loader/wiki).

## Contributing

We welcome contributions from anyone interested in improving Weave-Loader. If you find a bug or have an idea for a new feature, feel free to submit a pull request.

Before submitting a pull request, please make sure your changes are in line with our coding standards and that all tests pass. You can run tests by running the following command:

`./gradlew test`

## License

Weave-Loader is released under the MIT License. See [LICENSE](https://github.com/Weave-MC/Weave-Loader/blob/main/LICENSE) for more information.
