<div align="center">
    <img alt="Header" width="100%" src="./assets/readme_header.svg" />
</div>

<p align="center">
    <img alt="PZ Version" src="https://img.shields.io/badge/Project_Zomboid-b42+-blue">
    <img alt="Java Version" src="https://img.shields.io/badge/Java-25-orange">
    <img alt="Environment" src="https://img.shields.io/badge/Environment-Client%20%7C%20Server-green">
    <a href="https://discord.gg/PdYtyJMTZN"><img alt="Discord" src="https://img.shields.io/discord/1248698287997976656?logo=discord&logoColor=%23ffffff&label=Discord&color=%235865F2"></a>
    <img alt="GitHub License" src="https://img.shields.io/github/license/Brov3r/Avrix">
</p>

**Avrix** is a modern, modular Java-based mod loader for Project Zomboid (client and dedicated server), powered by
on-the-fly mixin bytecode transformation.

## ✨ Features

* **Mixin Technology:** Native support for Sponge-style mixin annotations powered
  by [ClassTransform](https://github.com/Lenni0451/ClassTransform) and ASM bytecode manipulation.
* **Unified Classloading:** Flat knot-classloader architecture ensuring zero classloader visibility barriers and fast
  class resolution.
* **Universal Environment:** Works out of the box on both graphical clients and headless dedicated servers (Windows,
  Linux, macOS).
* **Automated Dependency Resolution:** Strict semantic versioning (SemVer) validation and DAG topological sort for
  plugin load order.
* **Zero-Friction Modding:** Drop plugin `.jar` files into the `plugins/` directory to load them.

## 📚 Documentation

Visit the official documentation to choose your preferred language and get detailed guides on installation,
configuration, and plugin development:

[📄 Open Documentation](./wiki/wiki-language.md "Avrix Wiki")

## 🚀 Quick Start

1. ⬇️ Download the latest executable JAR from the [Releases Page](https://github.com/Brov3r/Avrix/releases).
2. 📁 Place the `Avrix-Loader-<version>.jar` file into the root folder of your Project Zomboid installation (client or
   dedicated server).
3. 💡 Launch the game via the command line or custom startup script:

```bash
java -jar ./Avrix-Loader-2.1.0.jar
```

*For dedicated servers with Steam integration disabled, append `-nosteam`:*

```bash
java -jar ./Avrix-Loader-2.1.0.jar -nosteam
```

> [!NOTE]
> **Logging Level:** You can adjust the loader log verbosity using the `-Dconsole.level=TRACE` (or `DEBUG`, `INFO`,
`WARN`, `ERROR`) JVM flag: ``java -Dconsole.level=TRACE -jar ./Avrix-Loader-2.1.0.jar``
>
>
> **Raw Game Logs:** Pass the `--no-redirect-log` flag to disable Avrix stream interception and restore default Project
> Zomboid console output: ``java -jar ./Avrix-Loader-2.1.0.jar --no-redirect-log``

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