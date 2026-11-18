package com.avrix.mixin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.logging.LoggerAdapterAbstract;

import java.util.Objects;

/**
 * Mixin logger adapter that forwards messages to SLF4J.
 */
final class Slf4jMixinLogger extends LoggerAdapterAbstract {

    private final Logger logger;

    /**
     * Creates a logger adapter.
     *
     * @param name Mixin logger name
     * @throws NullPointerException if {@code name} is null
     */
    Slf4jMixinLogger(String name) {
        super(Objects.requireNonNull(name, "Logger name cannot be null"));
        this.logger = LoggerFactory.getLogger(name.startsWith("mixin") ? name : "mixin." + name);
    }

    /**
     * Returns the backend logger type.
     *
     * @return adapter type
     */
    @Override
    public String getType() {
        return "SLF4J";
    }

    /**
     * Logs a caught exception.
     *
     * @param level     severity level
     * @param throwable caught exception
     */
    @Override
    public void catching(Level level, Throwable throwable) {
        log(level, "Caught exception", throwable);
    }

    /**
     * Logs a formatted message.
     *
     * @param level   severity level
     * @param message message template
     * @param params  template parameters
     */
    @Override
    public void log(Level level, String message, Object... params) {
        switch (level) {
            case FATAL, ERROR -> logger.error(message, params);
            case WARN -> logger.warn(message, params);
            case INFO -> logger.info(message, params);
            case DEBUG -> logger.debug(message, params);
            case TRACE -> logger.trace(message, params);
            default -> logger.debug(message, params);
        }
    }

    /**
     * Logs a message with an exception.
     *
     * @param level     severity level
     * @param message   message template
     * @param throwable exception to log
     */
    @Override
    public void log(Level level, String message, Throwable throwable) {
        switch (level) {
            case FATAL, ERROR -> logger.error(message, throwable);
            case WARN -> logger.warn(message, throwable);
            case INFO -> logger.info(message, throwable);
            case DEBUG -> logger.debug(message, throwable);
            case TRACE -> logger.trace(message, throwable);
            default -> logger.debug(message, throwable);
        }
    }

    /**
     * Logs and returns a throwable that is about to be thrown.
     *
     * @param throwable throwable about to be thrown
     * @param <T>       throwable type
     * @return the supplied throwable
     */
    @Override
    public <T extends Throwable> T throwing(T throwable) {
        log(Level.ERROR, "Throwing exception", throwable);
        return throwable;
    }
}
