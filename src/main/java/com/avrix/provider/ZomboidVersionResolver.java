package com.avrix.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves Project Zomboid versions from game class bytecode.
 */
public final class ZomboidVersionResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZomboidVersionResolver.class);

    private static final Pattern NORMALIZED_VERSION_PATTERN =
            Pattern.compile("^(\\d+)\\.(\\d+)(?:\\+[^.]*)?\\.(\\d+)");

    /**
     * Constructs a new version resolver.
     */
    public ZomboidVersionResolver() {
    }

    /**
     * Resolves the raw game version from {@code zombie.core.Core} and {@code zombie.GitVersion}.
     *
     * @param targetLoader classloader containing the game classes
     * @return raw version string, or {@code 0.0.0} when version data is unavailable
     */
    public String resolveRawVersion(ClassLoader targetLoader) {
        int major = 0;
        int minor = 0;
        String extra = "";
        String patch = "0";
        String revision = "";

        try (InputStream stream = targetLoader.getResourceAsStream("zombie/core/Core.class")) {
            if (stream != null) {
                ClassModel classModel = ClassFile.of().parse(stream.readAllBytes());
                major = readMajor(classModel);
                minor = readMinor(classModel);
                extra = readSuffix(classModel);
                patch = readPatch(classModel);
            }
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to parse version from zombie/core/Core.class", throwable);
        }

        try (InputStream gitStream = targetLoader.getResourceAsStream("zombie/GitVersion.class")) {
            if (gitStream != null) {
                ClassModel gitModel = ClassFile.of().parse(gitStream.readAllBytes());
                revision = readRevision(gitModel);
            }
        } catch (Throwable throwable) {
            LOGGER.debug("GitVersion metadata unavailable in current installation", throwable);
        }

        String extraSegment = extra.isBlank() ? "" : "+" + extra;
        String revisionSegment = revision.isBlank() ? "" : " " + revision;
        return "%d.%d%s.%s%s".formatted(major, minor, extraSegment, patch, revisionSegment);
    }

    /**
     * Normalizes a raw game version into a three-component SemVer-like string.
     *
     * @param rawVersion raw version string
     * @return normalized version, or {@code 0.0.0} when the raw value cannot be parsed
     */
    public String resolveNormalizedVersion(String rawVersion) {
        Matcher matcher = NORMALIZED_VERSION_PATTERN.matcher(rawVersion == null ? "" : rawVersion);
        if (matcher.find()) {
            return matcher.group(1) + "." + matcher.group(2) + "." + matcher.group(3);
        }

        LOGGER.warn("Unable to parse semantic version from raw string '{}'. Fallback to '0.0.0'", rawVersion);
        return "0.0.0";
    }

    private static int readMajor(ClassModel classModel) {
        return readGameVersionValue(classModel, 0);
    }

    private static int readMinor(ClassModel classModel) {
        return readGameVersionValue(classModel, 1);
    }

    private static String readSuffix(ClassModel classModel) {
        Object value = readGameVersionArgument(classModel, 2);
        return value instanceof String string ? string : "";
    }

    private static int readGameVersionValue(ClassModel classModel, int index) {
        Object value = readGameVersionArgument(classModel, index);
        return value instanceof Integer integer ? integer : 0;
    }

    private static Object readGameVersionArgument(ClassModel classModel, int argumentIndex) {
        for (MethodModel method : classModel.methods()) {
            if (!"<clinit>".equals(method.methodName().stringValue())) {
                continue;
            }

            Optional<CodeModel> codeOptional = method.code();
            if (codeOptional.isEmpty()) {
                return null;
            }

            List<Object> constantStack = new ArrayList<>();
            for (CodeElement element : codeOptional.get()) {
                if (element instanceof ConstantInstruction constantInstruction) {
                    constantStack.add(constantInstruction.constantValue());
                } else if (element instanceof InvokeInstruction invokeInstruction) {
                    if ("<init>".equals(invokeInstruction.name().stringValue())
                            && "zombie/core/GameVersion".equals(invokeInstruction.owner().asInternalName())
                            && constantStack.size() >= 3) {
                        return constantStack.get(constantStack.size() - 3 + argumentIndex);
                    }
                }
            }
            return null;
        }
        return null;
    }

    private static String readPatch(ClassModel classModel) {
        Pattern patchPattern = Pattern.compile("^\\.([0-9]{1,4})$");
        for (PoolEntry poolEntry : classModel.constantPool()) {
            if (poolEntry instanceof Utf8Entry utf8Entry) {
                Matcher matcher = patchPattern.matcher(utf8Entry.stringValue().trim());
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        return "0";
    }

    private static String readRevision(ClassModel classModel) {
        for (PoolEntry poolEntry : classModel.constantPool()) {
            if (poolEntry instanceof Utf8Entry utf8Entry) {
                String value = utf8Entry.stringValue().trim();
                if (value.length() >= 7 && value.length() <= 40 && value.matches("^[0-9a-fA-F]+$")) {
                    return value;
                }
            }
        }
        return "";
    }
}
