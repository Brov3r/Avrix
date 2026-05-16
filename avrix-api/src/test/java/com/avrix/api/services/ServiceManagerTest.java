package com.avrix.api.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ServiceManager}.
 */
@Execution(ExecutionMode.SAME_THREAD)
final class ServiceManagerTest {
    interface DummyService {
    }

    static final class DummyServiceImpl implements DummyService {
    }

    interface OtherService {
    }

    static final class OtherServiceImpl implements OtherService {
    }

    @AfterEach
    void clearRegistry() throws Exception {
        var field = ServiceManager.class.getDeclaredField("services");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<Class<?>, Object> services =
                (ConcurrentHashMap<Class<?>, Object>) field.get(null);
        services.clear();
    }

    @Test
    @DisplayName("Register and retrieve returns exact same instance")
    void registerAndGetService_ReturnsCorrectInstance() {
        DummyServiceImpl impl = new DummyServiceImpl();
        ServiceManager.register(DummyService.class, impl);

        Optional<DummyService> result = ServiceManager.getService(DummyService.class);
        assertThat(result)
                .isPresent()
                .containsSame(impl);
    }

    @Test
    @DisplayName("Get unregistered service returns empty Optional")
    void getService_NeverRegistered_ReturnsEmpty() {
        assertThat(ServiceManager.getService(DummyService.class))
                .isEmpty();
    }

    @Test
    @DisplayName("Unregister existing service removes it from registry")
    void unregister_ExistingService_GetServiceReturnsEmpty() {
        ServiceManager.register(DummyService.class, new DummyServiceImpl());
        ServiceManager.unregister(DummyService.class);

        assertThat(ServiceManager.getService(DummyService.class))
                .isEmpty();
    }

    @Test
    @DisplayName("Unregister non-existing service completes without exception")
    void unregister_NonExistingService_NoException() {
        assertThatCode(() -> ServiceManager.unregister(DummyService.class))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Subsequent register overwrites previous implementation")
    void register_OverwritesPreviousService() {
        DummyServiceImpl first = new DummyServiceImpl();
        DummyServiceImpl second = new DummyServiceImpl();

        ServiceManager.register(DummyService.class, first);
        ServiceManager.register(DummyService.class, second);

        assertThat(ServiceManager.getService(DummyService.class))
                .containsSame(second);
    }

    @Test
    @DisplayName("Register null implementation throws NullPointerException")
    void register_NullImplementation_ThrowsNullPointerException() {
        // Objects.requireNonNull throws NPE, not IllegalArgumentException
        assertThatThrownBy(() -> ServiceManager.register(DummyService.class, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("serviceImplementation cannot be null");
    }

    @Test
    @DisplayName("Register invalid implementation throws IllegalArgumentException")
    void register_InvalidImplementation_ThrowsIllegalArgumentException() throws Exception {
        Method registerMethod = ServiceManager.class.getDeclaredMethod(
                "register", Class.class, Object.class);
        registerMethod.setAccessible(true);

        OtherServiceImpl invalidImpl = new OtherServiceImpl();

        assertThatThrownBy(() -> registerMethod.invoke(null, DummyService.class, invalidImpl))
                .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .cause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not implement interface");
    }

    @Test
    @DisplayName("Concurrent register/get/unregister operations are thread-safe")
    void concurrentOperations_ThreadSafe() throws InterruptedException {
        int iterations = 10_000;
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(iterations);
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicInteger successReads = new AtomicInteger();

        for (int i = 0; i < iterations; i++) {
            executor.submit(() -> {
                try {
                    start.await(); // Synchronize start
                    ServiceManager.register(DummyService.class, new DummyServiceImpl());
                    ServiceManager.getService(DummyService.class)
                            .ifPresent(s -> successReads.incrementAndGet());
                    if (Math.random() > 0.5) {
                        ServiceManager.unregister(DummyService.class);
                    }
                } catch (Throwable e) {
                    error.compareAndSet(null, e);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS), "Concurrency test timed out");
        executor.shutdown();

        assertThat(error.get()).isNull();
        assertThat(successReads.get()).isGreaterThan(0);
    }

    @Test
    @DisplayName("getService returns Optional.empty for missing service")
    void getService_MissingService_ReturnsEmptyOptional() {
        Optional<DummyService> result = ServiceManager.getService(DummyService.class);
        assertThat(result)
                .isEmpty()
                .isInstanceOf(Optional.class);
    }

    @Test
    @DisplayName("getService Optional is not null")
    void getService_NeverReturnsNull() {
        Optional<DummyService> result = ServiceManager.getService(DummyService.class);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Cast in getService succeeds for correctly registered type")
    void getService_CastSucceeds_ForValidRegistration() {
        DummyServiceImpl impl = new DummyServiceImpl();
        ServiceManager.register(DummyService.class, impl);

        Optional<DummyService> result = ServiceManager.getService(DummyService.class);

        assertThat(result)
                .isPresent()
                .get()
                .isInstanceOf(DummyServiceImpl.class)
                .isSameAs(impl);
    }
}