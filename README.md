<img
    align="right" alt="title" width="200px"
    src="https://raw.githubusercontent.com/Weave-MC/.github/master/assets/icon.png"
/>

# Weave Loader, A Universal Minecraft Mod Loader

Weave Loader is a tool designed to simplify the process of modding Minecraft. It allows
developers to easily create mods by providing a simple and intuitive API for interacting with Minecraft's codebase,
while injecting into clients that are somewhat closed-off to developers.

## Supported Clients / Versions

<table>
<tr><th>Supported Clients</th><th>Supported Versions</th></tr>
<tr><td>
        
| Client  | Supported |
| ------- | :-------: |
| Vanilla | :x: |
| Forge   | :white_check_mark: |
| Lunar   | :white_check_mark: |
| Badlion | :x: |
| Feather | :x: |
| Labymod | :white_check_mark: |

</td><td>
    
| Version | Supported |
| ------- | :-------: |
| 1.7     | :white_check_mark: |
| 1.8     | :white_check_mark: |
| 1.12    | :white_check_mark: |
| 1.16    | :white_check_mark: |
| 1.17    | :white_check_mark: |
| 1.18    | :white_check_mark: |
| 1.19    | :white_check_mark: |
| 1.20    | :white_check_mark: |

</td></tr>
</table>


## Installation

To install Weave-Loader, you can either download a pre-built release from
the [releases page](https://github.com/Weave-MC/Weave-Loader/releases), or build it yourself from source.

### Building with Gradle

- `git clone` the project, this can be achieved by installing [git][git], then running

```bash
git clone https://github.com/Weave-MC/Weave-Loader.git
git submodule update --init --recursive
```

- **UN*X**

```bash
cd Weave-Loader && chmod +x ./gradlew && ./gradlew agent
```

- **Windows**

```cmd
cd Weave-Loader && .\gradlew.bat agent
```

## Usage

To use Weave-Loader, you have two options:
1. Use [Weave-Manager](https://github.com/Weave-MC/Weave-Manager) to handle the process of attaching Weave to your preferred Minecraft client automatically.
2. Manually add the agent mentioned in the previous step to the JVM arguments when launching Minecraft. You need to include the follow argument: `-javaagent:$PATH_TO_AGENT`

Weave will automatically load mods from `~/.weave/mods/`.

## Contributing

We welcome contributions from anyone interested in improving Weave-Loader. If you find a bug or have an idea for a new
feature, feel free to submit a pull request.

---

<div align="right">

Weave-Loader is licensed under the [GNU General Public License v3.0][license].

</div>

[git]:     https://git-scm.com/

[license]: https://github.com/Weave-MC/Weave-Loader/blob/master/LICENSE
