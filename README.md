<div align="center">
    <img alt="Header" width="100%" src="./assets/readme_header.svg" />
</div>

<p align="center">
    <a href="https://github.com/Brov3r/Avrix/actions/workflows/gradle.yml"><img alt="Build" src="https://img.shields.io/github/actions/workflow/status/Brov3r/Avrix/gradle.yml?branch=main&label=build"></a>
    <a href="https://github.com/Brov3r/Avrix/actions/workflows/codeql.yml"><img alt="CodeQL" src="https://img.shields.io/github/actions/workflow/status/Brov3r/Avrix/codeql.yml?branch=main&label=CodeQL"></a>
    <img alt="PZ Version" src="https://img.shields.io/badge/Project_Zomboid-b42+-blue">
    <img alt="Java Version" src="https://img.shields.io/badge/Java-25-orange">
    <img alt="Environment" src="https://img.shields.io/badge/Environment-Client%20%7C%20Server-green">
    <a href="https://discord.gg/PdYtyJMTZN"><img alt="Discord" src="https://img.shields.io/discord/1248698287997976656?logo=discord&logoColor=%23ffffff&label=Discord&color=%235865F2"></a>
    <img alt="GitHub License" src="https://img.shields.io/github/license/Brov3r/Avrix">
</p>

**Avrix** is a modern, modular Java-based mod loader for Project Zomboid (client and dedicated server), powered by
on-the-fly mixin bytecode transformation.

**Steam Workshop:** [Avrix on Steam Workshop](https://steamcommunity.com/sharedfiles/filedetails/?id=3833849848)

## ✨ Features

* **Mixin Technology:** Native support for Sponge-style mixin annotations powered
  by [ClassTransform](https://github.com/Lenni0451/ClassTransform) and ASM bytecode manipulation.
* **Unified Classloading:** Flat knot-classloader architecture ensuring zero classloader visibility barriers and fast
  class resolution.
* **Client and Server:** Supports graphical clients and headless dedicated servers on Windows and Linux. macOS code
  paths are experimental; running Project Zomboid with Avrix on macOS has not been verified.
* **Automated Dependency Resolution:** Strict semantic versioning (SemVer) validation and DAG topological sort for
  plugin load order.
* **Zero-Friction Modding:** Drop plugin `.jar` files into the `plugins/` directory to load them.

## 📚 Documentation

Visit the official documentation to choose your preferred language and get detailed guides on installation,
configuration, and plugin development:

[📄 Open Documentation](./wiki/wiki-language.md "Avrix Wiki")

## 🚀 Quick Start

1. ⬇️ Download the latest executable JAR from the [Releases Page](https://github.com/Brov3r/Avrix/releases), or
   subscribe to the [Steam Workshop item](https://steamcommunity.com/sharedfiles/filedetails/?id=3833849848).
2. 📁 Place the `Avrix-Loader-<version>.jar` file into the root folder of your Project Zomboid installation (client or
   dedicated server).
3. 💡 Change the working directory to the game installation root, then launch via the command line or a custom script:

```bash
cd "/path/to/ProjectZomboid"
java -jar ./Avrix-Loader-x.x.x.jar
```

On PowerShell, use `Set-Location "C:\path\to\ProjectZomboid"` before running Java. The JVM working directory
must be the actual game root (inside the application bundle on macOS). `-Davrix.game.dir` selects the installation;
it does not change the process working directory. Relative plugin directories are resolved against the game root.

*For dedicated servers with Steam integration disabled, append `-nosteam`:*

```bash
java -jar ./Avrix-Loader-x.x.x.jar -nosteam
```

> [!NOTE]
> **Logging Level:** You can adjust the loader log verbosity using the `-Dconsole.level=TRACE` (or `DEBUG`, `INFO`,
`WARN`, `ERROR`) JVM flag: ``java -Dconsole.level=TRACE -jar ./Avrix-Loader-x.x.x.jar``
>
>
> **Raw Game Logs:** Pass the `--no-redirect-log` flag to disable Avrix stream interception and restore default Project
> Zomboid console output: ``java -jar ./Avrix-Loader-x.x.x.jar --no-redirect-log``

## 🧪 Running from an IDE

For local development, Gradle provides two run configurations:

```bash
./gradlew runClient
./gradlew runServer
```

The tasks look for common Steam installation paths on Windows and Linux, with experimental macOS path detection.
macOS game launches remain unverified. Override the paths with Gradle
properties or environment variables when the game is installed elsewhere:

```bash
# Windows
./gradlew runClient -PpzClientDir="C:\Program Files (x86)\Steam\steamapps\common\ProjectZomboid"
./gradlew runServer -PpzServerDir="C:\Program Files (x86)\Steam\steamapps\common\Project Zomboid Dedicated Server"

# Linux
./gradlew runClient -PpzClientDir="$HOME/.steam/steam/steamapps/common/ProjectZomboid"

# macOS (experimental, game launch unverified)
./gradlew runClient -PpzClientDir="$HOME/Library/Application Support/Steam/steamapps/common/ProjectZomboid"
```

Supported environment variables: `PZ_CLIENT_DIR` and `PZ_SERVER_DIR`.

`./gradlew check` runs unit/integration tests, Javadoc validation, and `smokeTest` against the shaded JAR, including
the approval subprocess in headless mode. CI runs on Windows and Linux; these checks do not validate an actual game
launch or macOS compatibility. Run `./gradlew smokeTest` to check the packaged runtime separately.

The client/server environment itself is detected from installation files only. The loader does not require an
environment flag.

## 📊 Performance Benchmark

A lightweight comparative benchmark is available without adding JMH or extra dependencies:

```bash
./gradlew benchmark
```

It measures dependency resolution, metadata-only plugin loading, realistic plugin loading with entrypoints and
resources, and class loading with mixin transformation. Results are intended for regression comparison on the same
machine, not as absolute performance guarantees.

## 🤝 Contributing

Contributions are always welcome! Here is how you can help:

- 🧪 **Testing:** Install Avrix and run your server or client. If you encounter an unexpected crash or issue, please
  report it on [GitHub Issues](https://github.com/Brov3r/Avrix/issues).
- 🔌 **Build Plugins:** Create new mods and plugins for the community and share them on GitHub.
- 💻 **Code Contributions:** Fork the repository, create a feature branch, and submit
  a [Pull Request](https://github.com/Brov3r/Avrix/pulls).
- 💬 **Join Discord:** Connect with other modders, get support, and discuss development in
  our [Discord Community](https://discord.gg/PdYtyJMTZN).

## ⚖️ License

This project is licensed under the [MIT License](./LICENSE).
