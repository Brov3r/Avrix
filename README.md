<div align="center">
    <h1>Avrix</h1>
</div>

<p align="center">
    <img alt="PZ Version" src="https://img.shields.io/badge/Project_Zomboid-41.78.16-blue">
    <img alt="Java version" src="https://img.shields.io/badge/Java-17-orange">
    <img alt="Avrix Environment" src="https://img.shields.io/badge/Environment-client/server-green">
    <a href="https://discord.com/invite/PdYtyJMTZN"><img alt="Discord" src="https://img.shields.io/discord/1248698287997976656?logo=discord&logoColor=%23ffffff&logoSize=2&label=Discord&color=%235865F2"></a>
    <img alt="GitHub License" src="https://img.shields.io/github/license/Brov3r/Avrix">
    <img alt="GitHub issues" src="https://img.shields.io/github/issues-raw/Brov3r/Avrix">
    <img alt="GitHub repo size" src="https://img.shields.io/github/repo-size/Brov3r/Avrix">
    <img alt="GitHub Release" src="https://img.shields.io/github/v/release/brov3r/Avrix">
    <img alt="CodeQL" src="https://github.com/Brov3r/Avrix/actions/workflows/codeql.yml/badge.svg">
    <img alt="Java CI with Gradle" src="https://github.com/Brov3r/Avrix/actions/workflows/gradle.yml/badge.svg">
</p>

**Avrix** - a fundamentally new wrapper for running the Project Zomboid server and client with plugins (modifications).

To run the client/server with this wrapper, you must first
install [JDK 17 or higher](https://www.oracle.com/java/technologies/downloads/).

# Features

- Availability of [JavaDoc](https://brov3r.github.io/Avrix/), [Wiki](https://github.com/Brov3r/Avrix/wiki);
- Example of a [plugin](https://github.com/Brov3r/Avrix/tree/main/example-plugin);
- Working with the client and server side;
- Availability of tools for loading and working
  with [Lua scripts](https://brov3r.github.io/Avrix/com/avrix/lua/package-summary.html);
- The ability to easily add plugins by moving the Jar file to the plugins folder;
- The
  ability [to make changes to the bytecode](https://github.com/Brov3r/Avrix/blob/main/example-plugin/src/main/java/com/avrix/example/patches/PatchGameServer.java)
  of the game in runtime (similar to Minecraft Mixin);
- More than 200 standard and custom [events](https://brov3r.github.io/Avrix/com/avrix/events/package-summary.html) (with
  documentation) that you can easily subscribe to;
- Custom documented tools for working with game methods;
- Creating plugins for Avrix is very similar to creating plugins from Minecraft mod loaders (Paper, Fabric, Bukkit,
  etc.);

# How to use

## Assembled Jar

1) Download the executable Jar file and the necessary startup script from
   the [releases page](https://github.com/Brov3r/Avrix/releases )
2) Move the downloaded files to the root folder of the client/server
3) Run the script and follow the instructions displayed

## Self-assembly

1) Clone the repository
2) Create a Jar file of game dependencies through the Gradle task `buildZomboidDependencies`, after specifying the
   environment variables `ZOMBIE_FOLDER_PATH` and `GAME_FOLDER_PATH`
3) Build the Jar file using the `buildJar` task
4) Move the created Jar file to the root folder of the client/server
5) Copy the [`launch script`](./scripts) to the root folder of the client/server
6) Run launch script

# Contribute

We welcome any help in the development of this project! How can you help:

- **Test** - install the loader and just play. Mods are not necessary to install, so you will help find hidden bugs. If
  something turns up, please report it to [Issue](https://github.com/Brov3r/Avrix/issues) so that other developers know
  what needs to be fixed.
- **[Create plugins](https://github.com/search?q=avrix-loader&type=repositories)** - is a good way to develop our community and bring new functionality to the game.
- **Take part in the development** - you can always create a fork and make corrections by sending them for
  acceptance [pull request](https://github.com/Brov3r/Avrix/pulls).
- **Join our community at [Discord](https://discord.gg/PdYtyJMTZN)** - we want to create a healthy community of
  enthusiastic developers where everyone can learn something and find new friends.

# Disclaimer

This software is provided "as is", without warranty of any kind, express or implied, including but not limited to the
warranties of merchantability, fitness for a particular purpose, and noninfringement of third party rights. Neither the
author of the software nor its contributors shall be liable for any direct, indirect, incidental, special, exemplary, or
consequential damages (including, but not limited to, procurement of substitute goods or services; loss of use, data, or
profits; or business interruption) however caused and on any theory of liability, whether in contract, strict liability,
or tort (including negligence or otherwise) arising in any way out of the use of this software, even if advised of the
possibility of such damage.

By using this software, you agree to these terms and release the author of the software and its contributors from any
liability associated with the use of this software.

# License

This project is licensed under [GNU GPLv3](./LICENSE).
