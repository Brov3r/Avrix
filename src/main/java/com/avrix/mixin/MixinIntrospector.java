package com.avrix.mixin;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reads target declarations from SpongePowered {@code @Mixin} annotations without loading the mixin class.
 *
 * <p>Avrix registers mixin classes individually through plugin metadata, so the loader needs the target names before
 * the class is defined. This scanner also performs the registration-time check that a declared class really is a
 * mixin.
 */
public final class MixinIntrospector {

    private static final String MIXIN_DESCRIPTOR = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String VALUE = "value";
    private static final String TARGETS = "targets";

    private MixinIntrospector() {
        throw new UnsupportedOperationException("MixinIntrospector utility class cannot be instantiated");
    }

    /**
     * Scans a class file for a {@code @Mixin} annotation and returns its declared target classes.
     *
     * @param classBytes raw class file bytes
     * @return scan result with the annotated state and declared target names
     * @throws NullPointerException   if {@code classBytes} is null
     * @throws IllegalArgumentException if the bytes are not a valid class file
     */
    public static Result scan(byte[] classBytes) {
        Objects.requireNonNull(classBytes, "Class bytes cannot be null");

        List<String> targets = new ArrayList<>();
        boolean[] annotated = {false};
        ClassReader reader;
        try {
            reader = new ClassReader(classBytes);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Class file could not be parsed", e);
        }

        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            /**
             * Visits a class annotation and collects mixin targets.
             *
             * @param descriptor annotation descriptor
             * @param visible    whether the annotation is visible at runtime
             * @return visitor for mixin annotation values, or {@code null} for unrelated annotations
             */
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (!MIXIN_DESCRIPTOR.equals(descriptor)) {
                    return null;
                }
                annotated[0] = true;
                return new AnnotationVisitor(Opcodes.ASM9) {
                    /**
                     * Visits a named annotation value.
                     *
                     * @param name  member name
                     * @param value member value
                     */
                    @Override
                    public void visit(String name, Object value) {
                        addTarget(name, value, targets);
                    }

                    /**
                     * Visits an array-valued annotation member.
                     *
                     * @param name member name
                     * @return visitor for array values
                     */
                    @Override
                    public AnnotationVisitor visitArray(String name) {
                        return new AnnotationVisitor(Opcodes.ASM9) {
                            /**
                             * Visits one array element.
                             *
                             * @param ignored element name, unused for array values
                             * @param value   array element value
                             */
                            @Override
                            public void visit(String ignored, Object value) {
                                addTarget(name, value, targets);
                            }
                        };
                    }
                };
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        return new Result(annotated[0], List.copyOf(targets));
    }

    /**
     * Scans a class file and requires it to declare at least one mixin target.
     *
     * @param mixinClass mixin class name used in diagnostics
     * @param classBytes raw class file bytes
     * @return declared target class names
     * @throws NullPointerException     if an argument is null
     * @throws IllegalArgumentException if the class is not a mixin or declares no targets
     */
    public static List<String> requireTargets(String mixinClass, byte[] classBytes) {
        Objects.requireNonNull(mixinClass, "Mixin class name cannot be null");
        Result result = scan(classBytes);
        if (!result.mixin()) {
            throw new IllegalArgumentException("Class [" + mixinClass + "] is not annotated with @Mixin");
        }
        if (result.targetClasses().isEmpty()) {
            throw new IllegalArgumentException("Mixin [" + mixinClass + "] does not declare any target classes");
        }
        return result.targetClasses();
    }

    private static void addTarget(String name, Object value, List<String> targets) {
        if (VALUE.equals(name) && value instanceof Type type) {
            addTarget(targets, type.getClassName());
        } else if (TARGETS.equals(name) && value instanceof String text) {
            addTarget(targets, text);
        }
    }

    private static void addTarget(List<String> targets, String target) {
        String normalized = target == null ? "" : target.strip();
        if (normalized.isEmpty()) {
            return;
        }
        String binaryName = normalized.replace('/', '.');
        if (!targets.contains(binaryName)) {
            targets.add(binaryName);
        }
    }

    /**
     * Result of scanning one class file for mixin target declarations.
     *
     * @param mixin         whether the class carries a {@code @Mixin} annotation
     * @param targetClasses unique target class names, in declaration order
     */
    public record Result(boolean mixin, List<String> targetClasses) {

        /**
         * Creates an immutable scan result.
         *
         * @param mixin         whether the class carries a {@code @Mixin} annotation
         * @param targetClasses unique target class names, in declaration order
         */
        public Result {
            targetClasses = targetClasses == null ? List.of() : List.copyOf(targetClasses);
        }
    }
}
