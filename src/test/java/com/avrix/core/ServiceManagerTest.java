package com.avrix.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test suite for {@link ServiceManager} registry operations.
 */
@DisplayName("ServiceManager Unit Tests")
class ServiceManagerTest {

    interface DummyService {
        String ping();
    }

    static class DummyServiceImpl implements DummyService {
        @Override
        public String ping() {
            return "pong";
        }
    }

    interface AnotherService {
    }

    static class AnotherServiceImpl implements AnotherService {
    }

    static class UnrelatedClass {
    }

    @AfterEach
    void tearDown() {
        ServiceManager.clear();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should prevent instantiation of utility class via reflection")
        void shouldPreventInstantiation() throws NoSuchMethodException {
            var constructor = ServiceManager.class.getDeclaredConstructor();
            constructor.setAccessible(true);

            assertThatThrownBy(constructor::newInstance)
                    .isInstanceOf(InvocationTargetException.class)
                    .hasCauseInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should successfully register and retrieve a service instance")
        void shouldRegisterAndGetService() {
            DummyService instance = new DummyServiceImpl();

            ServiceManager.register(DummyService.class, instance);
            DummyService retrieved = ServiceManager.get(DummyService.class);

            assertThat(retrieved)
                    .isNotNull()
                    .isSameAs(instance);

            assertThat(retrieved.ping()).isEqualTo("pong");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when registering duplicate service types")
        void shouldThrowWhenServiceAlreadyRegistered() {
            DummyService instance1 = new DummyServiceImpl();
            DummyService instance2 = new DummyServiceImpl();

            ServiceManager.register(DummyService.class, instance1);

            assertThatThrownBy(() -> ServiceManager.register(DummyService.class, instance2))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Service already registered for type:")
                    .hasMessageContaining(DummyService.class.getName());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if implementation does not match contract type")
        @SuppressWarnings({"rawtypes", "unchecked"})
        void shouldThrowIfIncompatibleImplementation() {
            Class type = DummyService.class;
            Object badImplementation = new UnrelatedClass();

            assertThatThrownBy(() -> ServiceManager.register(type, badImplementation))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Implementation [")
                    .hasMessageContaining("] is not compatible with service type [");
        }

        @Test
        @DisplayName("Should throw NullPointerException if registration arguments are null")
        void shouldThrowOnNullRegistrationArgs() {
            assertThatThrownBy(() -> ServiceManager.register(null, new DummyServiceImpl()))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("serviceType cannot be null");

            assertThatThrownBy(() -> ServiceManager.register(DummyService.class, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("implementation cannot be null");
        }
    }

    @Nested
    @DisplayName("Retrieval & Query Tests")
    class RetrievalTests {

        @Test
        @DisplayName("Should throw IllegalStateException when querying unregistered service via get()")
        void shouldThrowIfServiceNotFound() {
            assertThatThrownBy(() -> ServiceManager.get(AnotherService.class))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Service not registered:")
                    .hasMessageContaining(AnotherService.class.getName());
        }

        @Test
        @DisplayName("Should return Optional.empty() when querying unregistered service via find()")
        void shouldReturnEmptyOptionalWhenServiceNotFound() {
            assertThat(ServiceManager.find(AnotherService.class)).isEmpty();
        }

        @Test
        @DisplayName("Should return Optional containing service instance when service is registered")
        void shouldReturnPopulatedOptionalWhenServiceFound() {
            DummyService instance = new DummyServiceImpl();
            ServiceManager.register(DummyService.class, instance);

            assertThat(ServiceManager.find(DummyService.class))
                    .isPresent()
                    .containsSame(instance);
        }

        @Test
        @DisplayName("Should accurately report service presence via contains()")
        void shouldReportServicePresence() {
            assertThat(ServiceManager.contains(DummyService.class)).isFalse();

            ServiceManager.register(DummyService.class, new DummyServiceImpl());

            assertThat(ServiceManager.contains(DummyService.class)).isTrue();
        }

        @Test
        @DisplayName("Should return all registered types as unmodifiable set")
        void shouldReturnRegisteredTypes() {
            ServiceManager.register(DummyService.class, new DummyServiceImpl());
            ServiceManager.register(AnotherService.class, new AnotherServiceImpl());

            var types = ServiceManager.getRegisteredTypes();

            assertThat(types).containsExactlyInAnyOrder(DummyService.class, AnotherService.class);
            assertThatThrownBy(() -> types.add(UnrelatedClass.class))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Should throw NullPointerException when passing null to query methods")
        void shouldThrowOnNullTypeQueries() {
            assertThatThrownBy(() -> ServiceManager.get(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("serviceType cannot be null");

            assertThatThrownBy(() -> ServiceManager.find(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("serviceType cannot be null");

            assertThatThrownBy(() -> ServiceManager.contains(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("serviceType cannot be null");
        }
    }

    @Nested
    @DisplayName("Unregistration & Clear Tests")
    class UnregistrationTests {

        @Test
        @DisplayName("Should successfully unregister an existing service")
        void shouldUnregisterService() {
            ServiceManager.register(DummyService.class, new DummyServiceImpl());
            assertThat(ServiceManager.contains(DummyService.class)).isTrue();

            boolean unregistered = ServiceManager.unregister(DummyService.class);

            assertThat(unregistered).isTrue();
            assertThat(ServiceManager.contains(DummyService.class)).isFalse();
            assertThatThrownBy(() -> ServiceManager.get(DummyService.class))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Should return false when unregistering a non-existent service")
        void shouldReturnFalseOnUnregisteringMissingService() {
            boolean unregistered = ServiceManager.unregister(AnotherService.class);
            assertThat(unregistered).isFalse();
        }

        @Test
        @DisplayName("Should throw NullPointerException if unregister argument is null")
        void shouldThrowOnNullUnregisterType() {
            assertThatThrownBy(() -> ServiceManager.unregister(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("serviceType cannot be null");
        }

        @Test
        @DisplayName("Should clear all registered services via clear()")
        void shouldClearRegistry() {
            ServiceManager.register(DummyService.class, new DummyServiceImpl());
            ServiceManager.register(AnotherService.class, new AnotherServiceImpl());

            assertThat(ServiceManager.getRegisteredTypes()).hasSize(2);

            ServiceManager.clear();

            assertThat(ServiceManager.getRegisteredTypes()).isEmpty();
            assertThat(ServiceManager.contains(DummyService.class)).isFalse();
            assertThat(ServiceManager.contains(AnotherService.class)).isFalse();
        }
    }
}