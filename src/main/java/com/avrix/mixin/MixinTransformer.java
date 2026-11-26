package com.avrix.mixin;

import com.avrix.core.ClassTransformer;
import com.avrix.core.KnotClassLoader;
import com.avrix.core.MixinRegistrar;
import com.avrix.utils.FatalErrors;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Instance-scoped mixin transformer for the Avrix loader.
 *
 * <p>Each registered mixin class receives a generated Mixin configuration that points to the class and its declared
 * targets. The configuration is served from memory by {@link MixinHostService}, so plugin metadata stays the single
 * source of truth and no {@code .mixins.json} resources are required.
 */
public final class MixinTransformer implements ClassTransformer, MixinRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger(MixinTransformer.class);
    private static final Object BOOTSTRAP_LOCK = new Object();
    private static final AtomicLong CONFIG_SEQUENCE = new AtomicLong();
    private static final int BASE_PRIORITY = 1000;

    private static boolean bootstrapped;

    private final boolean strict;
    private final CompositeClassProvider classProvider;
    private final IMixinTransformer transformer;
    private final MixinEnvironment environment;
    private final Set<String> registeredMixins = ConcurrentHashMap.newKeySet();
    private final Set<String> registeredMixinPackages = ConcurrentHashMap.newKeySet();
    private final Set<String> targetClasses = ConcurrentHashMap.newKeySet();

    /**
     * Creates a lenient mixin transformer for the provided {@link KnotClassLoader}.
     *
     * @param knotClassLoader the flat classloader containing game classes, dependencies, and plugins
     * @throws NullPointerException if {@code knotClassLoader} is null
     */
    public MixinTransformer(KnotClassLoader knotClassLoader) {
        this(knotClassLoader, false);
    }

    /**
     * Creates a mixin transformer for the provided {@link KnotClassLoader}.
     *
     * <p>In lenient mode transformation failures cancel only the affected class and are logged. In strict mode
     * failures are propagated to the classloader so startup can be aborted by the existing {@code --strict}
     * handling.
     *
     * @param knotClassLoader the flat classloader containing game classes, dependencies, and plugins
     * @param strict          whether transformation failures should abort class loading
     * @throws NullPointerException  if {@code knotClassLoader} is null
     * @throws IllegalStateException if the Mixin transformer cannot be initialized
     */
    public MixinTransformer(KnotClassLoader knotClassLoader, boolean strict) {
        Objects.requireNonNull(knotClassLoader, "KnotClassLoader cannot be null");

        this.strict = strict;
        this.classProvider = new CompositeClassProvider(knotClassLoader);
        MixinHostService.configure(knotClassLoader);
        initializeMixin();

        this.environment = MixinEnvironment.getCurrentEnvironment();
        Object activeTransformer = this.environment.getActiveTransformer();
        if (!(activeTransformer instanceof IMixinTransformer activeMixinTransformer)) {
            throw new IllegalStateException("Mixin transformer was not initialized by the host service");
        }
        this.transformer = activeMixinTransformer;

        LOGGER.info("Mixin environment initialized successfully (strict={}).", strict);
    }

    /**
     * Initializes the process-wide Mixin runtime once.
     *
     * <p>Mixin keeps global configuration state, so every transformer instance shares the same active transformer.
     * Instance-specific registrations are still tracked separately and only target classes registered on this
     * instance are transformed.
     */
    private static void initializeMixin() {
        synchronized (BOOTSTRAP_LOCK) {
            if (!bootstrapped) {
                MixinBootstrap.init();
                bootstrapped = true;
            }
            MixinExtrasBootstrap.init();
        }
    }

    /**
     * Transforms class bytecode using the active Mixin transformer.
     *
     * @param className     binary name of the class
     * @param originalBytes original class bytes
     * @return transformed class bytes, or {@code null} if the class is not a registered mixin target
     */
    @Override
    public synchronized byte[] transform(String className, byte[] originalBytes) {
        if (className == null || originalBytes == null || !targetClasses.contains(className)) {
            return null;
        }

        try {
            byte[] transformed = transformer.transformClass(environment, className, originalBytes);
            return transformed == null || Arrays.equals(transformed, originalBytes) ? null : transformed;
        } catch (Throwable failure) {
            FatalErrors.rethrowIfFatal(failure);
            if (strict) {
                throw failure;
            }
            LOGGER.error("Failed to transform class [{}]; using original bytecode", className, failure);
            return null;
        }
    }

    /**
     * Registers a single mixin class by its fully qualified class name.
     *
     * <p>Package restrictions are validated before any Mixin configuration is created. The mixin must be declared in
     * a named package, its targets must not belong to its own package, and its targets must not belong to the package
     * of an already registered mixin in either direction. Registering an already registered mixin is a no-op.
     *
     * @param mixinClass the fully qualified class name of the mixin (e.g., {@code com.example.PlayerMixin})
     * @throws NullPointerException     if {@code mixinClass} is null
     * @throws IllegalArgumentException if {@code mixinClass} is blank, cannot be found, is not a valid mixin, or
     *                                  conflicts with a registered mixin package
     */
    @Override
    public synchronized void addMixin(String mixinClass) {
        addMixin(mixinClass, null);
    }

    /**
     * Registers a single mixin class owned by a plugin.
     *
     * <p>Package restrictions are validated before any Mixin configuration is created. The mixin must be declared in
     * a named package, its targets must not belong to its own package, and its targets must not belong to the package
     * of an already registered mixin in either direction. Registering an already registered mixin is a no-op.
     *
     * <p>A non-blank owner is attached to the generated Mixin configuration as the
     * {@link FabricUtil#KEY_MOD_ID fabric-modId} decoration, so Mixin messages report the real owning plugin.
     *
     * @param mixinClass the fully qualified class name of the mixin (e.g., {@code com.example.PlayerMixin})
     * @param ownerId    canonical plugin identifier that owns the mixin, or {@code null} when unknown
     * @throws NullPointerException     if {@code mixinClass} is null
     * @throws IllegalArgumentException if {@code mixinClass} is blank, cannot be found, is not a valid mixin, or
     *                                  conflicts with a registered mixin package
     */
    @Override
    public synchronized void addMixin(String mixinClass, String ownerId) {
        Objects.requireNonNull(mixinClass, "mixinClass cannot be null");

        if (mixinClass.isBlank()) {
            throw new IllegalArgumentException("Mixin class name cannot be blank");
        }
        if (registeredMixins.contains(mixinClass)) {
            return;
        }

        MixinDeclaration declaration = inspectDeclaration(mixinClass, normalizeOwnerId(ownerId));
        validatePackageCompatibility(List.of(declaration));
        registerDeclaration(declaration);
    }

    /**
     * Checks all declarations without registering them with the active Mixin runtime.
     *
     * <p>This validates that each class exists and carries a {@code @Mixin} annotation with at least one target.
     * Deterministic package restrictions are checked as well: the mixin must be declared in a named package, its
     * targets must not belong to its own package, and every target is checked against all mixin packages declared in
     * this batch and all already registered mixin packages in both directions, regardless of declaration order.
     * Already registered mixins are treated as idempotent no-ops, matching {@link #addMixin}. Target classes are never
     * loaded or resolved. This method does not verify whether individual injections can be applied to the target class.
     *
     * @param mixinClasses fully qualified mixin class names
     * @throws NullPointerException     if the collection or a class name is null
     * @throws IllegalArgumentException if a mixin class name is blank, has no named package, or creates a package
     *                                  conflict with this batch or with already registered mixins
     * @throws RuntimeException         if a mixin cannot be read or is not a valid mixin declaration
     */
    @Override
    public synchronized void validateMixins(Collection<String> mixinClasses) {
        validatePackageCompatibility(parseDeclarations(mixinClasses));
    }

    /**
     * Registers a collection of mixin classes.
     *
     * <p>All declarations are parsed and preflighted before the first registration, so a batch rejected by package
     * validation leaves no partial registrations. Parsed declarations are reused during registration, so their class
     * bytes are not read again. If an individual registration fails after preflight, the failure is propagated; Mixin
     * does not provide a way to unregister configurations created earlier in the same call.
     *
     * @param mixins collection of fully qualified mixin class names
     * @throws NullPointerException     if {@code mixins} or any of its elements is null
     * @throws IllegalArgumentException if any declaration fails preflight validation
     */
    public synchronized void addMixins(Collection<String> mixins) {
        List<MixinDeclaration> declarations = parseDeclarations(mixins);
        validatePackageCompatibility(declarations);
        for (MixinDeclaration declaration : declarations) {
            registerDeclaration(declaration);
        }
    }

    /**
     * Checks whether this transformer has registered a mixin class.
     *
     * @param mixinClass fully qualified mixin class name
     * @return {@code true} if the class is registered on this instance
     */
    boolean isMixinRegistered(String mixinClass) {
        return registeredMixins.contains(mixinClass);
    }

    private List<String> inspectMixin(String mixinClass) {
        try {
            byte[] classBytes = classProvider.getClass(mixinClass);
            return MixinIntrospector.requireTargets(mixinClass, classBytes);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Mixin class not found: " + mixinClass, e);
        }
    }

    private List<MixinDeclaration> parseDeclarations(Collection<String> mixinClasses) {
        Objects.requireNonNull(mixinClasses, "Mixins collection cannot be null");

        List<String> pending = new ArrayList<>(mixinClasses.size());
        for (String mixin : mixinClasses) {
            pending.add(Objects.requireNonNull(mixin, "Mixin class cannot be null"));
        }

        List<MixinDeclaration> declarations = new ArrayList<>(pending.size());
        Set<String> seen = new HashSet<>(pending.size());
        for (String mixin : pending) {
            if (mixin.isBlank()) {
                throw new IllegalArgumentException("Mixin class name cannot be blank");
            }
            if (registeredMixins.contains(mixin) || !seen.add(mixin)) {
                continue;
            }
            declarations.add(inspectDeclaration(mixin));
        }
        return declarations;
    }

    private MixinDeclaration inspectDeclaration(String mixinClass) {
        return inspectDeclaration(mixinClass, null);
    }

    private MixinDeclaration inspectDeclaration(String mixinClass, String ownerId) {
        List<String> targets = inspectMixin(mixinClass);

        int separator = mixinClass.lastIndexOf('.');
        if (separator <= 0 || separator == mixinClass.length() - 1) {
            throw new IllegalArgumentException(
                    "Mixin class [" + mixinClass + "] must be declared in a named package"
            );
        }

        String packageName = mixinClass.substring(0, separator);
        String simpleName = mixinClass.substring(separator + 1);
        return new MixinDeclaration(mixinClass, packageName, simpleName, List.copyOf(targets), ownerId);
    }

    private static String normalizeOwnerId(String ownerId) {
        if (ownerId == null) {
            return null;
        }
        String normalized = ownerId.strip();
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * Validates declarations against their own packages, all packages in the batch, and all registered packages.
     *
     * <p>Membership is checked by walking the parent package prefixes of every target and probing package sets, so
     * each target is compared against package names rather than against every declaration.
     *
     * @param declarations new declarations from the current batch
     */
    private void validatePackageCompatibility(List<MixinDeclaration> declarations) {
        if (declarations.isEmpty()) {
            return;
        }

        Set<String> newPackages = new HashSet<>(Math.max(4, declarations.size()));
        for (MixinDeclaration declaration : declarations) {
            newPackages.add(declaration.packageName());
        }

        for (MixinDeclaration declaration : declarations) {
            for (String target : declaration.targets()) {
                String owningPackage = findOwningMixinPackage(target, newPackages);
                if (owningPackage != null) {
                    if (owningPackage.equals(declaration.packageName())) {
                        throw new IllegalArgumentException(
                                "Mixin [" + declaration.mixinClass()
                                        + "] must not share its package with target [" + target + "]"
                        );
                    }
                    throw new IllegalArgumentException(
                            "Target [" + target + "] of mixin [" + declaration.mixinClass()
                                    + "] is inside mixin package [" + owningPackage + "]"
                    );
                }

                String registeredPackage = findOwningMixinPackage(target, registeredMixinPackages);
                if (registeredPackage != null) {
                    throw new IllegalArgumentException(
                            "Target [" + target + "] of mixin [" + declaration.mixinClass()
                                    + "] is inside mixin package [" + registeredPackage + "] (already registered)"
                    );
                }
            }
        }

        if (!targetClasses.isEmpty()) {
            for (String target : targetClasses) {
                String owningPackage = findOwningMixinPackage(target, newPackages);
                if (owningPackage != null) {
                    throw new IllegalArgumentException(
                            "Registered target [" + target + "] is inside mixin package [" + owningPackage + "]"
                    );
                }
            }
        }
    }

    private static String findOwningMixinPackage(String target, Set<String> packages) {
        if (packages.isEmpty()) {
            return null;
        }

        int separator = target.lastIndexOf('.');
        while (separator > 0) {
            String candidate = target.substring(0, separator);
            if (packages.contains(candidate)) {
                return candidate;
            }
            separator = candidate.lastIndexOf('.');
        }
        return null;
    }

    private void registerDeclaration(MixinDeclaration declaration) {
        if (!registeredMixins.add(declaration.mixinClass())) {
            return;
        }

        try {
            registerConfiguration(declaration);
            targetClasses.addAll(declaration.targets());
            registeredMixinPackages.add(declaration.packageName());
            LOGGER.debug(
                    "Registered mixin transformer: [{}] targeting {}", declaration.mixinClass(), declaration.targets()
            );
        } catch (RuntimeException e) {
            registeredMixins.remove(declaration.mixinClass());
            throw e;
        }
    }

    private void registerConfiguration(MixinDeclaration declaration) {
        long sequence = CONFIG_SEQUENCE.incrementAndGet();
        String configName = "mixins/host/" + sequence + ".json";
        String config = mixinConfig(declaration.packageName(), declaration.simpleName(), sequence);
        MixinHostService.registerConfig(configName, config);
        Mixins.addConfiguration(configName);
        requestConfigSelection();

        Config registered = Mixins.getConfigs().stream()
                .filter(candidate -> configName.equals(candidate.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Mixin configuration [" + configName + "] was rejected for " + declaration.mixinClass()
                ));
        if (declaration.ownerId() != null) {
            registered.getConfig().decorate(FabricUtil.KEY_MOD_ID, declaration.ownerId());
        }
        LOGGER.trace("Registered Mixin configuration [{}] for {}", registered.getName(), declaration.mixinClass());
    }

    /**
     * Requests another configuration selection pass from Mixin.
     *
     * <p>Mixin only looks for newly registered configurations while no mixin has been applied in the current phase.
     * Avrix can register plugin mixins after game classes have already been transformed, so the processed counter is
     * reset to let the next transform pick up newly registered configurations.
     */
    private static void requestConfigSelection() {
        Object activeTransformer = MixinEnvironment.getCurrentEnvironment().getActiveTransformer();
        if (activeTransformer == null) {
            return;
        }

        try {
            Field processorField = activeTransformer.getClass().getDeclaredField("processor");
            processorField.setAccessible(true);
            Object processor = processorField.get(activeTransformer);

            Field transformedCountField = processor.getClass().getDeclaredField("transformedCount");
            transformedCountField.setAccessible(true);
            transformedCountField.setInt(processor, 0);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("Unable to request a new Mixin configuration selection pass", e);
        }
    }

    private record MixinDeclaration(
            String mixinClass,
            String packageName,
            String simpleName,
            List<String> targets,
            String ownerId
    ) {
    }

    private String mixinConfig(String packageName, String simpleName, long sequence) {
        int priority = BASE_PRIORITY + (int) (sequence % 100_000L);
        return new StringBuilder(192)
                .append('{')
                .append("\"required\":").append(strict).append(',')
                .append("\"target\":\"@env(").append(environment.getPhase()).append(")\",")
                .append("\"package\":\"").append(packageName).append("\",")
                .append("\"compatibility\":\"JAVA_25\",")
                .append("\"priority\":").append(priority).append(',')
                .append("\"mixins\":[\"").append(simpleName).append("\"]")
                .append('}')
                .toString();
    }
}
