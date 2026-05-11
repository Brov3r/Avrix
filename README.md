<div align="center">
    <img alt="Header" width="100%" src="./assets/readme_header.svg" />
</div>

<p align="center">
    <img alt="PZ Version" src="https://img.shields.io/badge/Project_Zomboid-beta 42+-blue">
    <img alt="Java version" src="https://img.shields.io/badge/Java-25-orange">
    <img alt="Avrix Environment" src="https://img.shields.io/badge/Environment-client/server-green">
    <a href="https://discord.com/invite/PdYtyJMTZN"><img alt="Discord" src="https://img.shields.io/discord/1248698287997976656?logo=discord&logoColor=%23ffffff&logoSize=2&label=Discord&color=%235865F2"></a>
    <img alt="GitHub License" src="https://img.shields.io/github/license/Brov3r/Avrix">
</p>

**Avrix** - is a modern Java-based mod(plugin) loader for Project Zomboid (client and server), powered by mixin
technology.

> [!NOTE]
> Avrix Loader is a plugin and mixin loader only — it does not provide a Project Zomboid API.
>
> All game-specific utilities, events, and helpers are shipped in the optional [Avrix-API](./avrix-api) plugin.

## ✨ Features

* Powered by [mixin technology](https://github.com/SpongePowered/Mixin) (implementation
  via [ClassTransform](https://github.com/Lenni0451/ClassTransform))
* Designed for both client and server environments
* Simple mod installation: just drop the JAR file into the `plugins/` folder
* Streamlined mod development, following the same patterns as [FabricMC](https://github.com/FabricMC/fabric-loader)
* Essential API, events, commands, and utilities for mod development — [Avrix-API](./avrix-api)

## 📚 WIKI

In the documentation section, you can select the language that suits you and find out how to quickly start working with Avrix.

[📄Open the documentation](./wiki/wiki-language.md "Wiki")

## 🚀Quick Start

1) ⬇️ Download the executable Jar file from
   the [releases page](https://github.com/Brov3r/Avrix/releases "Release")
2) 📁 Move the downloaded files to the root folder of the client/server
3) 💡 Run the downloaded `JAR file` using the CMD run command:

```bash
java "-Djdk.attach.allowAttachSelf=true" -XX:+EnableDynamicAgentLoading -jar ./Avrix-Loader-X.X.X.jar
```

## 🤝 Contribute

We welcome any help in the development of this project! How can you help:

- 🧪 **Test** - install the loader and just play. Mods are not necessary to install, so you will help find hidden bugs.
  If something turns up, please report it to [Issue](https://github.com/Brov3r/Avrix/issues) so that other developers
  know what needs to be fixed.
- 🔌 **[Create plugins](https://github.com/search?q=avrix-loader&type=repositories)** - is a good way to develop our
  community and bring new functionality to the game.
- 💻 **Take part in the development** - you can always create a fork and make corrections by sending them for
  acceptance [pull request](https://github.com/Brov3r/Avrix/pulls).
- 💬 **Join our community at [Discord](https://discord.gg/PdYtyJMTZN)** - we want to create a healthy community of
  enthusiastic developers where everyone can learn something and find new friends.

## ⚖️ License

This project is licensed under [MIT license](./LICENSE).
