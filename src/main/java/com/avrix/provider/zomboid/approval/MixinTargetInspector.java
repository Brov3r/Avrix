package com.avrix.provider.zomboid.approval;

import com.avrix.mixin.MixinIntrospector;

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
 * names arrive through the approval protocol; this inspector uses ASM to read their {@code @Mixin} annotations.
 */
public final class MixinTargetInspector {

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
                    collectTargets(mixin, bytes, targets);
                } catch (IOException | RuntimeException e) {
                    issues.add("Cannot read mixin class " + mixin + ": " + message(e));
                }
            }

            return new Result(mixins, targets.stream().sorted().toList(), issues);
        } catch (IOException | RuntimeException e) {
            return new Result(List.of(), List.of(), List.of("Cannot read plugin archive: " + message(e)));
        }
    }

    private static void collectTargets(String mixin, byte[] classBytes, Set<String> targets) {
        MixinIntrospector.Result result = MixinIntrospector.scan(classBytes);
        if (!result.mixin()) {
            throw new IllegalArgumentException("Class is not annotated with @Mixin");
        }
        if (result.targetClasses().isEmpty()) {
            throw new IllegalArgumentException("Mixin declares no target classes");
        }
        for (String target : result.targetClasses()) {
            if (targets.size() >= ApprovalLimits.MAX_MIXINS) {
                return;
            }
            targets.add(ApprovalLimits.displayText(target));
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
         *
         * @param mixinClasses  declared mixin class names
         * @param targetClasses unique target class names
         * @param issues        non-fatal inspection notes
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
