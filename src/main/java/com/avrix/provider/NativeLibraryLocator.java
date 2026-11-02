package com.avrix.provider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Resolves native-library directories for Project Zomboid installations.
 */
public final class NativeLibraryLocator {

    private final String osName;
    private final String osArch;

    /**
     * Constructs a locator using the current JVM operating-system properties.
     */
    public NativeLibraryLocator() {
        this(System.getProperty("os.name", ""), System.getProperty("os.arch", ""));
    }

    /**
     * Constructs a locator for the provided operating-system properties.
     *
     * @param osName operating-system name
     * @param osArch operating-system architecture
     */
    public NativeLibraryLocator(String osName, String osArch) {
        this.osName = (osName == null ? "" : osName).toLowerCase(Locale.ROOT);
        this.osArch = (osArch == null ? "" : osArch).toLowerCase(Locale.ROOT);
    }

    /**
     * Resolves native-library directories for a Project Zomboid installation.
     *
     * @param launchDirectory game installation root
     * @param server          whether the installation is a dedicated server
     * @return absolute, normalized, deduplicated native-library directories
     */
    public List<Path> locate(Path launchDirectory, boolean server) {
        if (launchDirectory == null || !Files.isDirectory(launchDirectory)) {
            return List.of();
        }

        Path normalizedLaunchDirectory = launchDirectory.toAbsolutePath().normalize();
        Set<Path> nativePaths = new LinkedHashSet<>();
        nativePaths.add(normalizedLaunchDirectory);

        Path commonNatives = normalizedLaunchDirectory.resolve("natives");
        if (Files.isDirectory(commonNatives)) {
            nativePaths.add(commonNatives);
        }

        if (server) {
            collectServerNatives(normalizedLaunchDirectory, nativePaths);
        } else {
            collectClientNatives(normalizedLaunchDirectory, nativePaths);
        }

        return List.copyOf(nativePaths);
    }

    private void collectServerNatives(Path launchDirectory, Set<Path> nativeCollector) {
        Path serverNativesRoot = launchDirectory.resolve("natives");
        if (!Files.isDirectory(serverNativesRoot)) {
            return;
        }

        nativeCollector.add(serverNativesRoot);
        try (var stream = Files.list(serverNativesRoot)) {
            stream.filter(Files::isDirectory)
                    .map(Path::toAbsolutePath)
                    .map(Path::normalize)
                    .forEach(nativeCollector::add);
        } catch (Exception _) {
            // Keep the already discovered paths; native discovery is best-effort.
        }
    }

    private void collectClientNatives(Path launchDirectory, Set<Path> nativeCollector) {
        for (Path path : resolveClientNativeCandidates(launchDirectory)) {
            if (Files.isDirectory(path)) {
                nativeCollector.add(path.toAbsolutePath().normalize());
            }
        }
    }

    private List<Path> resolveClientNativeCandidates(Path launchDirectory) {
        boolean is64Bit = osArch.contains("64") || osArch.contains("aarch64");
        boolean isArm64 = osArch.contains("aarch64") || osArch.contains("arm64");

        List<Path> candidates = new ArrayList<>();
        if (osName.contains("win")) {
            candidates.add(launchDirectory.resolve(isArm64 ? "winarm64" : is64Bit ? "win64" : "win32"));
            candidates.add(launchDirectory.resolve("natives").resolve(isArm64 ? "winarm64" : is64Bit ? "win64" : "win32"));
            candidates.add(launchDirectory.resolve("natives").resolve(isArm64 ? "windows-arm64" : "windows-x86_64"));
        } else if (osName.contains("linux")) {
            candidates.add(launchDirectory.resolve(isArm64 ? "linux-arm64" : is64Bit ? "linux64" : "linux32"));
            candidates.add(launchDirectory.resolve("natives").resolve(isArm64 ? "linux-arm64" : is64Bit ? "linux64" : "linux32"));
            candidates.add(launchDirectory.resolve("natives").resolve(isArm64 ? "linux-aarch64" : "linux-x86_64"));
        } else if (osName.contains("mac")) {
            candidates.add(launchDirectory.resolve(isArm64 ? "macarm64" : "mac64"));
            candidates.add(launchDirectory.resolve("natives").resolve(isArm64 ? "macarm64" : "mac64"));
            candidates.add(launchDirectory.resolve("natives").resolve("osx"));
            candidates.add(launchDirectory.resolve("natives").resolve("macos"));
        }
        return candidates;
    }
}
