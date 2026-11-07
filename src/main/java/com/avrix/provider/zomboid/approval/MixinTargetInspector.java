package com.avrix.provider.zomboid.approval;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Reads the target classes of declared mixin classes from a plugin archive without loading plugin code.
 * <p>
 * The approval window runs before plugins are loaded, so class loading is not an option. The declared mixin class
 * names arrive through the approval protocol; this inspector uses ASM to read their {@code @Mixin} or
 * {@code @CTransformer} annotations.
 */
public final class MixinTargetInspector {

    private static final String MIXIN_DESCRIPTOR = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String C_TRANSFORMER_DESCRIPTOR =
            "Lnet/lenni0451/classtransform/annotations/CTransformer;";
    private static final String VALUE = "value";
    private static final String MIXIN_TARGETS = "targets";
    private static final String C_TRANSFORMER_NAMES = "name";

    private MixinTargetInspector() {
        throw new UnsupportedOperationException("MixinTargetInspector utility class cannot be instantiated");
    }

    /**
     * Inspects a plugin archive for the targets of its declared mixin classes.
     *
     * @param jarPath      plugin archive path
     * @param mixinClasses mixin transformer class names declared in the plugin metadata
     * @return inspection result, never {@code null}
     * @throws NullPointerException if {@code jarPath} is {@code null}
     */
    public static Result inspect(Path jarPath, List<String> mixinClasses) {
        Objects.requireNonNull(jarPath, "JAR path cannot be null");

        List<String> mixins = mixinClasses == null ? List.of() : mixinClasses.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
        List<String> issues = new ArrayList<>();
        try (JarFile jar = new JarFile(jarPath.toFile(), false, JarFile.OPEN_READ, Runtime.version())) {
            Set<String> targets = new LinkedHashSet<>();

            for (String mixin : mixins) {
                String entryName = mixin.replace('.', '/') + ".class";
                JarEntry entry = jar.getJarEntry(entryName);
                if (entry == null) {
                    issues.add("Mixin class not found: " + mixin);
                    continue;
                }

                try (InputStream input = jar.getInputStream(entry)) {
                    byte[] bytes = input.readNBytes(ApprovalLimits.MAX_CLASS_BYTES + 1);
                    if (bytes.length > ApprovalLimits.MAX_CLASS_BYTES) {
                        issues.add("Mixin class exceeds inspection byte limit: " + mixin);
                        continue;
                    }
                    collectTargets(bytes, targets);
                } catch (IOException | RuntimeException e) {
                    issues.add("Cannot read mixin class " + mixin + ": " + message(e));
                }
            }

            return new Result(mixins, targets.stream().sorted().toList(), issues);
        } catch (IOException | RuntimeException e) {
            return new Result(List.of(), List.of(), List.of("Cannot read plugin archive: " + message(e)));
        }
    }

    private static void collectTargets(byte[] classBytes, Set<String> targets) {
        ClassReader reader = new ClassReader(classBytes);
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            /**
             * Visits a class annotation.
             *
             * @param descriptor annotation descriptor
             * @param visible    whether the annotation is visible at runtime
             * @return visitor for annotation values, or {@code null} when the annotation is not a mixin annotation
             */
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                return switch (descriptor) {
                    case MIXIN_DESCRIPTOR -> annotationVisitor(VALUE, MIXIN_TARGETS, targets);
                    case C_TRANSFORMER_DESCRIPTOR -> annotationVisitor(VALUE, C_TRANSFORMER_NAMES, targets);
                    default -> null;
                };
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    private static AnnotationVisitor annotationVisitor(
            String classArrayName,
            String stringArrayName,
            Set<String> targets
    ) {
        return new AnnotationVisitor(Opcodes.ASM9) {
            /**
             * Visits a named annotation value.
             *
             * @param name  value name
             * @param value value
             */
            @Override
            public void visit(String name, Object value) {
                addTarget(name, value, classArrayName, stringArrayName, targets);
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
                        addTarget(name, value, classArrayName, stringArrayName, targets);
                    }
                };
            }
        };
    }

    private static void addTarget(
            String name,
            Object value,
            String classArrayName,
            String stringArrayName,
            Set<String> targets
    ) {
        if (targets.size() >= ApprovalLimits.MAX_MIXINS) {
            return;
        }
        if (classArrayName.equals(name) && value instanceof Type type) {
            targets.add(ApprovalLimits.displayText(type.getClassName()));
        } else if (stringArrayName.equals(name) && value instanceof String text && !text.isBlank()) {
            targets.add(ApprovalLimits.displayText(text.strip()));
        }
    }

    private static String message(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName()
                : ApprovalLimits.displayText(message);
    }

    /**
     * Declared mixin classes, their target classes, and non-fatal inspection notes.
     *
     * @param mixinClasses  mixin class names declared in {@code metadata.yml}
     * @param targetClasses unique class names found in mixin annotations
     * @param issues        non-fatal problems found while reading the archive
     */
    public record Result(List<String> mixinClasses, List<String> targetClasses, List<String> issues) {

        /**
         * Creates an immutable inspection result.
         */
        public Result {
            mixinClasses = boundedDisplay(mixinClasses);
            targetClasses = boundedDisplay(targetClasses);
            issues = boundedDisplay(issues);
        }

        private static List<String> boundedDisplay(List<String> values) {
            return values == null ? List.of() : values.stream().limit(ApprovalLimits.MAX_MIXINS)
                    .map(ApprovalLimits::displayText).toList();
        }
    }
}
