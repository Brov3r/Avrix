package com.avrix.mixin;

import com.avrix.core.KnotClassLoader;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.IAdviceProvider;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IFeatureValidator;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.ITransformer;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.service.MixinServiceAbstract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mixin service bridging the SpongePowered Mixin runtime to the Avrix flat class loader.
 *
 * <p>The service is discovered through the standard {@link IMixinService} service file. It provides Mixin with class
 * bytes, mixin class loading, and synthetic mixin configurations generated from plugin {@code metadata.yml}
 * declarations. The active class loader is process-wide because Mixin itself is process-wide; Avrix creates one
 * service per game process.
 */
public final class MixinHostService extends MixinServiceAbstract {

    private static final Logger LOGGER = LoggerFactory.getLogger(MixinHostService.class);

    private static final Map<String, String> SYNTHETIC_CONFIGS = new ConcurrentHashMap<>();
    private static volatile KnotClassLoader classLoader;

    private final CompositeClassProvider bytecodeProvider = new CompositeClassProvider(MixinHostService::classLoader);
    private final MixinHostClassProvider classProvider = new MixinHostClassProvider(MixinHostService::classLoader);
    private final MixinHostClassTracker classTracker = new MixinHostClassTracker(MixinHostService::classLoader);
    private final ITransformerProvider transformerProvider = new EmptyTransformerProvider();
    private final IContainerHandle primaryContainer = new MixinHostContainer();

    private boolean initialized;

    /**
     * Creates the Mixin host service discovered by the Java service loader.
     */
    public MixinHostService() {
    }

    /**
     * Configures the class loader used by the Mixin service.
     *
     * @param classLoader active Avrix class loader
     * @throws NullPointerException if {@code classLoader} is null
     */
    static void configure(KnotClassLoader classLoader) {
        MixinHostService.classLoader = Objects.requireNonNull(classLoader, "KnotClassLoader cannot be null");
    }

    /**
     * Returns the class loader used by the Mixin service.
     *
     * @return active class loader, or {@code null} before Avrix initializes Mixin
     */
    static KnotClassLoader classLoader() {
        return classLoader;
    }

    /**
     * Registers an in-memory mixin configuration resource.
     *
     * @param resourceName configuration resource name
     * @param json         configuration JSON
     * @throws NullPointerException if an argument is null
     */
    static void registerConfig(String resourceName, String json) {
        SYNTHETIC_CONFIGS.put(
                Objects.requireNonNull(resourceName, "Configuration resource name cannot be null"),
                Objects.requireNonNull(json, "Configuration JSON cannot be null")
        );
    }

    /**
     * Returns the service display name.
     *
     * @return service name
     */
    @Override
    public String getName() {
        return "Avrix";
    }

    /**
     * Checks whether the service can operate in the current process.
     *
     * @return {@code true} once an Avrix class loader has been configured
     */
    @Override
    public boolean isValid() {
        return classLoader != null;
    }

    /**
     * Creates the Mixin transformer and initializes platform agents.
     */
    @Override
    public synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        IMixinTransformerFactory transformerFactory = getInternal(IMixinTransformerFactory.class);
        if (transformerFactory == null) {
            LOGGER.error("Mixin transformer factory was not offered by the Mixin bootstrap");
        } else {
            transformerFactory.createTransformer();
        }

        super.init();
        LOGGER.info("Mixin host service initialized.");
    }

    /**
     * Returns the class provider backed by the Avrix class loader.
     *
     * @return class provider
     */
    @Override
    public IClassProvider getClassProvider() {
        return classProvider;
    }

    /**
     * Returns the bytecode provider backed by the Avrix class loader.
     *
     * @return bytecode provider
     */
    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return bytecodeProvider;
    }

    /**
     * Returns a provider with no host transformers.
     *
     * @return empty transformer provider
     */
    @Override
    public ITransformerProvider getTransformerProvider() {
        return transformerProvider;
    }

    /**
     * Returns the Avrix class tracker.
     *
     * @return class tracker
     */
    @Override
    public IClassTracker getClassTracker() {
        return classTracker;
    }

    /**
     * Returns the audit trail, if one is configured.
     *
     * @return always {@code null}
     */
    @Override
    public IMixinAuditTrail getAuditTrail() {
        return null;
    }

    /**
     * Returns the feature validator.
     *
     * @return validator that permits all features
     */
    @Override
    public IFeatureValidator getFeatureValidator() {
        return IFeatureValidator.ALLOW_ALL;
    }

    /**
     * Returns the advice provider used for compatibility diagnostics.
     *
     * @return generic advice provider
     */
    @Override
    public IAdviceProvider getAdviceProvider() {
        return IAdviceProvider.GENERIC;
    }

    /**
     * Returns the platform agents required by this service.
     *
     * @return empty list
     */
    @Override
    public Collection<String> getPlatformAgents() {
        return List.of();
    }

    /**
     * Returns the primary Mixin container.
     *
     * @return Avrix container
     */
    @Override
    public IContainerHandle getPrimaryContainer() {
        return primaryContainer;
    }

    /**
     * Returns additional Mixin containers discovered by the service.
     *
     * @return empty list
     */
    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        return List.of();
    }

    /**
     * Opens a class path resource, including synthetic mixin configurations.
     *
     * @param name resource path
     * @return resource stream, or {@code null} when the resource is absent
     */
    @Override
    public InputStream getResourceAsStream(String name) {
        if (name == null) {
            return null;
        }

        String syntheticConfig = SYNTHETIC_CONFIGS.get(name);
        if (syntheticConfig != null) {
            return new ByteArrayInputStream(syntheticConfig.getBytes(StandardCharsets.UTF_8));
        }

        KnotClassLoader loader = classLoader;
        return loader == null ? null : loader.getResourceAsStream(name);
    }

    /**
     * Creates a logger that forwards Mixin messages to SLF4J.
     *
     * @param name Mixin logger name
     * @return logger adapter
     */
    @Override
    protected ILogger createLogger(String name) {
        return new Slf4jMixinLogger(name);
    }

    /**
     * Returns the minimum compatibility level supported by the Avrix service.
     *
     * @return Java 25 compatibility level
     */
    @Override
    public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() {
        return MixinEnvironment.CompatibilityLevel.JAVA_25;
    }

    /**
     * Returns the maximum compatibility level supported by the Avrix service.
     *
     * @return Java 25 compatibility level
     */
    @Override
    public MixinEnvironment.CompatibilityLevel getMaxCompatibilityLevel() {
        return MixinEnvironment.CompatibilityLevel.JAVA_25;
    }

    private static final class EmptyTransformerProvider implements ITransformerProvider {

        @Override
        public Collection<ITransformer> getTransformers() {
            return List.of();
        }

        @Override
        public Collection<ITransformer> getDelegatedTransformers() {
            return List.of();
        }

        @Override
        public void addTransformerExclusion(String name) {
        }
    }
}
