package com.avrix;

import com.avrix.agent.AgentLoader;
import com.avrix.plugin.PluginManager;
import com.avrix.utils.PatchManager;
import zombie.ZomboidFileSystem;
import zombie.gameStates.MainScreenState;
import zombie.network.GameServer;

import java.nio.file.Paths;

/**
 * The Launcher class serves as the entry point for the application.
 */
public class Launcher {
    /**
     * The main method is the entry point of the Java application.
     *
     * @param args Command-line arguments passed to the application.
     * @throws Exception if a critical error occurs that prevents startup
     */
    public static void main(String[] args) throws Exception {
        AgentLoader.loadAgent();

        PatchManager.applyDefaultPatches();

        PluginManager.loadPlugins();

        ZomboidFileSystem.instance.setCacheDir(Paths.get("zomboid").toString());

        switch (System.getProperty("avrix.mode")) {
            case "client" -> MainScreenState.main(args);
            case "server" -> GameServer.main(args);
        }
    }
}