package com.avrix.mixin;

import org.spongepowered.asm.launch.platform.container.IContainerHandle;

import java.util.Collection;
import java.util.List;

/**
 * Minimal Mixin platform container describing the Avrix loader.
 */
final class MixinHostContainer implements IContainerHandle {

    /**
     * Returns the container identifier.
     *
     * @return stable identifier
     */
    @Override
    public String getId() {
        return "avrix";
    }

    /**
     * Returns a human-readable container description.
     *
     * @return description
     */
    @Override
    public String getDescription() {
        return "Avrix loader";
    }

    /**
     * Returns a container attribute.
     *
     * @param name attribute name
     * @return always {@code null} because Avrix exposes no platform attributes
     */
    @Override
    public String getAttribute(String name) {
        return null;
    }

    /**
     * Returns nested containers.
     *
     * @return empty collection
     */
    @Override
    public Collection<IContainerHandle> getNestedContainers() {
        return List.of();
    }
}
