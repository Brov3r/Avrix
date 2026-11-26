package com.avrix.core;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test.game.TargetGameService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for reading a JAR that is not stored on the local file system.
 */
@DisplayName("KnotClassLoader remote JAR reading")
class KnotClassLoaderRemoteJarTest {

    private static final ClassLoader PARENT = ClassLoader.getPlatformClassLoader();

    @TempDir
    private Path tempDir;

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    @Test
    @DisplayName("Should load a class from a JAR served over HTTP")
    void shouldLoadClassFromRemoteJar() throws Exception {
        Path jar = createClassJar("remote.jar");
        byte[] jarBytes = Files.readAllBytes(jar);
        server = startJarServer(jarBytes);

        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/remote.jar";
        try (KnotClassLoader loader = new KnotClassLoader(
                new URL[]{URI.create(baseUrl).toURL()}, PARENT)) {
            Class<?> loaded = loader.loadClass(TargetGameService.class.getName());

            assertThat(loaded.getClassLoader()).isSameAs(loader);
            assertThat(loaded.getDeclaredConstructor().newInstance()).isNotNull();
            assertThat(loaded.getProtectionDomain().getCodeSource().getLocation().toExternalForm())
                    .isEqualTo(baseUrl);
        }
    }

    private HttpServer startJarServer(byte[] jarBytes) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        httpServer.createContext("/remote.jar", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/java-archive");
            exchange.sendResponseHeaders(200, jarBytes.length);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(jarBytes);
            }
        });
        httpServer.start();
        return httpServer;
    }

    private Path createClassJar(String name) throws IOException {
        Path jar = tempDir.resolve(name);
        String resourcePath = TargetGameService.class.getName().replace('.', '/') + ".class";
        try (InputStream classBytes = TargetGameService.class.getClassLoader().getResourceAsStream(resourcePath);
                JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            if (classBytes == null) {
                throw new IOException("Class resource not found: " + resourcePath);
            }
            output.putNextEntry(new JarEntry(resourcePath));
            output.write(classBytes.readAllBytes());
            output.closeEntry();
        }
        return jar;
    }
}
