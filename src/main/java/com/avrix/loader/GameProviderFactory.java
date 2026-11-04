package com.avrix.loader;

import com.avrix.api.GameProvider;
import com.avrix.utils.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Discovers and selects {@link GameProvider} implementations through {@link ServiceLoader}.
 */
public final class GameProviderFactory {

    private static final String SERVICE_FILE = "META-INF/services/" + GameProvider.class.getName();

    /**
     * Prevents instantiation of this static factory.
     */
    private GameProviderFactory() {
        throw new UnsupportedOperationException("This is a factory class and cannot be instantiated.");
    }

    /**
     * Loads a game provider using the current thread context class loader.
     *
     * @param providerId unique identifier of the provider to load
     * @return matching game provider
     * @throws NullPointerException     if {@code providerId} is null
     * @throws IllegalArgumentException if {@code providerId} is blank
     * @throws IllegalStateException    if no provider is registered or multiple providers share the requested ID
     */
    public static GameProvider load(String providerId) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader classLoader = (contextClassLoader != null)
                ? contextClassLoader
                : GameProvider.class.getClassLoader();
        return load(providerId, classLoader);
    }

    /**
     * Loads a game provider using the provided class loader.
     * <p>
     * When {@code providerId} equals {@link Constants#AUTO_PROVIDER_ID}, the only registered provider is selected.
     * If several providers are available, the caller must choose one explicitly.
     *
     * @param providerId  unique identifier of the provider to load, or {@link Constants#AUTO_PROVIDER_ID}
     * @param classLoader class loader used to discover service implementations
     * @return matching game provider
     * @throws NullPointerException     if {@code providerId} or {@code classLoader} is null
     * @throws IllegalArgumentException if {@code providerId} is blank
     * @throws IllegalStateException    if service discovery fails, no provider is registered, multiple providers
     *                                  share the requested ID, or auto-selection is ambiguous
     */
    public static GameProvider load(String providerId, ClassLoader classLoader) {
        Objects.requireNonNull(providerId, "Game provider ID cannot be null");
        Objects.requireNonNull(classLoader, "ClassLoader cannot be null");

        if (providerId.isBlank()) {
            throw new IllegalArgumentException("Game provider ID cannot be blank");
        }

        List<GameProvider> providers = new ArrayList<>();
        List<String> availableProviders = new ArrayList<>();

        try {
            for (GameProvider provider : ServiceLoader.load(GameProvider.class, classLoader)) {
                providers.add(provider);
                availableProviders.add(provider.getId());
            }
        } catch (ServiceConfigurationError e) {
            throw new IllegalStateException("Failed to discover GameProvider implementations from " + SERVICE_FILE, e);
        }

        if (Constants.AUTO_PROVIDER_ID.equals(providerId)) {
            if (providers.isEmpty()) {
                throw new IllegalStateException(
                        "No GameProvider implementations registered. Add a provider to " + SERVICE_FILE
                );
            }
            if (providers.size() > 1) {
                throw new IllegalStateException(
                        "Multiple GameProvider implementations registered: %s. Select one with -D%s=<id>"
                                .formatted(availableProviders, Constants.GAME_PROVIDER_PROPERTY)
                );
            }
            return providers.get(0);
        }

        GameProvider match = null;
        for (GameProvider provider : providers) {
            if (!providerId.equals(provider.getId())) {
                continue;
            }
            if (match != null) {
                throw new IllegalStateException(
                        "Multiple GameProvider implementations registered for ID [%s]".formatted(providerId)
                );
            }
            match = provider;
        }

        if (match == null) {
            throw new IllegalStateException(
                    "No GameProvider registered for ID [%s]. Available providers: %s"
                            .formatted(providerId, availableProviders)
            );
        }

        return match;
    }
}
