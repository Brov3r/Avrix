package com.avrix.core;

/**
 * Transforms class bytecode before it is defined by a class loader.
 */
public interface ClassTransformer {

    /**
     * Transforms the bytecode of a class.
     *
     * @param className    binary name of the class
     * @param originalBytes original class bytes
     * @return transformed class bytes, or {@code null} if no transformation is required
     */
    byte[] transform(String className, byte[] originalBytes);
}
