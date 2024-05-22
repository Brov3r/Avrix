# Avrix

Custom core wrapper for the Project Zomboid server.

Provides the ability to download plugins and expand basic functionality.

# How to use
1) Clone the repository
2) Create a Jar file of game dependencies through the Gradle task `createZombieJar`, after specifying the environment variable `ZOMBIE_FOLDER_PATH`
3) Build the kernel using the `shadowJar` task
4) Move the created Jar file to the root folder of the game
5) Copy the launch script `scripts/AvrixLauncher.bat` to the root folder with the game
6) Run `AvrixLauncher.bat`

# License
This project is licensed under `GNU GPLv3`.