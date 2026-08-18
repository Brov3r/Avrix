package com.avrix.mixin;

import com.avrix.core.KnotClassLoader;
import com.avrix.core.ServiceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import java.util.Arrays;
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

    @BeforeEach
    void setUp() {
        ServiceManager.clear();
        MixinTransformer.reset();

        URL[] testClasspathUrls = resolveCurrentClasspathUrls();

        this.knotClassLoader = new KnotClassLoader(testClasspathUrls, getClass().getClassLoader());
        ServiceManager.register(KnotClassLoader.class, knotClassLoader);

        MixinTransformer.init(knotClassLoader);
    }

    @AfterEach
    void tearDown() {
        ServiceManager.clear();
        MixinTransformer.reset();
    }

    @Test
    @DisplayName("Should successfully transform target class bytecode via registered mixin")
    void shouldTransformClassWithMixin() throws Throwable {
        String targetClassName = TargetGameService.class.getName();
        String mixinClassName = TargetGameServiceMixin.class.getName();

        MixinTransformer.addMixin(mixinClassName);

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
    @DisplayName("Should load original unmodified bytecode when no mixin is registered")
    void shouldLoadUnmodifiedClassWithoutMixin() throws Throwable {
        String targetClassName = TargetGameService.class.getName();

        // Load class without adding any mixins
        Class<?> rawClass = knotClassLoader.loadClass(targetClassName);
        Object instance = rawClass.getDeclaredConstructor().newInstance();

        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        MethodHandle getGreetingHandle = lookup.findVirtual(
                rawClass,
                "getGreeting",
                MethodType.methodType(String.class, String.class)
        );

        String result = (String) getGreetingHandle.invoke(instance, "Brov3r");

        // Assert original implementation is preserved
        assertThat(result).isEqualTo("Hello, Brov3r");
    }

    @Test
    @DisplayName("Should prevent mixin registration when environment is not initialized")
    void shouldFailRegisteringMixinWhenNotInitialized() {
        MixinTransformer.reset();

        assertThatThrownBy(() -> MixinTransformer.addMixin("test.game.NonExistentMixin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MixinTransformer is not initialized");
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