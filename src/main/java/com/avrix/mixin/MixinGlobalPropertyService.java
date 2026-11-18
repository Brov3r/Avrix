package com.avrix.mixin;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory global property service used by Mixin when no host platform blackboard is available.
 */
public final class MixinGlobalPropertyService implements IGlobalPropertyService {

    private final Map<String, Object> properties = new ConcurrentHashMap<>();

    /**
     * Creates the global property service discovered by the Java service loader.
     */
    public MixinGlobalPropertyService() {
    }

    /**
     * Resolves a property key.
     *
     * @param name key name
     * @return property key
     * @throws NullPointerException if {@code name} is null
     */
    @Override
    public IPropertyKey resolveKey(String name) {
        return new Key(Objects.requireNonNull(name, "Property key name cannot be null"));
    }

    /**
     * Returns a property value.
     *
     * @param key property key
     * @param <T> value type
     * @return stored value, or {@code null}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(IPropertyKey key) {
        return key == null ? null : (T) properties.get(key.toString());
    }

    /**
     * Stores a property value.
     *
     * @param key   property key
     * @param value value to store, or {@code null} to remove the property
     */
    @Override
    public void setProperty(IPropertyKey key, Object value) {
        if (key == null) {
            return;
        }
        if (value == null) {
            properties.remove(key.toString());
        } else {
            properties.put(key.toString(), value);
        }
    }

    /**
     * Returns a property value or a default when the property is absent.
     *
     * @param key          property key
     * @param defaultValue fallback value
     * @param <T>          value type
     * @return stored value, or {@code defaultValue}
     */
    @Override
    public <T> T getProperty(IPropertyKey key, T defaultValue) {
        T value = getProperty(key);
        return value == null ? defaultValue : value;
    }

    /**
     * Returns a property value as a string.
     *
     * @param key          property key
     * @param defaultValue fallback value
     * @return string representation of the stored value, or {@code defaultValue}
     */
    @Override
    public String getPropertyString(IPropertyKey key, String defaultValue) {
        Object value = getProperty(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private record Key(String name) implements IPropertyKey {

        /**
         * Returns the original property name.
         *
         * @return property name
         */
        @Override
        public String toString() {
            return name;
        }
    }
}
