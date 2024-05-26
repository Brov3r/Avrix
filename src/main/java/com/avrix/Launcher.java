package com.avrix;

import com.avrix.agent.AgentLoader;
import com.avrix.agent.Patcher;
import javassist.CannotCompileException;
import zombie.ZomboidFileSystem;
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
     */
    public static void main(String[] args) {
        AgentLoader.loadAgent();

        Patcher patch = new Patcher.PatcherBuilder("zombie.network.GameServer").
                patchMethod("main", (ctClass, ctMethod) -> {
                    try {
                        ctMethod.insertBefore("{System.out.println(\"[#] First.\");}");
                    } catch (CannotCompileException e) {
                        throw new RuntimeException(e);
                    }
                }).
                patchMethod("main", (ctClass, ctMethod) -> {
                    try {
                        ctMethod.insertBefore("{System.out.println(\"[#] Second.\");}");
                    } catch (CannotCompileException e) {
                        throw new RuntimeException(e);
                    }
                }).build();
        patch.applyPatch();

        ZomboidFileSystem.instance.setCacheDir(Paths.get("zomboid").toString());
        GameServer.main(args);
    }
}
