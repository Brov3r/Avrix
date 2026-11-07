package com.avrix.plugins;

import com.avrix.core.Metadata;
import com.avrix.utils.Constants;
import com.github.zafarkhaja.semver.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * Resolves plugin dependencies and produces a deterministic topological load order.
 */
public final class DependencyResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyResolver.class);

    private final Set<String> corePluginIds;
    private final List<Metadata> corePlugins;
    private final boolean strict;

    /**
     * Constructs a lenient resolver with the identifiers reserved for core components.
     *
     * @param corePlugins core plugin descriptors whose identifiers cannot be used by user plugins
     * @throws NullPointerException if {@code corePlugins} is null
     */
    public DependencyResolver(Collection<Metadata> corePlugins) {
        this(corePlugins, false);
    }

    /**
     * Constructs a resolver with the identifiers reserved for core components.
     *
     * @param corePlugins core plugin descriptors whose identifiers cannot be used by user plugins
     * @param strict      whether declared plugin conflicts should abort resolution
     * @throws NullPointerException if {@code corePlugins} is null
     */
    public DependencyResolver(Collection<Metadata> corePlugins, boolean strict) {
        Objects.requireNonNull(corePlugins, "Core plugins cannot be null");

        Set<String> ids = new TreeSet<>();
        for (Metadata corePlugin : corePlugins) {
            ids.add(corePlugin.id());
        }
        this.corePluginIds = Set.copyOf(ids);
        this.corePlugins = List.copyOf(corePlugins);
        this.strict = strict;
    }

    /**
     * Resolves the dependency graph and returns a deterministic user plugin load order.
     *
     * @param candidates candidate plugin descriptors, including core descriptors
     * @return ordered list of user plugins, with core descriptors omitted
     * @throws NullPointerException  if {@code candidates} is null
     * @throws IllegalStateException if identifiers collide, a reserved ID is used, dependencies are missing,
     *                               version constraints are invalid, a conflict is detected in strict mode, or a cycle
     *                               is detected
     */
    public List<Metadata> resolve(Collection<Metadata> candidates) {
        Objects.requireNonNull(candidates, "Candidates list cannot be null");

        Map<String, Metadata> registry = new TreeMap<>();
        for (Metadata meta : candidates) {
            String id = meta.id();

            if (isReservedId(id) && !corePluginIds.contains(id)) {
                throw new ResolutionFailure(Set.of(id), "Plugin ID [%s] is reserved for a core component".formatted(id));
            }

            if (registry.put(id, meta) != null) {
                throw new ResolutionFailure(Set.of(id), "Duplicate plugin identifier [%s]".formatted(id));
            }
        }

        validateConflicts(registry.values(), registry);

        Map<String, Set<String>> adjacencyList = new TreeMap<>();
        for (String id : registry.keySet()) {
            adjacencyList.put(id, new TreeSet<>());
        }

        Set<String> wildcardBeforePlugins = new TreeSet<>();
        Set<String> wildcardAfterPlugins = new TreeSet<>();

        for (Metadata meta : registry.values()) {
            String currentId = meta.id();

            if (meta.loadBefore().contains("*") && meta.loadAfter().contains("*")) {
                throw new ResolutionFailure(Set.of(currentId),
                        "Plugin [%s] cannot declare both loadBefore and loadAfter wildcards".formatted(currentId)
                );
            }

            for (Map.Entry<String, String> dependency : meta.dependencies().entrySet()) {
                String requiredId = dependency.getKey();
                String constraint = dependency.getValue();

                if (currentId.equals(requiredId)) {
                    throw new ResolutionFailure(Set.of(currentId), "Plugin [%s] cannot depend on itself".formatted(currentId));
                }

                try {
                    SemVerConstraints.validate(constraint);
                } catch (IllegalArgumentException e) {
                    throw new ResolutionFailure(Set.of(currentId),
                            "Invalid version constraint in plugin [%s] for dependency [%s]: %s"
                                    .formatted(currentId, requiredId, e.getMessage()),
                            e
                    );
                }

                Metadata requiredMeta = registry.get(requiredId);

                if (requiredMeta == null) {
                    throw new ResolutionFailure(Set.of(currentId),
                            "Plugin [%s] depends on missing component [%s]".formatted(currentId, requiredId)
                    );
                }

                Version actualVersion;
                try {
                    actualVersion = Version.parse(requiredMeta.version());
                } catch (Exception e) {
                    throw new ResolutionFailure(Set.of(currentId),
                            "Invalid version [%s] declared by plugin [%s] required by plugin [%s]"
                                    .formatted(requiredMeta.version(), requiredMeta.id(), currentId), e
                    );
                }

                boolean satisfies;
                try {
                    satisfies = actualVersion.satisfies(constraint);
                } catch (Exception e) {
                    throw new ResolutionFailure(Set.of(currentId),
                            "Invalid version constraint in plugin [%s] for dependency [%s]: %s"
                                    .formatted(currentId, requiredId, e.getMessage()), e
                    );
                }

                if (!satisfies) {
                    throw new ResolutionFailure(Set.of(currentId),
                            "Plugin [%s] requires [%s] matching [%s], but found version [%s]"
                                    .formatted(currentId, requiredId, constraint, requiredMeta.version())
                    );
                }

                adjacencyList.get(requiredMeta.id()).add(currentId);
            }

            for (String afterId : meta.loadAfter()) {
                if ("*".equals(afterId)) {
                    wildcardAfterPlugins.add(currentId);
                } else if (currentId.equals(afterId)) {
                    throw new ResolutionFailure(Set.of(currentId), "Plugin [%s] cannot declare loadAfter itself".formatted(currentId));
                } else {
                    Metadata afterMeta = registry.get(afterId);
                    if (afterMeta == null) {
                        LOGGER.warn("Plugin [{}] declares loadAfter for unknown plugin [{}]", currentId, afterId);
                    } else {
                        adjacencyList.get(afterMeta.id()).add(currentId);
                    }
                }
            }

            for (String beforeId : meta.loadBefore()) {
                if ("*".equals(beforeId)) {
                    wildcardBeforePlugins.add(currentId);
                } else if (currentId.equals(beforeId)) {
                    throw new ResolutionFailure(Set.of(currentId), "Plugin [%s] cannot declare loadBefore itself".formatted(currentId));
                } else {
                    Metadata beforeMeta = registry.get(beforeId);
                    if (beforeMeta == null) {
                        LOGGER.warn("Plugin [{}] declares loadBefore for unknown plugin [{}]", currentId, beforeId);
                    } else {
                        adjacencyList.get(currentId).add(beforeMeta.id());
                    }
                }
            }
        }

        for (String currentId : wildcardBeforePlugins) {
            Metadata currentMeta = registry.get(currentId);
            for (String otherId : registry.keySet()) {
                if (otherId.equals(currentId)) {
                    continue;
                }
                if (currentMeta.dependencies().containsKey(otherId)) {
                    continue;
                }
                if (currentMeta.loadAfter().contains(otherId)) {
                    continue;
                }
                Metadata otherMeta = registry.get(otherId);
                if (otherMeta.loadBefore().contains(currentId)) {
                    continue;
                }
                if (wildcardBeforePlugins.contains(otherId) && adjacencyList.get(otherId).contains(currentId)) {
                    continue;
                }

                adjacencyList.get(currentId).add(otherId);
            }
        }

        for (String currentId : wildcardAfterPlugins) {
            Metadata currentMeta = registry.get(currentId);
            for (String otherId : registry.keySet()) {
                if (otherId.equals(currentId)) {
                    continue;
                }
                Metadata otherMeta = registry.get(otherId);
                if (otherMeta.dependencies().containsKey(currentId)) {
                    continue;
                }
                if (otherMeta.loadAfter().contains(currentId)) {
                    continue;
                }
                if (currentMeta.loadBefore().contains(otherId)) {
                    continue;
                }
                if (wildcardAfterPlugins.contains(otherId) && adjacencyList.get(currentId).contains(otherId)) {
                    continue;
                }

                adjacencyList.get(otherId).add(currentId);
            }
        }

        Map<String, Integer> inDegrees = new HashMap<>();
        for (String id : registry.keySet()) {
            inDegrees.put(id, 0);
        }

        for (Set<String> outgoingEdges : adjacencyList.values()) {
            for (String target : outgoingEdges) {
                inDegrees.put(target, inDegrees.get(target) + 1);
            }
        }

        Queue<String> queue = new PriorityQueue<>();
        for (Map.Entry<String, Integer> entry : inDegrees.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<Metadata> sortedList = new ArrayList<>(registry.size());
        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            sortedList.add(registry.get(currentId));

            for (String neighbor : adjacencyList.get(currentId)) {
                int updatedDegree = inDegrees.get(neighbor) - 1;
                inDegrees.put(neighbor, updatedDegree);

                if (updatedDegree == 0) {
                    queue.add(neighbor);
                }
            }
        }

        if (sortedList.size() != registry.size()) {
            List<String> cycleNodes = inDegrees.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .map(Map.Entry::getKey)
                    .sorted()
                    .toList();
            Set<String> circularIds = new TreeSet<>();
            for (String id : cycleNodes) {
                if (reachesItself(id, adjacencyList)) {
                    circularIds.add(id);
                }
            }
            throw new ResolutionFailure(circularIds,
                    "Cyclic plugin dependency or load order detected. Circular nodes: " + String.join(", ", circularIds));
        }

        List<Metadata> userPluginsOrder = sortedList.stream()
                .filter(meta -> !corePluginIds.contains(meta.id()))
                .toList();

        LOGGER.debug("Topological sort resolved: {} user plugin(s) ({} core entries omitted from execution queue)",
                userPluginsOrder.size(), corePluginIds.size());

        return List.copyOf(userPluginsOrder);
    }

    /**
     * Resolves the load order after excluding invalid plugins and their unavailable hard dependents.
     * <p>
     * Duplicate user IDs and cycle members are excluded entirely. Soft ordering references to excluded plugins
     * do not prevent otherwise valid plugins from loading. Core descriptors are retained.
     *
     * @param candidates candidate descriptors, including the core descriptors supplied to this resolver
     * @param failures consumer receiving a failure for each excluded user descriptor
     * @return deterministic order of the remaining user plugins
     * @throws NullPointerException if either argument is null
     * @throws IllegalStateException if a failure cannot be resolved by excluding user plugins
     */
    public List<Metadata> resolveAvailable(Collection<Metadata> candidates, Consumer<PluginLoadResult> failures) {
        Objects.requireNonNull(candidates, "Candidates list cannot be null");
        Objects.requireNonNull(failures, "Failure consumer cannot be null");
        List<Metadata> remaining = new ArrayList<>(candidates);
        while (true) {
            try {
                return resolve(remaining);
            } catch (ResolutionFailure failure) {
                boolean removed = remaining.removeIf(meta -> {
                    if (!failure.pluginIds.contains(meta.id()) || corePlugins.stream().anyMatch(core -> core == meta)) {
                        return false;
                    }
                    failures.accept(PluginLoadResult.failed(meta.id(), failure));
                    return true;
                });
                if (!removed) {
                    throw failure;
                }
            }
        }
    }

    private static boolean reachesItself(String start, Map<String, Set<String>> graph) {
        Set<String> visited = new HashSet<>();
        ArrayDeque<String> pending = new ArrayDeque<>(graph.get(start));
        while (!pending.isEmpty()) {
            String id = pending.removeFirst();
            if (start.equals(id)) {
                return true;
            }
            if (visited.add(id)) {
                pending.addAll(graph.get(id));
            }
        }
        return false;
    }

    private static final class ResolutionFailure extends IllegalStateException {
        private final Set<String> pluginIds;

        private ResolutionFailure(Set<String> pluginIds, String message) {
            this(pluginIds, message, null);
        }

        private ResolutionFailure(Set<String> pluginIds, String message, Throwable cause) {
            super(message, cause);
            this.pluginIds = Set.copyOf(pluginIds);
        }
    }

    /**
     * Validates declared plugin conflicts against installed plugins.
     * <p>
     * A single declaration is sufficient: if plugin A declares a conflict with B, the conflict is reported even when B
     * does not declare A. Conflict identifiers use the canonical lower-case form validated by {@link Metadata}, and
     * declarations against plugins that are not installed are ignored.
     *
     * @param installedPlugins installed plugin descriptors in deterministic order
     * @param registry         installed plugin descriptors keyed by identifier
     * @throws IllegalStateException if strict mode is enabled and a conflict is detected
     */
    private void validateConflicts(
            Collection<Metadata> installedPlugins,
            Map<String, Metadata> registry
    ) {
        for (Metadata metadata : installedPlugins) {
            Set<String> checkedConflicts = new TreeSet<>();
            for (String conflictId : metadata.conflicts()) {
                if (!checkedConflicts.add(conflictId)) {
                    continue;
                }

                Metadata conflictingPlugin = registry.get(conflictId);
                if (conflictingPlugin == null) {
                    continue;
                }

                String message = "Plugin [%s] conflicts with installed plugin [%s]"
                        .formatted(metadata.id(), conflictingPlugin.id());
                if (strict) {
                    throw new IllegalStateException(message);
                }
                LOGGER.warn(message);
            }
        }
    }

    private static boolean isReservedId(String id) {
        return Constants.LOADER_ID.equals(id);
    }
}
