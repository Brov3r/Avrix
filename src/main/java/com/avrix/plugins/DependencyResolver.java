package com.avrix.plugins;

import com.avrix.core.Metadata;
import com.avrix.utils.Constants;
import com.github.zafarkhaja.semver.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Resolves plugin dependencies and produces a deterministic topological load order.
 */
public final class DependencyResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyResolver.class);

    private final Set<String> corePluginIds;

    /**
     * Constructs a resolver with the identifiers reserved for core components.
     *
     * @param corePlugins core plugin descriptors whose identifiers cannot be used by user plugins
     * @throws NullPointerException if {@code corePlugins} is null
     */
    public DependencyResolver(Collection<Metadata> corePlugins) {
        Objects.requireNonNull(corePlugins, "Core plugins cannot be null");

        Set<String> ids = new TreeSet<>();
        for (Metadata corePlugin : corePlugins) {
            ids.add(corePlugin.id());
        }
        this.corePluginIds = Set.copyOf(ids);
    }

    /**
     * Resolves the dependency graph and returns a deterministic user plugin load order.
     *
     * @param candidates candidate plugin descriptors, including core descriptors
     * @return ordered list of user plugins, with core descriptors omitted
     * @throws NullPointerException  if {@code candidates} is null
     * @throws IllegalStateException if identifiers collide, a reserved ID is used, dependencies are missing,
     *                               version constraints are invalid, or a cycle is detected
     */
    public List<Metadata> resolve(Collection<Metadata> candidates) {
        Objects.requireNonNull(candidates, "Candidates list cannot be null");

        Map<String, Metadata> registry = new TreeMap<>();
        for (Metadata meta : candidates) {
            String id = meta.id();

            if (isReservedId(id) && !corePluginIds.contains(id)) {
                throw new IllegalStateException("Plugin ID [%s] is reserved for a core component".formatted(id));
            }

            if (registry.putIfAbsent(id, meta) != null) {
                throw new IllegalStateException("Duplicate plugin identifier [%s]".formatted(id));
            }
        }

        Map<String, Set<String>> adjacencyList = new TreeMap<>();
        for (String id : registry.keySet()) {
            adjacencyList.put(id, new TreeSet<>());
        }

        Set<String> wildcardBeforePlugins = new TreeSet<>();
        Set<String> wildcardAfterPlugins = new TreeSet<>();

        for (Metadata meta : registry.values()) {
            String currentId = meta.id();

            for (Map.Entry<String, String> dependency : meta.dependencies().entrySet()) {
                String requiredId = dependency.getKey();
                String constraint = dependency.getValue();
                Metadata requiredMeta = registry.get(requiredId);

                if (requiredMeta == null) {
                    throw new IllegalStateException(
                            "Plugin [%s] depends on missing component [%s]".formatted(currentId, requiredId)
                    );
                }

                Version actualVersion;
                try {
                    actualVersion = Version.parse(requiredMeta.version());
                } catch (Exception e) {
                    throw new IllegalStateException(
                            "Invalid version [%s] declared by plugin [%s] required by plugin [%s]"
                                    .formatted(requiredMeta.version(), requiredId, currentId), e
                    );
                }

                boolean satisfies;
                try {
                    satisfies = actualVersion.satisfies(constraint);
                } catch (Exception e) {
                    throw new IllegalStateException(
                            "Invalid version constraint in plugin [%s] for dependency [%s]: %s"
                                    .formatted(currentId, requiredId, e.getMessage()), e
                    );
                }

                if (!satisfies) {
                    throw new IllegalStateException(
                            "Plugin [%s] requires [%s] matching [%s], but found version [%s]"
                                    .formatted(currentId, requiredId, constraint, requiredMeta.version())
                    );
                }

                adjacencyList.get(requiredId).add(currentId);
            }

            for (String afterId : meta.loadAfter()) {
                if ("*".equals(afterId)) {
                    wildcardAfterPlugins.add(currentId);
                } else if (registry.containsKey(afterId)) {
                    adjacencyList.get(afterId).add(currentId);
                }
            }

            for (String beforeId : meta.loadBefore()) {
                if ("*".equals(beforeId)) {
                    wildcardBeforePlugins.add(currentId);
                } else if (registry.containsKey(beforeId)) {
                    adjacencyList.get(currentId).add(beforeId);
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
            throw new IllegalStateException("Cyclic plugin dependency or load order detected. Circular nodes: " + String.join(", ", cycleNodes));
        }

        List<Metadata> userPluginsOrder = sortedList.stream()
                .filter(meta -> !corePluginIds.contains(meta.id()))
                .toList();

        LOGGER.debug("Topological sort resolved: {} user plugin(s) ({} core entries omitted from execution queue)",
                userPluginsOrder.size(), corePluginIds.size());

        return List.copyOf(userPluginsOrder);
    }

    private static boolean isReservedId(String id) {
        return Constants.LOADER_ID.equals(id) || Constants.DEFAULT_GAME_PROVIDER_ID.equals(id);
    }
}
