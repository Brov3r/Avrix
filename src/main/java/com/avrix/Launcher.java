package com.avrix;

import com.avrix.agent.AgentLoader;
import com.avrix.agent.ClassTransformer;
import zombie.ZomboidFileSystem;
import zombie.gameStates.MainScreenState;
import zombie.network.GameServer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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

        applyPatches();

        ZomboidFileSystem.instance.setCacheDir(Paths.get("zomboid").toString());

        switch (System.getProperty("avrix.mode")) {
            case "client" -> MainScreenState.main(args);
            case "server" -> GameServer.main(args);
        }
    }

    /**
     * Applying patches to game files.
     * Searches for patches in the {@link com.avrix.patches} package as descendants of {@link ClassTransformer},
     * calls class modification methods and applies them at runtime.
     *
     * @throws IOException        if an I/O error occurs when reading the JAR file.
     * @throws URISyntaxException if a string could not be parsed as a URI reference.
     */
    private static void applyPatches() throws IOException, URISyntaxException {
        File jarFile = new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        URL jarUrl = jarFile.toURI().toURL();

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl}, Launcher.class.getClassLoader())) {
            JarURLConnection jarConnection = (JarURLConnection) new URL("jar:file:" + jarFile + "!/").openConnection();
            JarFile jar = jarConnection.getJarFile();
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class") && entry.getName().startsWith("com/avrix/patches/")) {
                    String className = entry.getName().replace("/", ".").replace(".class", "");

                    try {
                        Class<?> clazz = classLoader.loadClass(className);
                        if (ClassTransformer.class.isAssignableFrom(clazz)) {
                            ClassTransformer patchInstance = (ClassTransformer) clazz.getDeclaredConstructor().newInstance();
                            patchInstance.modifyClass();
                            patchInstance.applyModifications();
                        }
                    } catch (ClassNotFoundException e) {
                        System.err.println("[!] Class not found: " + className);
                        throw e;
                    } catch (InstantiationException e) {
                        System.err.println("[!] Failed to instantiate patcher class: " + className);
                        throw e;
                    } catch (IllegalAccessException e) {
                        System.err.println("[!] Illegal access when instantiating patcher class: " + className);
                        throw e;
                    } catch (NoSuchMethodException e) {
                        System.err.println("[!] No default constructor found for patcher class: " + className);
                        throw e;
                    } catch (InvocationTargetException e) {
                        System.err.println("[!] Error occurred while invoking constructor for class: '" + className + "', cause: " + e.getCause());
                        throw e;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[!] IO exception occurred while accessing the JAR file: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("[!] Critical error when trying to apply patches to the game! Reason: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}