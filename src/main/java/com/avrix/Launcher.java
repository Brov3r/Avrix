package com.avrix;

import zombie.ZomboidFileSystem;
import zombie.network.GameServer;

import java.nio.file.Paths;

public class Launcher {
    public static void launch(String[] args) {
        ZomboidFileSystem.instance.setCacheDir(Paths.get("zomboid").toString());
        GameServer.main(args);
    }
}
