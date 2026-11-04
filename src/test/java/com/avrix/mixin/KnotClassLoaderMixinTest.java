package com.avrix.mixin;

import com.avrix.core.KnotClassLoader;
import com.avrix.core.ServiceManager;
import net.lenni0451.classtransform.utils.FailStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import test.game.FirstPipelineInjectMixin;
import test.game.SecondPipelineInjectMixin;
import test.game.TargetGameService;
import test.game.TargetGameServiceMixin;

import java.io.File;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end integration test validating bytecode transformations on the fly
 * within the unified {@link KnotClassLoader} via ClassTransform and MixinsTranslator.
 */
@DisplayName("KnotClassLoader & Mixin Integration Tests")
class KnotClassLoaderMixinTest {

    private KnotClassLoader knotClassLoader;
    private MixinTransformer mixinTransformer;

    @BeforeEach
    void setUp() {
        ServiceManager.clear();

        URL[] testClasspathUrls = resolveCurrentClasspathUrls();

        this.knotClassLoader = new KnotClassLoader(testClasspathUrls, getClass().getClassLoader());
        ServiceManager.register(KnotClassLoader.class, knotClassLoader);

        this.mixinTransformer = new MixinTransformer(knotClassLoader);
        knotClassLoader.setClassTransformer(mixinTransformer);
    }

    @AfterEach
    void tearDown() throws Exception {
        ServiceManager.clear();
        mixinTransformer = null;
        if (knotClassLoader != null) {
            knotClassLoader.close();
            knotClassLoader = null;
        }
    }

    @Test
    @DisplayName("Should configure the transformation failure strategy from the strict flag")
    void shouldConfigureTransformationFailureStrategy() {
        assertThat(mixinTransformer.getManager().getFailStrategy()).isEqualTo(FailStrategy.CANCEL);
        assertThat(new MixinTransformer(knotClassLoader, true).getManager().getFailStrategy())
                .isEqualTo(FailStrategy.THROW);
    }

    @Test
    @DisplayName("Should successfully transform target class bytecode via registered mixin")
    void shouldTransformClassWithMixin() throws Throwable {
        String targetClassName = TargetGameService.class.getName();
        String mixinClassName = TargetGameServiceMixin.class.getName();

        mixinTransformer.addMixin(mixinClassName);

        Class<?> transformedClass = knotClassLoader.loadClass(targetClassName);
        assertThat(transformedClass.getClassLoader()).isSameAs(knotClassLoader);

        Object instance = transformedClass.getDeclaredConstructor().newInstance();

        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        MethodHandle getGreetingHandle = lookup.findVirtual(
                transformedClass,
                "getGreeting",
                MethodType.methodType(String.class, String.class)
        );

        String result = (String) getGreetingHandle.invoke(instance, "Brov3r");

        assertThat(result)
                .isEqualTo("Intercepted by Avrix Mixin: Brov3r")
                .doesNotContain("Hello, Brov3r");
    }

    @Test
    @DisplayName("Should sequentially execute multiple mixin injections on the same target method")
    @SuppressWarnings("unchecked")
    void shouldApplyMultipleInjectionsToSameMethod() throws Throwable {
        String targetClassName = TargetGameService.class.getName();

        // Register both independent mixins targeting the same method
        mixinTransformer.addMixin(FirstPipelineInjectMixin.class.getName());
        mixinTransformer.addMixin(SecondPipelineInjectMixin.class.getName());

        Class<?> transformedClass = knotClassLoader.loadClass(targetClassName);
        assertThat(transformedClass.getClassLoader()).isSameAs(knotClassLoader);

        Object instance = transformedClass.getDeclaredConstructor().newInstance();

        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        MethodHandle executePipelineHandle = lookup.findVirtual(
                transformedClass,
                "executePipeline",
                MethodType.methodType(String.class, List.class, String.class)
        );

        List<String> executionTrace = new ArrayList<>();
        String returnValue = (String) executePipelineHandle.invoke(instance, executionTrace, "SampleData");

        // Assert return value from original method is intact
        assertThat(returnValue).isEqualTo("PROCESSED: SampleData");

        // Verify that both HEAD and TAIL mixin injections executed in proper chronological order
        assertThat(executionTrace).containsExactly(
                "INJECT_FIRST: SampleData",
                "ORIGINAL_BODY: SampleData",
                "INJECT_SECOND: SampleData"
        );
    }

    @Test
    @DisplayName("Should load original unmodified bytecode when no mixin is registered")
    void shouldLoadUnmodifiedClassWithoutMixin() throws Throwable {
        String targetClassName = TargetGameService.class.getName();

        Class<?> rawClass = knotClassLoader.loadClass(targetClassName);
        Object instance = rawClass.getDeclaredConstructor().newInstance();

        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        MethodHandle getGreetingHandle = lookup.findVirtual(
                rawClass,
                "getGreeting",
                MethodType.methodType(String.class, String.class)
        );

        String result = (String) getGreetingHandle.invoke(instance, "Brov3r");

        assertThat(result).isEqualTo("Hello, Brov3r");
    }

    @Test
    @DisplayName("Should apply a custom ClassTransformer to locally defined classes")
    void shouldUseConfiguredClassTransformer() throws Exception {
        List<String> transformedClasses = new ArrayList<>();
        knotClassLoader.setClassTransformer((className, originalBytes) -> {
            transformedClasses.add(className);
            return null;
        });

        knotClassLoader.loadClass(TargetGameService.class.getName());

        assertThat(transformedClasses).contains(TargetGameService.class.getName());
    }

    @Test
    @DisplayName("Should expose the transformer manager and register mixin collections")
    void shouldExposeManagerAndRegisterMixinCollections() {
        assertThat(mixinTransformer.getManager()).isNotNull();
        assertThatThrownBy(() -> new MixinTransformer(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("KnotClassLoader");

        mixinTransformer.addMixins(List.of(TargetGameServiceMixin.class.getName()));

        assertThatThrownBy(() -> mixinTransformer.addMixins(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Mixins collection");
    }

    @Test
    @DisplayName("Should reject blank mixin class names")
    void shouldRejectBlankMixinName() {
        assertThatThrownBy(() -> mixinTransformer.addMixin(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mixin class name cannot be blank");
    }

    private static URL[] resolveCurrentClasspathUrls() {
        String classpathProperty = System.getProperty("java.class.path", "");
        if (classpathProperty.isBlank()) {
            return new URL[0];
        }

        return Arrays.stream(classpathProperty.split(Pattern.quote(File.pathSeparator)))
                .filter(pathStr -> !pathStr.isBlank())
                .map(Paths::get)
                .map(Path::toUri)
                .map(uri -> {
                    try {
                        return uri.toURL();
                    } catch (MalformedURLException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .toArray(URL[]::new);
    }
}