package com.avrix.utils;

import com.avrix.enums.PluginType;
import com.avrix.plugins.Metadata;
import org.tinylog.Logger;

import java.util.*;

/**
 * Utility class responsible for computing a deterministic plugin load order
 * based on plugin type tiers and declared dependencies.
 */
public final class LoadOrderSorter {

    /**
     * Prevents instantiation of this utility class.
     */
    private LoadOrderSorter() {
        // utility class
    }

    /**
     * Sorts the given plugin metadata list into a deterministic load order.
     *
     * <p>
     * Order rules:
     * <ul>
     *   <li>Exactly one LOADER and exactly one PROVIDER must be present.</li>
     *   <li>Global tier priority: LOADER -> PROVIDER -> PLUGIN</li>
     *   <li>Dependencies must be loaded before dependents.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Determinism: within the same tier, when multiple nodes are available to load,
     * the sorter picks them in ascending {@code id} order.
     * </p>
     *
     * @param plugins plugin metadata collection; must not be {@code null}
     * @return ordered immutable list of metadata
     * @throws NullPointerException     if {@code plugins} is {@code null}
     * @throws IllegalArgumentException if duplicate ids exist, dependencies are missing,
     *                                  a dependency violates tier ordering, or loader/provider cardinality is invalid
     * @throws IllegalStateException    if a cyclic dependency is detected within a tier
     */
    public static List<Metadata> sort(Collection<Metadata> plugins) {
        Objects.requireNonNull(plugins, "plugins must not be null");

        Map<String, Metadata> byId = new HashMap<>(Math.max(16, plugins.size() * 2));

        int loaderCount = 0;
        int providerCount = 0;
        int pluginCount = 0;

        for (Metadata md : plugins) {
            if (md == null) {
                throw new IllegalArgumentException("plugins must not contain null elements");
            }

            String id = requireNonBlank(md.getId(), "metadata.id");
            Metadata prev = byId.putIfAbsent(id, md);
            if (prev != null) {
                throw new IllegalArgumentException("Duplicate plugin id: " + id);
            }

            PluginType type = Objects.requireNonNull(md.getType(), "type must not be null");
            switch (type) {
                case LOADER -> loaderCount++;
                case PROVIDER -> providerCount++;
                case PLUGIN -> pluginCount++;
            }
        }

        Logger.debug("Sorting plugins: total={}, loaders={}, providers={}, plugins={}",
                byId.size(), loaderCount, providerCount, pluginCount);

        if (loaderCount != 1) {
            throw new IllegalArgumentException("Exactly one LOADER must be present, but found: " + loaderCount);
        }
        if (providerCount != 1) {
            throw new IllegalArgumentException("Exactly one PROVIDER must be present, but found: " + providerCount);
        }

        // Validate dependency existence and tier constraints.
        for (Metadata md : byId.values()) {
            int tier = tierOf(md.getType());
            for (String depId : dependencyIds(md)) {
                Metadata dep = byId.get(depId);
                if (dep == null) {
                    throw new IllegalArgumentException("Missing dependency '" + depId + "' required by '" + md.getId() + "'");
                }
                int depTier = tierOf(dep.getType());
                if (depTier > tier) {
                    throw new IllegalArgumentException(
                            "Dependency tier violation: '" + md.getId() + "' (" + md.getType()
                                    + ") depends on '" + depId + "' (" + dep.getType() + ")"
                    );
                }
            }
        }

        List<Metadata> result = new ArrayList<>(byId.size());
        result.addAll(sortTier(byId, 0));
        result.addAll(sortTier(byId, 1));
        result.addAll(sortTier(byId, 2));

        return List.copyOf(result);
    }

    /**
     * Sorts a single tier using Kahn's topological sort considering only edges within this tier.
     * Dependencies on earlier tiers are assumed satisfied by the outer ordering.
     */
    private static List<Metadata> sortTier(Map<String, Metadata> byId, int tier) {
        List<String> ids = new ArrayList<>();
        for (Metadata md : byId.values()) {
            if (tierOf(md.getType()) == tier) {
                ids.add(md.getId());
            }
        }
        if (ids.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> indegree = new HashMap<>(Math.max(16, ids.size() * 2));
        Map<String, List<String>> adj = new HashMap<>(Math.max(16, ids.size() * 2));

        for (String id : ids) {
            indegree.put(id, 0);
        }

        for (String id : ids) {
            Metadata md = byId.get(id);
            for (String depId : dependencyIds(md)) {
                Metadata dep = byId.get(depId);
                if (dep != null && tierOf(dep.getType()) == tier) {
                    // Edge: dep -> id
                    adj.computeIfAbsent(depId, __ -> new ArrayList<>()).add(id);
                    indegree.put(id, indegree.get(id) + 1);
                }
            }
        }

        PriorityQueue<String> ready = new PriorityQueue<>();
        for (String id : ids) {
            if (indegree.get(id) == 0) {
                ready.add(id);
            }
        }

        List<Metadata> ordered = new ArrayList<>(ids.size());
        int processed = 0;

        while (!ready.isEmpty()) {
            String id = ready.poll();
            ordered.add(byId.get(id));
            processed++;

            List<String> dependents = adj.get(id);
            if (dependents == null) {
                continue;
            }
            for (String to : dependents) {
                int next = indegree.get(to) - 1;
                indegree.put(to, next);
                if (next == 0) {
                    ready.add(to);
                }
            }
        }

        if (processed != ids.size()) {
            List<String> remaining = new ArrayList<>();
            for (String id : ids) {
                if (indegree.get(id) != 0) {
                    remaining.add(id);
                }
            }
            Collections.sort(remaining);
            throw new IllegalStateException("Cyclic dependency detected in tier " + tierName(tier) + ": " + remaining);
        }

        return ordered;
    }

    /**
     * Returns a human-readable tier name for error messages and logs.
     *
     * @param tier tier index
     * @return tier name such as "LOADER", "PROVIDER", "PLUGIN", or "UNKNOWN(n)"
     */
    private static String tierName(int tier) {
        return switch (tier) {
            case 0 -> "LOADER";
            case 1 -> "PROVIDER";
            case 2 -> "PLUGIN";
            default -> "UNKNOWN(" + tier + ")";
        };
    }

    /**
     * Maps {@link PluginType} to its tier index.
     *
     * <p>
     * Lower tier index means earlier load order.
     * </p>
     *
     * @param type plugin type; must not be {@code null}
     * @return tier index: 0=LOADER, 1=PROVIDER, 2=PLUGIN
     * @throws NullPointerException if {@code type} is {@code null}
     */
    private static int tierOf(PluginType type) {
        Objects.requireNonNull(type, "type must not be null");
        return switch (type) {
            case LOADER -> 0;
            case PROVIDER -> 1;
            case PLUGIN -> 2;
        };
    }

    /**
     * Returns dependency ids declared by the given metadata.
     *
     * <p>
     * This method treats {@code null} or empty dependency maps as having no dependencies.
     * Only ids (map keys) are used for ordering; version constraints (map values) are ignored here.
     * </p>
     *
     * @param md plugin metadata; must not be {@code null}
     * @return immutable set of dependency ids, never {@code null}
     */
    private static Set<String> dependencyIds(Metadata md) {
        Map<String, String> deps = md.getDependencies();
        if (deps == null || deps.isEmpty()) {
            return Set.of();
        }
        return deps.keySet();
    }

    /**
     * Validates that a string value is non-null and not blank.
     *
     * @param value value to validate
     * @param field logical field name for error messages
     * @return the original value
     * @throws NullPointerException     if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is blank
     */
    private static String requireNonBlank(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}