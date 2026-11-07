package com.avrix.provider.zomboid;

import com.avrix.provider.GameVersionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeElement;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.attribute.ConstantValueAttribute;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.InvokeDynamicInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves Project Zomboid versions from game class bytecode without loading or initializing game classes.
 */
public final class ZomboidVersionResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZomboidVersionResolver.class);

    private static final String CORE_CLASS = "zombie/core/Core.class";
    private static final String GIT_VERSION_CLASS = "zombie/GitVersion.class";
    private static final String GAME_VERSION_CLASS = "zombie/core/GameVersion";
    private static final String GAME_AND_BUILD_VERSION_METHOD = "getGameAndBuildVersion";
    private static final String BUILD_VERSION_FIELD = "buildVersion";
    private static final String REVISION_FIELD = "REVISION";

    private static final Pattern NORMALIZED_VERSION_PATTERN =
            Pattern.compile("^(\\d+)\\.(\\d+)(?:[+-][^.]*)?\\.(\\d+)");
    private static final Pattern PATCH_RECIPE_PATTERN = Pattern.compile("^\\.(\\d+)");
    private static final Pattern HEX_REVISION_PATTERN = Pattern.compile("^[0-9a-fA-F]{7,40}$");

    /**
     * Constructs a new version resolver.
     */
    public ZomboidVersionResolver() {
    }

    /**
     * Resolves the raw game version from the classloader-provided game classes.
     * <p>
     * This method reads class resources only; it never loads or initializes game classes.
     *
     * @param targetLoader classloader containing the game classes
     * @return raw version string, or {@code 0.0.0} when version data is unavailable
     */
    public String resolveRawVersion(ClassLoader targetLoader) {
        return resolve(targetLoader).raw();
    }

    /**
     * Resolves structured version information from a Project Zomboid game archive.
     *
     * @param gameArchive path to {@code projectzomboid.jar}
     * @return resolved version information, never {@code null}
     */
    public GameVersionInfo resolve(Path gameArchive) {
        if (gameArchive == null || !Files.isRegularFile(gameArchive)) {
            LOGGER.warn("Project Zomboid game archive not found: [{}]", gameArchive);
            return GameVersionInfo.fallback();
        }

        try (JarFile jarFile = new JarFile(gameArchive.toFile(), false)) {
            byte[] coreBytes = readEntry(jarFile, CORE_CLASS);
            byte[] gitVersionBytes = readEntry(jarFile, GIT_VERSION_CLASS);
            return resolveFromBytes(coreBytes, gitVersionBytes);
        } catch (IOException e) {
            LOGGER.warn("Failed to read Project Zomboid version from [{}]", gameArchive, e);
            return GameVersionInfo.fallback();
        }
    }

    /**
     * Resolves structured version information from a classloader without loading game classes.
     *
     * @param targetLoader classloader containing the game classes
     * @return resolved version information, never {@code null}
     */
    public GameVersionInfo resolve(ClassLoader targetLoader) {
        if (targetLoader == null) {
            return GameVersionInfo.fallback();
        }

        try {
            URL coreResource = targetLoader.getResource(CORE_CLASS);
            if (coreResource != null && "jar".equalsIgnoreCase(coreResource.getProtocol())) {
                JarURLConnection connection = (JarURLConnection) coreResource.openConnection();
                connection.setUseCaches(false);
                URL jarFileUrl = connection.getJarFileURL();
                if (jarFileUrl != null) {
                    return resolve(Paths.get(jarFileUrl.toURI()));
                }
            }

            byte[] coreBytes = readResource(targetLoader, CORE_CLASS);
            byte[] gitVersionBytes = readResource(targetLoader, GIT_VERSION_CLASS);
            return resolveFromBytes(coreBytes, gitVersionBytes);
        } catch (Exception e) {
            LOGGER.debug("Failed to resolve Project Zomboid version from classloader", e);
            return GameVersionInfo.fallback();
        }
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

    private static GameVersionInfo resolveFromBytes(byte[] coreBytes, byte[] gitVersionBytes) {
        if (coreBytes == null) {
            LOGGER.warn("Project Zomboid Core class is unavailable");
            return GameVersionInfo.fallback();
        }

        try {
            ClassModel core = ClassFile.of().parse(coreBytes);
            GameVersionData gameVersion = readGameVersion(core);
            int patch = readPatch(core);
            String revision = gitVersionBytes == null
                    ? ""
                    : readRevision(ClassFile.of().parse(gitVersionBytes));

            String suffix = gameVersion.suffix();
            String raw = gameVersion.major() + "." + gameVersion.minor() + suffix + "." + patch;
            if (!revision.isBlank()) {
                raw += " " + revision;
            }
            String normalized = gameVersion.major() + "." + gameVersion.minor() + "." + patch;
            return new GameVersionInfo(
                    gameVersion.major(),
                    gameVersion.minor(),
                    patch,
                    suffix,
                    revision,
                    raw,
                    normalized
            );
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to parse Project Zomboid version bytecode", throwable);
            return GameVersionInfo.fallback();
        }
    }

    private static GameVersionData readGameVersion(ClassModel core) {
        Optional<MethodModel> classInitializer = core.methods().stream()
                .filter(method -> "<clinit>".equals(method.methodName().stringValue()))
                .findFirst();
        if (classInitializer.isEmpty() || classInitializer.get().code().isEmpty()) {
            return GameVersionData.fallback();
        }

        boolean collecting = false;
        List<Object> constants = new ArrayList<>();
        for (CodeElement element : classInitializer.get().code().orElseThrow()) {
            if (element instanceof NewObjectInstruction newObject) {
                if (GAME_VERSION_CLASS.equals(newObject.className().asInternalName())) {
                    collecting = true;
                    constants.clear();
                }
                continue;
            }

            if (!collecting) {
                continue;
            }

            if (element instanceof ConstantInstruction constantInstruction) {
                constants.add(constantInstruction.constantValue());
                continue;
            }

            if (element instanceof InvokeInstruction invokeInstruction
                    && "<init>".equals(invokeInstruction.name().stringValue())
                    && GAME_VERSION_CLASS.equals(invokeInstruction.owner().asInternalName())) {
                GameVersionData data = findGameVersionArguments(constants);
                if (data != null) {
                    return data;
                }
                collecting = false;
                constants.clear();
            }
        }

        return GameVersionData.fallback();
    }

    private static GameVersionData findGameVersionArguments(List<Object> constants) {
        for (int index = constants.size() - 3; index >= 0; index--) {
            Object major = constants.get(index);
            Object minor = constants.get(index + 1);
            Object suffix = constants.get(index + 2);
            if (major instanceof Integer majorValue
                    && minor instanceof Integer minorValue
                    && suffix instanceof String suffixValue) {
                return new GameVersionData(majorValue, minorValue, suffixValue);
            }
        }
        return null;
    }

    private static int readPatch(ClassModel core) {
        Integer recipePatch = readPatchFromGameAndBuildVersion(core);
        Integer buildVersion = readIntField(core, BUILD_VERSION_FIELD);

        if (recipePatch != null && buildVersion != null && !recipePatch.equals(buildVersion)) {
            LOGGER.warn(
                    "Project Zomboid patch mismatch: getGameAndBuildVersion={}, buildVersion={}. Using recipe value.",
                    recipePatch,
                    buildVersion
            );
        }
        if (recipePatch != null) {
            return recipePatch;
        }
        if (buildVersion != null) {
            return buildVersion;
        }
        return 0;
    }

    private static Integer readPatchFromGameAndBuildVersion(ClassModel core) {
        Optional<MethodModel> method = core.methods().stream()
                .filter(candidate -> GAME_AND_BUILD_VERSION_METHOD.equals(candidate.methodName().stringValue()))
                .findFirst();
        if (method.isEmpty() || method.get().code().isEmpty()) {
            return null;
        }

        for (CodeElement element : method.get().code().orElseThrow()) {
            if (!(element instanceof InvokeDynamicInstruction invokeDynamic)) {
                continue;
            }
            for (Object argument : invokeDynamic.bootstrapArgs()) {
                if (argument instanceof String recipe) {
                    Integer patch = parsePatchRecipe(recipe);
                    if (patch != null) {
                        return patch;
                    }
                }
            }
        }
        return null;
    }

    private static Integer parsePatchRecipe(String recipe) {
        int placeholder = recipe.indexOf('\u0001');
        if (placeholder < 0) {
            return null;
        }

        Matcher matcher = PATCH_RECIPE_PATTERN.matcher(recipe.substring(placeholder + 1));
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static Integer readIntField(ClassModel model, String fieldName) {
        for (FieldModel field : model.fields()) {
            if (!fieldName.equals(field.fieldName().stringValue())) {
                continue;
            }

            Optional<ConstantValueAttribute> constantValue = field.findAttribute(Attributes.constantValue());
            if (constantValue.isPresent()) {
                Object value = constantValue.get().constant().constantValue();
                if (value instanceof Integer integer) {
                    return integer;
                }
            }
        }
        return null;
    }

    private static String readRevision(ClassModel gitVersion) {
        for (FieldModel field : gitVersion.fields()) {
            if (!REVISION_FIELD.equals(field.fieldName().stringValue())) {
                continue;
            }

            Optional<ConstantValueAttribute> constantValue = field.findAttribute(Attributes.constantValue());
            if (constantValue.isPresent()) {
                Object value = constantValue.get().constant().constantValue();
                if (value instanceof String revision && !revision.isBlank()) {
                    return revision.trim();
                }
            }
        }

        for (PoolEntry poolEntry : gitVersion.constantPool()) {
            if (poolEntry instanceof Utf8Entry utf8Entry) {
                String value = utf8Entry.stringValue().trim();
                if (HEX_REVISION_PATTERN.matcher(value).matches()) {
                    return value;
                }
            }
        }
        return "";
    }

    private static byte[] readEntry(JarFile jarFile, String entryName) throws IOException {
        JarEntry entry = jarFile.getJarEntry(entryName);
        if (entry == null) {
            return null;
        }

        try (InputStream inputStream = jarFile.getInputStream(entry)) {
            return inputStream.readAllBytes();
        }
    }

    private static byte[] readResource(ClassLoader targetLoader, String resourceName) {
        try (InputStream inputStream = targetLoader.getResourceAsStream(resourceName)) {
            return inputStream == null ? null : inputStream.readAllBytes();
        } catch (IOException e) {
            LOGGER.debug("Failed to read class resource [{}] from classloader", resourceName, e);
            return null;
        }
    }

    private record GameVersionData(int major, int minor, String suffix) {

        private static GameVersionData fallback() {
            return new GameVersionData(0, 0, "");
        }
    }
}
