package com.avrix.bootstrap;

import com.avrix.enums.Environment;
import com.avrix.loaders.CoreClassLoader;
import com.avrix.provider.GameProvider;
import com.avrix.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class DefaultBootstrap implements Bootstrap {
    private static final Logger log = LoggerFactory.getLogger(DefaultBootstrap.class);
    private GameProvider provider;
    private CoreClassLoader classLoader;

    private static boolean initialized;
    private static boolean launched;

    /**
     * Initializes the bootstrap with the specified {@link GameProvider}.
     * Must be called before {@link #launch(String[])}.
     *
     * @param gameProvider the {@link GameProvider} to initialize with
     * @throws IllegalStateException if already initialized
     */
    @Override
    public void initialize(GameProvider gameProvider) {
        if (initialized) return;

        log.info("Initializing Bootstrap...");

        try {
            if (classLoader == null) {
                Path rootDir = getRootDirectory();
                log.debug("Resolved root directory: '{}'", rootDir);

                URL[] urls = findRootJars(rootDir);
                log.info("Discovered {} root JAR(s).", urls.length);

                classLoader = new CoreClassLoader(urls, DefaultBootstrap.class.getClassLoader());
            }

            provider = gameProvider;
            provider.initialize(classLoader);
            provider.redirectSystemStreamsToLogger();

            initialized = true;

            log.info("Bootstrap initialized successfully. Provider='{}', Environment={}",
                    provider.getId(), provider.getEnvironment());

        } catch (Exception e) {
            initialized = false;
            launched = false;
            provider = null;
            classLoader = null;

            throw new RuntimeException("Bootstrap initialization failed", e);
        }
    }

    /**
     * Launches the loader with the given command line arguments.
     *
     * @param args command line arguments
     * @throws IllegalStateException if not initialized
     */
    @Override
    public void launch(String[] args) {
        if (launched) return;

        try {
            provider.launch((args == null) ? new String[0] : args);

            launched = true;
        } catch (Exception e) {
            launched = false;
            throw new RuntimeException("Failed to launch Boostrap!", e);
        }
    }

    /**
     * Returns the class loader used by this bootstrap.
     *
     * @return the {@link URLClassLoader} instance
     */
    @Override
    public URLClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Returns the {@link GameProvider} associated with this bootstrap.
     *
     * @return the {@link GameProvider} instance
     */
    @Override
    public GameProvider getProvider() {
        return provider;
    }

    /**
     * Returns the display name of the loader.
     *
     * @return loader name
     */
    @Override
    public String getName() {
        return Constants.LOADER_NAME;
    }

    /**
     * Returns the unique identifier of the loader.
     *
     * @return loader ID
     */
    @Override
    public String getId() {
        return Constants.LOADER_ID;
    }

    /**
     * Returns the author(s) of the loader.
     *
     * @return author information
     */
    @Override
    public String getAuthor() {
        return Constants.LOADER_AUTHOR;
    }

    /**
     * Returns the license under which the loader is distributed.
     *
     * @return license information
     */
    @Override
    public String getLicense() {
        return Constants.LOADER_LICENSE;
    }

    /**
     * Returns the version of the loader.
     *
     * @return version string
     */
    @Override
    public String getVersion() {
        return Constants.LOADER_VERSION;
    }

    /**
     * Returns contact information for the loader.
     *
     * @return contact details
     */
    @Override
    public String getContacts() {
        return Constants.LOADER_CONTACTS;
    }

    /**
     * Returns the current {@link Environment} ({@link Environment#CLIENT} or {@link Environment#SERVER}).
     *
     * @return {@link Environment} type
     */
    @Override
    public Environment getEnvironment() {
        if (provider == null || provider.getEnvironment() == Environment.BOTH) return Environment.CLIENT;

        return provider.getEnvironment();
    }

    /**
     * Resolves the root directory of the loader distribution.
     *
     * <p>
     * The root directory is derived from the location of the {@link DefaultBootstrap} code source:
     * parent directory of the loader JAR (or classes directory).
     * </p>
     *
     * @return root directory path
     * @throws IllegalStateException if the code source cannot be resolved
     */
    private static Path getRootDirectory() {
        try {
            CodeSource codeSource = DefaultBootstrap.class.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                throw new IllegalStateException("CodeSource is null for LoaderManager.");
            }

            Path jarOrDir = Paths.get(codeSource.getLocation().toURI()).toAbsolutePath().normalize();
            Path parent = jarOrDir.getParent();
            if (parent == null) {
                throw new IllegalStateException("Cannot resolve root directory from: " + jarOrDir);
            }
            return parent;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve root directory for LoaderManager.", e);
        }
    }

    /**
     * Discovers JAR files located directly in the given root directory (depth 1).
     *
     * @param rootDir root directory
     * @return array of jar URLs (possibly empty)
     * @throws NullPointerException if {@code rootDir} is {@code null}
     * @throws Exception            if directory cannot be listed
     */
    private static URL[] findRootJars(Path rootDir) throws Exception {
        Objects.requireNonNull(rootDir, "Root directory must not be null");

        try (Stream<Path> stream = Files.list(rootDir)) {
            List<URL> urls = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(Constants.JAR_EXTENSION))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .map(p -> {
                        try {
                            return p.toUri().toURL();
                        } catch (Exception e) {
                            throw new IllegalStateException("Failed to convert path to URL: " + p, e);
                        }
                    })
                    .toList();

            log.debug("Root JARs: {}", urls.stream()
                    .map((url -> {
                        try {
                            return Paths.get(url.toURI()).getFileName().toString();
                        } catch (URISyntaxException e) {
                            return url.toExternalForm();
                        }
                    }))
                    .toList());

            if (urls.size() > 30) {
                log.debug("Root JAR list truncated. Total={}", urls.size());
            }

            return urls.toArray(URL[]::new);
        }
    }
}