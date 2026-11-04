package com.avrix.core;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Defines which class names are delegated to the parent class loader instead of being resolved locally.
 */
public final class ClassLoadingPolicy {

    private static final Set<String> DEFAULT_PARENT_DELEGATED_PREFIXES = Set.of(
            "java.",
            "jdk.",
            "org.slf4j.",
            "ch.qos.logback.",
            "com.avrix.",
            "org.objectweb.asm.",
            "net.lenni0451.classtransform.",
            "org.spongepowered.configurate.",
            "com.github.zafarkhaja.semver.",
            "org.yaml.snakeyaml.",
            "io.leangen.geantyref."
    );

    private final Set<String> parentDelegatedPrefixes;

    private ClassLoadingPolicy(Set<String> parentDelegatedPrefixes) {
        this.parentDelegatedPrefixes = Set.copyOf(parentDelegatedPrefixes);
    }

    /**
     * Returns the default classloading policy used by Avrix.
     *
     * @return policy that delegates JVM, logging, and Avrix packages to the parent class loader
     */
    public static ClassLoadingPolicy defaults() {
        return new ClassLoadingPolicy(DEFAULT_PARENT_DELEGATED_PREFIXES);
    }

    /**
     * Creates a policy builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the immutable set of prefixes delegated to the parent class loader.
     *
     * @return parent-delegated package prefixes
     */
    public Set<String> parentDelegatedPrefixes() {
        return parentDelegatedPrefixes;
    }

    /**
     * Checks whether a class name must be loaded by the parent class loader.
     *
     * @param className binary class name
     * @return {@code true} if the class matches a parent-delegated prefix
     */
    public boolean delegatesToParent(String className) {
        Objects.requireNonNull(className, "Class name cannot be null");

        for (String prefix : parentDelegatedPrefixes) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builder for {@link ClassLoadingPolicy}.
     */
    public static final class Builder {

        private final Set<String> parentDelegatedPrefixes = new LinkedHashSet<>();

        private Builder() {
        }

        /**
         * Adds package prefixes that must be delegated to the parent class loader.
         *
         * @param prefixes package prefixes such as {@code "java."}
         * @return this builder
         * @throws NullPointerException     if {@code prefixes} or any prefix is null
         * @throws IllegalArgumentException if any prefix is blank
         */
        public Builder delegateToParent(String... prefixes) {
            Objects.requireNonNull(prefixes, "Prefixes cannot be null");

            for (String prefix : prefixes) {
                Objects.requireNonNull(prefix, "Prefix cannot be null");
                String normalized = prefix.strip();
                if (normalized.isEmpty()) {
                    throw new IllegalArgumentException("Prefix cannot be blank");
                }
                if (!normalized.endsWith(".")) {
                    normalized = normalized + ".";
                }
                parentDelegatedPrefixes.add(normalized);
            }
            return this;
        }

        /**
         * Builds the immutable classloading policy.
         *
         * @return new classloading policy
         */
        public ClassLoadingPolicy build() {
            return new ClassLoadingPolicy(parentDelegatedPrefixes);
        }
    }
}
