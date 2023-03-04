<img 
    align="right" alt="title" width="200px"
    src="https://static.wikia.nocookie.net/minecraft_gamepedia/images/d/d7/Loom_%28S%29_JE1_BE1.png/revision/latest?cb=20210116072516"
/>

### Weave Loader, a Lunar Client Mod Loader

<img
     align="left" alt="Worlflow Badge"
     src="https://github.com/Weave-MC/Weave-Loader/actions/workflows/gradle.yml/badge.svg"
/>

---

Weave Loader is a tool designed to simplify the process of modding Minecraft, and specifically Lunar Client. It allows developers to easily create mods by providing a simple and intuitive API for interacting with Minecraft's codebase, while injecting into clients that are somewhat closed-off to developers. 

## Getting Started

- Make sure a [JDK 17][jdk] is installed on your system.

- To test (Windows and Unix)

```bash
java --version # should output some JDK 17. 
```

### Installation

To install Weave-Loader, you can either download a pre-built release from the [releases page](https://github.com/Weave-MC/Weave-Loader/releases), or build it yourself from source.

#### Building with Gradle

- `git clone` the project, this can be achieved by installing [git][git], then running

```bash
git clone https://github.com/Weave-MC/Weave-Loader.git "Weave-Loader" 
```

- **UN*X**

```bash
cd $_ ; chmod +x ./gradlew && ./gradlew build
```

> Note that `$_` is the last argument from the previous command, should be run after cloning. 

- **Windows**

```powershell
cd Weave-Loader ; .\gradlew.bat build
```

### Usage

You can use Weave-Loader as a dependency in your project by implementing it as a repository. To do this, add the following code to your `build.gradle` file.

- **Groovy DSL**

```gradle
repositories {
    maven {
        url 'https://jitpack.io'
    }
}

dependencies {
    implementation 'com.github.Weave-MC:Weave-Loader:${VERSION}'
}
```

- **Kotlin DSL**

```kt
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.Weave-MC:Weave-Loader:${VERSION}")
}
```

Replace `${VERSION}` with the version of Weave-Loader in the Build Reference. (soon:tm:)

When you launch Minecraft, Weave-Loader will automatically attach to the game's process and modify its classes to provide modding capabilities. You can then use the provided API to create your own mods.

For more information on how to use Weave-Loader, see the [wiki](https://github.com/Weave-MC/Weave-Loader/wiki).

## Contributing

We welcome contributions from anyone interested in improving Weave-Loader. If you find a bug or have an idea for a new feature, feel free to submit a pull request.

Before submitting a pull request, please make sure your changes are in line with our coding standards and that all tests pass by passing gradle with a test flag, `./gradlew test` or `.\gradlew.bat test`

---

<div align="right">

Weave-Loader is licensed under the [GNU General Public License Version 3][license]. 

</div>

[jdk]:     https://www.azul.com/downloads/?version=java-17-lts&package=jdk
[git]:     https://git-scm.com/
[license]: https://github.com/Weave-MC/Weave-Loader/blob/main/LICENSE
