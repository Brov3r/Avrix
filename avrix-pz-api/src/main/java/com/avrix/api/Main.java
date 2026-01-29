package com.avrix.api;

import com.avrix.plugins.Metadata;
import com.avrix.plugins.Plugin;
import org.tinylog.Logger;

/**
 * Project Zomboid API Plugin
 */
public class Main extends Plugin {
    /**
     * Creates a plugin instance.
     *
     * @param metadata plugin metadata; must not be {@code null}
     * @throws NullPointerException     if {@code metadata} is {@code null}
     * @throws IllegalArgumentException if {@code metadata.getId()} is blank
     */
    protected Main(Metadata metadata) {
        super(metadata);
    }

    /**
     * Called during plugin initialization phase.
     */
    @Override
    public void onInitialize() {
        Logger.info("Zomboid API initialized!");
    }

    /**
     * Called when the plugin is launched (runtime start).
     */
    @Override
    public void onLaunch() {
        Logger.info("Zomboid API launched!");
    }
}