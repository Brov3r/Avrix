package com.avrix;

import com.avrix.agent.AgentLoader;
import com.avrix.plugin.PluginManager;
import com.avrix.utils.PatchUtils;
import zombie.gameStates.MainScreenState;
import zombie.network.GameServer;

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

        PatchUtils.applyDefaultPatches();

        PluginManager.loadPlugins();

        switch (System.getProperty("avrix.mode")) {
            case "client" -> MainScreenState.main(args);
            case "server" -> GameServer.main(args);
        }
    }
}