package com.avrix.provider.zomboid.approval;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Discovers the languages shipped with the approval window.
 * <p>
 * Adding a translation requires no Java changes: put a {@code messages_<locale>.properties} file in the
 * {@code translations} directory and add a {@code language.name} entry to it. The root English bundle is always
 * available.
 */
public final class ApprovalLanguages {

    private static final String DEFAULT_CODE = "en";
    private static final String DEFAULT_FILE = "messages.properties";
    private static final String PACKAGE_PATH = "com/avrix/provider/zomboid/approval/translations/";
    private static final String BUNDLE_PREFIX = "messages_";
    private static final String BUNDLE_SUFFIX = ".properties";
    private static final String BUNDLE_NAME = "com.avrix.provider.zomboid.approval.translations.messages";
    private static final String DISPLAY_NAME_KEY = "language.name";
    private static final ResourceBundle.Control CONTROL =
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

    private static final List<ApprovalLanguage> AVAILABLE = loadAvailable();

    private ApprovalLanguages() {
        throw new UnsupportedOperationException("ApprovalLanguages utility class cannot be instantiated");
    }

    /**
     * Returns every discovered language with English first.
     *
     * @return immutable language list, never empty
     */
    public static List<ApprovalLanguage> available() {
        return AVAILABLE;
    }

    /**
     * Detects the language from the operating system locale.
     *
     * @return matching language, or English when the locale is not supported
     */
    public static ApprovalLanguage detect() {
        return detect(Locale.getDefault());
    }

    /**
     * Maps a locale to a discovered language, preferring an exact match and then a language-only match.
     *
     * @param locale locale to map
     * @return matching language, or English when the locale is not supported
     */
    public static ApprovalLanguage detect(Locale locale) {
        if (locale != null) {
            for (ApprovalLanguage language : AVAILABLE) {
                if (language.locale().equals(locale)) {
                    return language;
                }
            }
            for (ApprovalLanguage language : AVAILABLE) {
                if (language.locale().getLanguage().equalsIgnoreCase(locale.getLanguage())) {
                    return language;
                }
            }
        }
        return defaultLanguage();
    }

    /**
     * Returns the English language or the first discovered language as a last resort.
     *
     * @return default language
     */
    public static ApprovalLanguage defaultLanguage() {
        for (ApprovalLanguage language : AVAILABLE) {
            if (DEFAULT_CODE.equals(language.code())) {
                return language;
            }
        }
        return AVAILABLE.get(0);
    }

    private static List<ApprovalLanguage> loadAvailable() {
        Set<String> codes = new TreeSet<>();
        codes.add(DEFAULT_CODE);
        for (URL root : findRootBundles()) {
            collectCodes(root, codes);
        }

        Map<String, ApprovalLanguage> languages = new TreeMap<>();
        for (String code : codes) {
            if (code != null && !code.isBlank()) {
                languages.put(code, createLanguage(code));
            }
        }

        List<ApprovalLanguage> result = new ArrayList<>(languages.values());
        result.sort(Comparator
                .comparing((ApprovalLanguage language) -> !DEFAULT_CODE.equals(language.code()))
                .thenComparing(ApprovalLanguage::displayName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(result);
    }

    private static List<URL> findRootBundles() {
        List<URL> roots = new ArrayList<>();
        try {
            Enumeration<URL> resources = classLoader().getResources(PACKAGE_PATH + DEFAULT_FILE);
            while (resources.hasMoreElements()) {
                roots.add(resources.nextElement());
            }
        } catch (IOException ignored) {
            // English is still added explicitly; the selector falls back to it.
        }
        return roots;
    }

    private static void collectCodes(URL root, Set<String> codes) {
        try {
            if ("file".equals(root.getProtocol())) {
                Path file = Path.of(root.toURI());
                Path directory = file.getParent();
                if (directory == null) {
                    return;
                }
                try (DirectoryStream<Path> files = Files.newDirectoryStream(
                        directory,
                        BUNDLE_PREFIX + "*" + BUNDLE_SUFFIX
                )) {
                    for (Path candidate : files) {
                        String code = codeFromFileName(candidate.getFileName().toString());
                        if (code != null) {
                            codes.add(code);
                        }
                    }
                }
            } else if ("jar".equals(root.getProtocol())) {
                JarURLConnection connection = (JarURLConnection) root.openConnection();
                try (JarFile jar = new JarFile(Path.of(connection.getJarFileURL().toURI()).toFile())) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        String name = entries.nextElement().getName();
                        if (name.startsWith(PACKAGE_PATH + BUNDLE_PREFIX) && name.endsWith(BUNDLE_SUFFIX)
                                && name.indexOf('/', PACKAGE_PATH.length()) < 0) {
                            String code = codeFromFileName(name.substring(name.lastIndexOf('/') + 1));
                            if (code != null) {
                                codes.add(code);
                            }
                        }
                    }
                }
            }
        } catch (IOException | URISyntaxException | RuntimeException ignored) {
            // A partial or unusual classpath must not prevent the approval window from opening.
        }
    }

    private static String codeFromFileName(String fileName) {
        if (!fileName.startsWith(BUNDLE_PREFIX) || !fileName.endsWith(BUNDLE_SUFFIX)) {
            return null;
        }
        String code = fileName.substring(BUNDLE_PREFIX.length(), fileName.length() - BUNDLE_SUFFIX.length());
        return code.isBlank() ? null : code;
    }

    private static ApprovalLanguage createLanguage(String code) {
        Locale locale = DEFAULT_CODE.equals(code)
                ? Locale.ENGLISH
                : Locale.forLanguageTag(code.replace('_', '-'));
        return new ApprovalLanguage(code, locale, loadDisplayName(locale, code));
    }

    private static String loadDisplayName(Locale locale, String code) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale, CONTROL);
            String displayName = bundle.getString(DISPLAY_NAME_KEY);
            return displayName == null || displayName.isBlank() ? code.toUpperCase(Locale.ROOT) : displayName;
        } catch (MissingResourceException e) {
            return code.toUpperCase(Locale.ROOT);
        }
    }

    private static ClassLoader classLoader() {
        ClassLoader loader = ApprovalLanguages.class.getClassLoader();
        return loader == null ? ClassLoader.getSystemClassLoader() : loader;
    }
}
