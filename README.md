<div align="center">
    <h1>Avrix</h1>
</div>

<p align="center">
    <img alt="PZ Version" src="https://img.shields.io/badge/Project_Zomboid-v41.78.16-blue">
    <img alt="Java version" src="https://img.shields.io/badge/Java-17-orange">
    <img alt="GitHub License" src="https://img.shields.io/github/license/Brov3r/Avrix">
    <img alt="GitHub issues" src="https://img.shields.io/github/issues-raw/Brov3r/Avrix">
    <img alt="GitHub repo size" src="https://img.shields.io/github/repo-size/Brov3r/Avrix">
</p>

**Avrix** - This is a custom core wrapper for running the Project Zomboid game server with additional features.

Provides the ability to load plugins and expand basic functionality.

# How to use
1) Clone the repository
2) Create a Jar file of game dependencies through the Gradle task `createZombieJar`, after specifying the environment variable `ZOMBIE_FOLDER_PATH`
3) Build the core using the `shadowJar` task
4) Move the created Jar file to the root folder of the server
5) Copy the launch script `scripts/AvrixLauncher.bat` to the root folder with the server
6) Run `AvrixLauncher.bat`

# License
This project is licensed under `GNU GPLv3`.