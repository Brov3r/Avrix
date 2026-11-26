package com.avrix.mixin;

import com.avrix.core.KnotClassLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import test.game.TargetGameService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency tests for {@link CompositeClassProvider} cache invalidation.
 */
@DisplayName("CompositeClassProvider Concurrency Tests")
class CompositeClassProviderConcurrencyTest {

    private static final ClassLoader PARENT = CompositeClassProviderConcurrencyTest.class.getClassLoader();
    private static final String CLASS_NAME = "race.fixture.Generation";
    private static final String RESOURCE_PATH = CLASS_NAME.replace('.', '/') + ".class";

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should not publish bytes read from a replaced class loader into the refreshed cache")
    void shouldNotPublishStaleBytesAfterLoaderReplacement() throws Exception {
        byte[] firstBytes = fixtureClass(CLASS_NAME, "first");
        byte[] secondBytes = fixtureClass(CLASS_NAME, "second");
        Path firstDirectory = writeClass("first", firstBytes);
        Path secondDirectory = writeClass("second", secondBytes);

        CountDownLatch readStarted = new CountDownLatch(1);
        CountDownLatch releaseRead = new CountDownLatch(1);
        AtomicReference<KnotClassLoader> activeLoader = new AtomicReference<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (KnotClassLoader firstLoader = new BlockingKnotClassLoader(
                new URL[]{firstDirectory.toUri().toURL()}, PARENT, RESOURCE_PATH, readStarted, releaseRead);
             KnotClassLoader secondLoader = new KnotClassLoader(
                     new URL[]{secondDirectory.toUri().toURL()}, PARENT)) {
            activeLoader.set(firstLoader);
            CompositeClassProvider provider = new CompositeClassProvider(activeLoader::get);

            Future<byte[]> inFlightRead = executor.submit(() -> provider.getClass(CLASS_NAME));
            assertThat(readStarted.await(5, TimeUnit.SECONDS)).isTrue();

            activeLoader.set(secondLoader);
            assertThat(provider.getClass(CLASS_NAME)).isEqualTo(secondBytes);

            releaseRead.countDown();
            assertThat(inFlightRead.get(5, TimeUnit.SECONDS)).isEqualTo(firstBytes);

            assertThat(provider.getClass(CLASS_NAME)).isEqualTo(secondBytes);
        } finally {
            releaseRead.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Should not publish bytes read before a classpath version change into the refreshed cache")
    void shouldNotPublishStaleBytesAfterClasspathVersionChange() throws Exception {
        String resourcePath = TargetGameService.class.getName().replace('.', '/') + ".class";
        byte[] parentBytes = readClassBytes(TargetGameService.class);
        byte[] localBytes = fixtureClass(TargetGameService.class.getName(), "late");
        Path localDirectory = writeClass("late-version", resourcePath, localBytes);

        CountDownLatch readStarted = new CountDownLatch(1);
        CountDownLatch releaseRead = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (KnotClassLoader loader = new BlockingKnotClassLoader(
                new URL[0], PARENT, resourcePath, readStarted, releaseRead)) {
            CompositeClassProvider provider = new CompositeClassProvider(loader);

            Future<byte[]> inFlightRead =
                    executor.submit(() -> provider.getClass(TargetGameService.class.getName()));
            assertThat(readStarted.await(5, TimeUnit.SECONDS)).isTrue();

            loader.addURL(localDirectory.toUri().toURL());
            assertThat(provider.getClass(TargetGameService.class.getName())).isEqualTo(localBytes);

            releaseRead.countDown();
            assertThat(inFlightRead.get(5, TimeUnit.SECONDS)).isEqualTo(parentBytes);

            assertThat(provider.getClass(TargetGameService.class.getName())).isEqualTo(localBytes);
        } finally {
            releaseRead.countDown();
            executor.shutdownNow();
        }
    }

    private Path writeClass(String directory, byte[] bytes) throws IOException {
        return writeClass(directory, RESOURCE_PATH, bytes);
    }

    private Path writeClass(String directory, String resourcePath, byte[] bytes) throws IOException {
        Path root = Files.createDirectories(tempDir.resolve(directory));
        Path target = root.resolve(resourcePath);
        Files.createDirectories(target.getParent());
        Files.write(target, bytes);
        return root;
    }

    private static byte[] readClassBytes(Class<?> type) throws IOException {
        String resourcePath = type.getName().replace('.', '/') + ".class";

        try (InputStream inputStream = PARENT.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Class resource not found: " + resourcePath);
            }
            return inputStream.readAllBytes();
        }
    }

    private static byte[] fixtureClass(String name, String marker) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V25, Opcodes.ACC_PUBLIC, name.replace('.', '/'), null, "java/lang/Object", null);
        writer.visitField(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                "MARKER",
                "Ljava/lang/String;",
                null,
                marker
        ).visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    /**
     * Loader that resolves a fixture resource and then pauses before the caller can read its bytes.
     */
    private static final class BlockingKnotClassLoader extends KnotClassLoader {

        private final String watchedResource;
        private final CountDownLatch readStarted;
        private final CountDownLatch releaseRead;
        private final AtomicBoolean blocked = new AtomicBoolean();

        BlockingKnotClassLoader(
                URL[] urls,
                ClassLoader parent,
                String watchedResource,
                CountDownLatch readStarted,
                CountDownLatch releaseRead
        ) {
            super(urls, parent);
            this.watchedResource = watchedResource;
            this.readStarted = readStarted;
            this.releaseRead = releaseRead;
        }

        @Override
        public URL findResource(String name) {
            URL resource = super.findResource(name);
            if (watchedResource.equals(name) && blocked.compareAndSet(false, true)) {
                readStarted.countDown();
                awaitRelease();
            }
            return resource;
        }

        private void awaitRelease() {
            try {
                if (!releaseRead.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to release the fixture read");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting to release the fixture read", e);
            }
        }
    }
}
