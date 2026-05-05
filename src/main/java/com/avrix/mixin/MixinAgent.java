package com.avrix.mixin;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.mixinstranslator.MixinsTranslator;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.util.List;

/**
 * Java Agent bridge for dynamic Mixin injection via ClassTransform.
 */
public class MixinAgent {
    private static final Logger log = LoggerFactory.getLogger(MixinAgent.class);

    private static volatile TransformerManager manager;
    private static volatile Instrumentation instrumentation;

    /**
     * JVM entry point for dynamic agent attachment.
     * <p>
     * Initializes the {@link TransformerManager}, flushes pending mixins,
     * and hooks into the instrumentation API.
     *
     * @param agentArgs agent arguments (reserved, currently unused)
     * @param inst      the instrumentation instance provided by the JVM
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        log.info("Attaching MixinAgent...");

        ClassLoader loader = MixinAgentBootstrap.getClassLoader();
        if (loader == null) {
            log.error("MixinAgent cannot be attached because the ClassLoader is null. Call 'MixinAgentBootstrap.loadAgent()' first.");
            return;
        }

        instrumentation = inst;

        manager = new TransformerManager(new BasicClassProvider(loader));
        manager.addTransformerPreprocessor(new MixinsTranslator());

        try {
            manager.hookInstrumentation(inst);
            log.info("MixinAgent hooked successfully.");
        } catch (Exception e) {
            log.error("Failed to hook MixinAgent instrumentation.", e);
            manager = null; // Rollback state
        }
    }

    /**
     * Registers a single mixin configuration class.
     * <p>
     * Safe to call before or after agent attachment. If called prior to attachment,
     * the mixin is queued and applied automatically during {@link #agentmain}.
     *
     * @param mixin fully qualified name of the mixin config class
     */
    public synchronized static void addMixin(String mixin) {
        if (manager == null || mixin == null || mixin.isBlank()) return;

        manager.addTransformer(mixin);

        log.debug("Registered mixin transformer: {}", mixin);
    }

    /**
     * Registers multiple mixin configuration classes.
     *
     * @param mixins list of fully qualified mixin config class names
     */
    public synchronized static void addMixins(List<String> mixins) {
        if (mixins == null) return;
        for (String mixin : mixins) {
            addMixin(mixin);
        }
    }

    /**
     * Returns {@code true} if the {@link MixinAgent} is successfully attached and hooked.
     *
     * @return attachment status
     */
    public static boolean isAttached() {
        return instrumentation != null && manager != null;
    }
}