package com.avrix.api.lua;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zombie.Lua.LuaManager;
import zombie.ZomboidFileSystem;
import zombie.network.CoopMaster;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for extending Project Zomboid's Lua scripting environment with custom Java-backed functionality.
 */
public final class LuaExtension {
    private static final Logger log = LoggerFactory.getLogger(LuaExtension.class);

    private static final Set<Class<?>> EXPOSED_CLASSES = ConcurrentHashMap.newKeySet();
    private static final Set<Object> EXPOSED_GLOBAL_OBJECTS = ConcurrentHashMap.newKeySet();

    /**
     * Reflective handle to the private {@code LuaManager.paths} field.
     * Initialized once in the static initializer.
     */
    private static Field luaManagerPathsField;

    static {
        try {
            luaManagerPathsField = LuaManager.class.getDeclaredField("paths");
            luaManagerPathsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            log.error("Failed to access the 'paths' field from LuaManager!", e);
        }
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private LuaExtension() {
    }

    /**
     * Searches for a folder inside the JAR archive, unpacks it into a temporary directory and load it.
     *
     * @param jarFile      JAR archive file
     * @param internalPath path inside the JAR (e.g. "media/lua")
     */
    public static void loadLuaFolderFromJar(File jarFile, String internalPath) {
        Objects.requireNonNull(jarFile, "JarFile must not be null");
        Objects.requireNonNull(internalPath, "InternalPath must not be null");

        if (!jarFile.exists()) {
            log.error("Jar file not found: {}", jarFile.getAbsolutePath());
            return;
        }

        Path normalized = Path.of(internalPath).normalize();
        if (internalPath.contains("..")) {
            log.error("Path '{}' traversal sequences are not allowed in internal path", internalPath);
            return;
        }

        try {
            URI jarUri = URI.create("jar:" + jarFile.toURI());
            try (FileSystem jarFs = FileSystems.newFileSystem(jarUri, Collections.emptyMap())) {
                Path sourceRoot = jarFs.getPath(normalized.toString());
                if (!Files.exists(sourceRoot)) {
                    log.error("Internal path '{}' not found in Jar: {}", internalPath, jarFile.getName());
                    return;
                }

                Path tempDir = Files.createTempDirectory("avrix_jar_lua_");
                try {
                    try (Stream<Path> walk = Files.walk(sourceRoot)) {
                        walk.forEach(source -> {
                            try {
                                Path relative = sourceRoot.relativize(source);
                                Path target = tempDir.resolve(relative.toString());
                                if (Files.isDirectory(source)) {
                                    Files.createDirectories(target);
                                } else {
                                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                                }
                            } catch (IOException e) {
                                log.error("Failed to extract JAR entry: {}", source, e);
                                return;
                            }
                        });
                    }
                    loadLuaFolder(tempDir, true);
                } finally {
                    deleteRecursivelyQuietly(tempDir);
                }
            }
        } catch (Exception e) {
            log.error("Failed to load Lua scripts from Jar file: {}", jarFile.getAbsolutePath(), e);
        }

    }

    /**
     * Recursively deletes a directory and all its contents, suppressing non-critical I/O errors.
     *
     * @param dir the root directory to delete; must be non-null
     */
    private static void deleteRecursivelyQuietly(Path dir) {
        if (!Files.exists(dir)) {
            log.trace("Cleanup skipped: path does not exist: {}", dir);
            return;
        }

        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                            log.trace("Deleted: {}", path);
                        } catch (IOException e) {
                            if (log.isDebugEnabled()) {
                                log.debug("Failed to delete temporary path (non-critical): {} – {}",
                                        path, e.getMessage());
                            } else {
                                log.warn("Failed to delete temporary path: {} – {}",
                                        path, e.getMessage());
                            }
                        } catch (SecurityException e) {
                            log.warn("Permission denied deleting temporary path: {}", path, e);
                        }
                    });
            log.trace("Cleanup completed for directory: {}", dir);
        } catch (IOException e) {
            log.error("Failed to traverse directory for cleanup: {}", dir, e);
        } catch (RuntimeException e) {
            log.error("Unexpected error during cleanup of: {}", dir, e);
        }
    }

    /**
     * Normalizes a filesystem path for consistent lookup by LuaManager.
     * <p>
     * Converts backslashes to forward slashes and lowercases the result
     * to match the internal path handling used by {@code searchFolders()}
     * and {@code require()}.
     *
     * @param path the {@link Path} to normalize
     * @return normalized path string using forward slashes and lowercase
     */
    private static String normalizePath(Path path) {
        return path.toString()
                .replace("\\", "/")
                .toLowerCase(Locale.ENGLISH);
    }

    /**
     * Loads and executes a single Lua script file.
     * <p>
     * The file is compiled and executed via {@link LuaManager#RunLua(String, boolean)}.
     * If {@code rewriteEvents} is {@code true}, the LuaCompiler will apply
     * event-rewriting transformations.
     *
     * @param luaFilePath   absolute path to the {@code .lua} file
     * @param rewriteEvents whether to enable event rewriting during compilation
     */
    public static void loadLua(Path luaFilePath, boolean rewriteEvents) {
        if (!Files.isRegularFile(luaFilePath)) {
            log.error("Not a valid Lua file: {}", luaFilePath.toAbsolutePath());
            return;
        }

        if (!luaFilePath.getFileName().toString().toLowerCase().endsWith(".lua")) {
            log.error("File does not have '.lua' extension: {}", luaFilePath);
            return;
        }

        log.trace("Loading custom Lua file: '{}' (rewriteEvents={})",
                luaFilePath.getFileName(), rewriteEvents);

        try {
            String normalizedPath = normalizePath(luaFilePath.toAbsolutePath());
            LuaManager.RunLua(normalizedPath, rewriteEvents);
            log.trace("Custom Lua file loaded successfully: {}", luaFilePath.getFileName());
        } catch (Exception e) {
            log.error("Failed to load Lua file: {}", luaFilePath.toAbsolutePath(), e);
        }
    }

    /**
     * Recursively loads all {@code .lua} files from a directory.
     * <p>
     * Files are processed in case-insensitive alphabetical order.
     *
     * @param folderPath    path to the directory containing Lua scripts
     * @param rewriteEvents whether to enable event rewriting during compilation
     */
    public static void loadLuaFolder(Path folderPath, boolean rewriteEvents) {
        if (!Files.isDirectory(folderPath)) {
            log.error("Failed to load Lua folder: '{}' is not a valid directory.", folderPath.toAbsolutePath());
            return;
        }

        List<Path> luaFiles = registerLuaContent(folderPath);

        if (luaFiles.isEmpty()) {
            log.warn("No Lua scripts found in directory: {}", folderPath);
            return;
        }

        log.info("Starting execution of {} scripts from folder: {}", luaFiles.size(), folderPath.getFileName());

        for (Path luaFile : luaFiles) {
            loadLua(luaFile, rewriteEvents);
            CoopMaster.instance.update();
        }
    }

    /**
     * Registers a path or file for Lua module resolution and indexes its contents.
     * <p>
     * If the given path is a directory, it is added to {@code LuaManager.paths}
     * (via reflection) so that {@code require()} can resolve modules from it.
     * All {@code .lua} files under the path are also registered in
     * {@link ZomboidFileSystem#activeFileMap} for direct lookup.
     * <p>
     * Paths are normalized using {@link #normalizePath(Path)} before registration.
     *
     * @param path the file or directory path to register
     * @return list of discovered {@code .lua} files (sorted, case-insensitive),
     * or empty list if registration failed or no files were found
     */
    public static List<Path> registerLuaContent(Path path) {
        if (!Files.exists(path)) {
            log.error("Aborting Lua registration: Path does not exist at '{}'", path.toAbsolutePath());
            return Collections.emptyList();
        }

        if (luaManagerPathsField == null) {
            log.error("Internal Error: Cannot register Lua content because 'LuaManager.paths' field is inaccessible.");
            return Collections.emptyList();
        }

        try {
            @SuppressWarnings("unchecked")
            ArrayList<String> paths = (ArrayList<String>) luaManagerPathsField.get(null);

            if (paths == null) {
                log.error("Internal Error: 'LuaManager.paths' instance is null. Is the game initialized?");
                return Collections.emptyList();
            }

            Path folderPath = Files.isDirectory(path) ? path : path.getParent();
            String normalizedRoot = normalizePath(folderPath);

            if (!normalizedRoot.endsWith("/")) {
                normalizedRoot += "/";
            }

            if (!paths.contains(normalizedRoot)) {
                paths.add(normalizedRoot);
                log.trace("New Lua search directory registered: [{}]", normalizedRoot);
            }

            List<Path> foundFiles = findLuaFiles(path);
            for (Path lua : foundFiles) {
                String normalizedFile = normalizePath(lua);
                ZomboidFileSystem.instance.activeFileMap.put(normalizedFile, normalizedFile);
            }

            if (!foundFiles.isEmpty()) {
                log.trace("Indexed {} Lua files from folder: {}", foundFiles.size(), path.getFileName());
            } else {
                log.warn("No Lua files found to register in: {}", path);
            }

            return foundFiles;
        } catch (Exception e) {
            log.error("Critical failure during Lua registration for path: {}", path, e);
            return Collections.emptyList();
        }
    }

    /**
     * Recursively searches for all {@code .lua} files under a directory.
     * <p>
     * Results are sorted case-insensitively to ensure deterministic loading order.
     * I/O errors during traversal are logged and result in an empty list.
     *
     * @param folder the root directory to search
     * @return sorted list of {@code .lua} file paths, or empty list on error
     */
    private static List<Path> findLuaFiles(Path folder) {
        try (Stream<Path> pathStream = Files.walk(folder)) {
            return pathStream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".lua"))
                    .sorted(Comparator.comparing(Path::toString, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
        } catch (RuntimeException | IOException e) {
            log.error("Error when searching for Lua files in the path: {}", folder, e);
        }
        return Collections.emptyList();
    }

    /**
     * Registers a Java class for exposure to the Lua scripting environment.
     * <p>
     * The class will be processed during the next {@code Exposer.exposeAll()} call.
     * Only public methods and fields of the class will be accessible from Lua,
     * subject to the exposure rules of {@link LuaManager.Exposer}.
     *
     * @param clazz the {@link Class} to expose; must be non-null
     */
    public static void addExposedClass(Class<?> clazz) {
        if (clazz != null) {
            EXPOSED_CLASSES.add(clazz);
        }
    }

    /**
     * Registers an object instance to provide global Lua functions.
     * <p>
     * The object's public methods annotated with {@code @LuaMethod(global = true)}
     * will become directly callable from Lua scripts. This is typically used for
     * utility objects or API entry points that should not require namespacing.
     *
     * @param globalObject the object instance to expose; must be non-null
     */
    public static void addExposedGlobalObject(Object globalObject) {
        if (globalObject != null) {
            EXPOSED_GLOBAL_OBJECTS.add(globalObject);
        }
    }

    /**
     * Returns an unmodifiable view of classes scheduled for Lua exposure.
     *
     * @return unmodifiable set of exposed classes; never {@code null}
     * @see #addExposedClass(Class)
     */
    public static Set<Class<?>> getExposedClasses() {
        return Collections.unmodifiableSet(EXPOSED_CLASSES);
    }

    /**
     * Returns an unmodifiable view of objects scheduled to provide global Lua functions.
     *
     * @return unmodifiable set of exposed global objects; never {@code null}
     * @see #addExposedGlobalObject(Object)
     */
    public static Set<Object> getExposedGlobalObjects() {
        return Collections.unmodifiableSet(EXPOSED_GLOBAL_OBJECTS);
    }
}