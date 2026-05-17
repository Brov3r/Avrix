package com.avrix.api.events;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("EventManager")
class EventManagerTest {

    @AfterEach
    void cleanup() {
        EventManager.clearAllListeners();
    }

    private abstract static class OnTestEvent extends Event {
        @Override
        public String getEventName() {
            return "OnTestEvent";
        }

        public abstract void handle(String test);
    }

    private static class TestListener extends OnTestEvent {
        private String captured;
        private boolean called;

        @Override
        public void handle(String test) {
            this.called = true;
            this.captured = test;
        }

        boolean isCalled() {
            return called;
        }

        String getCaptured() {
            return captured;
        }
    }

    private abstract static class OnTestEvent2 extends Event {
        @Override
        public String getEventName() {
            return "OnTestEvent2";
        }

        public abstract void handle(String test, int testInt);
    }

    private static class TestListener2 extends OnTestEvent2 {
        private String strVal;
        private int intVal;
        private boolean called;

        @Override
        public void handle(String test, int testInt) {
            this.called = true;
            this.strVal = test;
            this.intVal = testInt;
        }

        boolean isCalled() {
            return called;
        }

        String getStrVal() {
            return strVal;
        }

        int getIntVal() {
            return intVal;
        }
    }

    private abstract static class MultiSigEvent extends Event {
        @Override
        public String getEventName() {
            return "multi.sig";
        }

        public abstract void handle(String msg);

        public abstract void handle(String msg, int code);
    }

    private static class DualHandleListener extends MultiSigEvent {
        volatile boolean oneArgCalled, twoArgsCalled;
        volatile String capturedMsg;
        volatile int capturedCode;

        @Override
        public void handle(String msg) {
            this.oneArgCalled = true;
            this.capturedMsg = msg;
        }

        @Override
        public void handle(String msg, int code) {
            this.twoArgsCalled = true;
            this.capturedMsg = msg;
            this.capturedCode = code;
        }
    }

    private abstract static class PriorityEvent extends Event {
        @Override
        public String getEventName() {
            return "priority.event";
        }

        public abstract void handle(String msg);
    }

    private static class PriorityListener extends PriorityEvent {
        static final List<String> ORDER = new CopyOnWriteArrayList<>();
        final String id;

        PriorityListener(String id) {
            this.id = id;
        }

        @Override
        public void handle(String msg) {
            ORDER.add(id);
        }

        static void reset() {
            ORDER.clear();
        }
    }

    private abstract static class ErrorEvent extends Event {
        @Override
        public String getEventName() {
            return "error.event";
        }

        public abstract void handle(String msg);
    }

    private static class ThrowingListener extends ErrorEvent {
        @Override
        public void handle(String msg) {
            throw new RuntimeException("Boom!");
        }
    }

    private static class GoodListener extends ErrorEvent {
        boolean called = false;

        @Override
        public void handle(String msg) {
            called = true;
        }
    }

    interface Payload {
        String kind();
    }

    private static class TextPayload implements Payload {
        final String content;

        TextPayload(String content) {
            this.content = content;
        }

        @Override
        public String kind() {
            return "text";
        }
    }

    private static class NumberPayload implements Payload {
        final int value;

        NumberPayload(int value) {
            this.value = value;
        }

        @Override
        public String kind() {
            return "number";
        }
    }

    private abstract static class PayloadEvent extends Event {
        @Override
        public String getEventName() {
            return "payload.event";
        }

        public abstract void handle(Payload p);
    }

    private static class PolyListener extends PayloadEvent {
        Payload captured;
        boolean called;

        @Override
        public void handle(Payload p) {
            called = true;
            captured = p;
        }
    }

    private abstract static class SpecificEvent extends Event {
        @Override
        public String getEventName() {
            return "specific.event";
        }

        public abstract void handle(TextPayload tp);
    }

    private static class SpecificListener extends SpecificEvent {
        TextPayload captured;
        boolean called;

        @Override
        public void handle(TextPayload tp) {
            called = true;
            captured = tp;
        }
    }

    private abstract static class ObjectEvent extends Event {
        @Override
        public String getEventName() {
            return "object.event";
        }

        public abstract void handle(Object obj);
    }

    private static class ObjectListener extends ObjectEvent {
        Object captured;
        boolean called;

        @Override
        public void handle(Object obj) {
            called = true;
            captured = obj;
        }
    }

    @Test
    @DisplayName("1. Basic invoke: handler receives correct argument")
    void testBasicInvoke() {
        TestListener listener = new TestListener();
        EventManager.addListener(listener);
        EventManager.invoke("OnTestEvent", "Hello World");

        assertThat(listener.isCalled()).isTrue();
        assertThat(listener.getCaptured()).isEqualTo("Hello World");
    }

    @Test
    @DisplayName("2. Multi-argument handler works correctly")
    void testMultiArgInvoke() {
        TestListener2 listener = new TestListener2();
        EventManager.addListener(listener);
        EventManager.invoke("OnTestEvent2", "test", 42);

        assertThat(listener.isCalled()).isTrue();
        assertThat(listener.getStrVal()).isEqualTo("test");
        assertThat(listener.getIntVal()).isEqualTo(42);
    }

    @Test
    @DisplayName("3. Overloaded handle: dynamic dispatch by args count/types")
    void testOverloadedHandleDynamicDispatch() {
        DualHandleListener l = new DualHandleListener();
        EventManager.addListener(l);

        EventManager.invoke("multi.sig", "hello");
        assertThat(l.oneArgCalled).isTrue();
        assertThat(l.twoArgsCalled).isFalse();
        assertThat(l.capturedMsg).isEqualTo("hello");

        l.oneArgCalled = false;
        l.capturedMsg = null;
        EventManager.invoke("multi.sig", "world", 42);
        assertThat(l.twoArgsCalled).isTrue();
        assertThat(l.capturedMsg).isEqualTo("world");
        assertThat(l.capturedCode).isEqualTo(42);
    }

    @Test
    @DisplayName("4. Priority: higher values execute first (descending)")
    void testPriorityOrder() {
        PriorityListener.reset();
        EventManager.addListener(new PriorityListener("low"), 10);
        EventManager.addListener(new PriorityListener("high"), 100);
        EventManager.addListener(new PriorityListener("mid"), 50);

        EventManager.invoke("priority.event", "x");

        assertThat(PriorityListener.ORDER).containsExactly("high", "mid", "low");
    }

    @Test
    @DisplayName("5. Exception isolation: handler failure doesn't stop others")
    void testHandlerExceptionIsolation() {
        GoodListener good = new GoodListener();
        ThrowingListener bad = new ThrowingListener();

        EventManager.addListener(good, 10);
        EventManager.addListener(bad, 5);

        assertThatCode(() -> EventManager.invoke("error.event", "test"))
                .doesNotThrowAnyException();
        assertThat(good.called).isTrue();
    }

    @Test
    @DisplayName("6. Null argument accepted for reference types")
    void testNullForReferenceType() {
        TestListener listener = new TestListener();
        EventManager.addListener(listener);

        EventManager.invoke("OnTestEvent", (String) null);

        assertThat(listener.isCalled()).isTrue();
        assertThat(listener.getCaptured()).isNull();
    }

    @Test
    @DisplayName("7. Autoboxing: Integer -> int parameter works")
    void testPrimitiveAutoboxing() {
        abstract class PrimEvent extends Event {
            @Override
            public String getEventName() {
                return "prim";
            }

            public abstract void handle(int x);
        }
        class PrimListener extends PrimEvent {
            int val;

            @Override
            public void handle(int x) {
                val = x;
            }
        }

        PrimListener l = new PrimListener();
        EventManager.addListener(l);
        EventManager.invoke("prim", Integer.valueOf(99));

        assertThat(l.val).isEqualTo(99);
    }

    @Test
    @DisplayName("8. removeListener: returns true & actually removes")
    void testRemoveListener() {
        TestListener l = new TestListener();
        EventManager.addListener(l);
        assertThat(EventManager.removeListener(l)).isTrue();
        assertThat(EventManager.getListenersForEvent("OnTestEvent")).isEmpty();
    }

    @Test
    @DisplayName("9. clearAllListeners: removes everything")
    void testClearAllListeners() {
        EventManager.addListener(new TestListener());
        EventManager.addListener(new TestListener2());
        EventManager.clearAllListeners();

        assertThat(EventManager.getAllListeners()).isEmpty();
        assertThat(EventManager.getListenersForEvent("OnTestEvent")).isEmpty();
    }

    @Test
    @DisplayName("10. API safety: getListeners returns unmodifiable snapshot")
    void testUnmodifiableSnapshot() {
        EventManager.addListener(new TestListener());
        var list = EventManager.getListenersForEvent("OnTestEvent");

        assertThatCode(() -> list.add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("11. Argument mismatch: listener skipped, no crash")
    void testArgumentMismatchSkipsListener() {
        TestListener l = new TestListener();
        EventManager.addListener(l);
        EventManager.invoke("OnTestEvent", 123);

        assertThat(l.isCalled()).isFalse();
    }

    @Test
    @DisplayName("12. Custom class: interface parameter accepts any implementation")
    void testCustomClassInterfacePolymorphism() {
        PolyListener listener = new PolyListener();
        EventManager.addListener(listener);

        EventManager.invoke("payload.event", new TextPayload("hello"));

        assertThat(listener.called).isTrue();
        assertThat(listener.captured).isInstanceOf(TextPayload.class);
        assertThat(((TextPayload) listener.captured).content).isEqualTo("hello");
    }

    @Test
    @DisplayName("13. Custom class: exact type match")
    void testCustomClassExactMatch() {
        SpecificListener listener = new SpecificListener();
        EventManager.addListener(listener);

        TextPayload payload = new TextPayload("exact");
        EventManager.invoke("specific.event", payload);

        assertThat(listener.called).isTrue();
        assertThat(listener.captured).isSameAs(payload);
    }

    @Test
    @DisplayName("14. Custom class: mismatched implementation is skipped")
    void testCustomClassMismatchSkipped() {
        SpecificListener listener = new SpecificListener();
        EventManager.addListener(listener);

        EventManager.invoke("specific.event", new NumberPayload(42));

        assertThat(listener.called).isFalse();
    }

    @Test
    @DisplayName("15. Custom class: Object parameter accepts any type")
    void testObjectParamAcceptsAnything() {
        ObjectListener listener = new ObjectListener();
        EventManager.addListener(listener);

        EventManager.invoke("object.event", "string");
        assertThat(listener.called).isTrue();
        assertThat(listener.captured).isEqualTo("string");

        listener.called = false;
        EventManager.invoke("object.event", new NumberPayload(99));
        assertThat(listener.called).isTrue();
        assertThat(listener.captured).isInstanceOf(NumberPayload.class);
    }

    @Test
    @DisplayName("16. Custom class: null passed to reference type parameter")
    void testCustomClassNullParam() {
        PolyListener listener = new PolyListener();
        EventManager.addListener(listener);

        EventManager.invoke("payload.event", (Payload) null);

        assertThat(listener.called).isTrue();
        assertThat(listener.captured).isNull();
    }

    @Nested
    @DisplayName("CustomEvent.normalizeEventName")
    class CustomEventNormalizeEventNameTest {

        private static final Method NORMALIZE_METHOD;

        static {
            try {
                NORMALIZE_METHOD = EventManager.CustomEvent.class.getDeclaredMethod("normalizeEventName", String.class);
                NORMALIZE_METHOD.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        private static String invokeNormalize(String name) {
            try {
                return (String) NORMALIZE_METHOD.invoke(null, name);
            } catch (java.lang.reflect.InvocationTargetException e) {
                // Propagate original NPE for proper assertion
                if (e.getCause() instanceof NullPointerException) {
                    throw (NullPointerException) e.getCause();
                }
                throw new RuntimeException("Failed to invoke normalizeEventName", e);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke normalizeEventName", e);
            }
        }

        @Nested
        @DisplayName("PascalCase conversion")
        class PascalCaseConversion {

            @ParameterizedTest(name = "\"{0}\" → \"{1}\"")
            @CsvSource({
                    "my-event_123, MyEvent123",
                    "test event, TestEvent",
                    "snake_case_name, SnakeCaseName",
                    "AlreadyPascalCase, AlreadyPascalCase",
                    "123_test, 123Test",
                    "___multiple___underscores___, MultipleUnderscores",
                    "simple, Simple",
                    "a, A",
                    "test_123_event, Test123Event",
                    "UPPERCASE, Uppercase",
                    "MiXeD_CaSe, MixedCase",
                    "_leading_underscore, LeadingUnderscore",
                    "trailing_underscore_, TrailingUnderscore",
                    "a_b_c, ABC",
                    "event-name_with-mixed!@#, EventNameWithMixed"
            })
            void transformsToPascalCase(String input, String expected) {
                assertThat(invokeNormalize(input)).isEqualTo(expected);
            }
        }

        @Nested
        @DisplayName("Edge cases")
        class EdgeCases {
            @ParameterizedTest
            @NullAndEmptySource
            @ValueSource(strings = {" ", "  ", "\t", "\n", "_", "__", "___", "!@#", "___!!!___"})
            @DisplayName("blank or non-Latin input returns empty string")
            void blankOrInvalidInputReturnsEmpty(String input) {
                assertThat(invokeNormalize(input)).isEmpty();
            }

            @Test
            @DisplayName("whitespace trimming is applied before normalization")
            void trimsWhitespace() {
                assertThat(invokeNormalize("  test event  ")).isEqualTo("TestEvent");
                assertThat(invokeNormalize("\t\ntest_event\n\t")).isEqualTo("TestEvent");
            }
        }

        @Nested
        @DisplayName("Character handling")
        class CharacterHandling {

            @Test
            @DisplayName("non-Latin characters are removed and act as separators")
            void removesNonLatinCharacters() {
                assertThat(invokeNormalize("test_событие_event")).isEqualTo("TestEvent");
                assertThat(invokeNormalize("Hello_世界_World")).isEqualTo("HelloWorld");
                // Non-Latin 'é' is removed (not in [A-Za-z0-9])
                assertThat(invokeNormalize("Café")).isEqualTo("Caf");
            }

            @Test
            @DisplayName("digits are preserved and not capitalized")
            void preservesDigits() {
                assertThat(invokeNormalize("event_123")).isEqualTo("Event123");
                assertThat(invokeNormalize("123_event_456")).isEqualTo("123Event456");
                assertThat(invokeNormalize("a1_b2_c3")).isEqualTo("A1B2C3");
            }

            @Test
            @DisplayName("underscores and other separators act as word boundaries")
            void separatorsAsWordBoundaries() {
                assertThat(invokeNormalize("a_b_c")).isEqualTo("ABC");
                assertThat(invokeNormalize("_test_")).isEqualTo("Test");
                assertThat(invokeNormalize("test___event")).isEqualTo("TestEvent");
                assertThat(invokeNormalize("event-name_with-mixed!@#")).isEqualTo("EventNameWithMixed");
            }
        }

        @Nested
        @DisplayName("Constructor integration")
        class ConstructorIntegration {

            @ParameterizedTest(name = "\"{0}\" → normalized name \"{1}\"")
            @CsvSource({
                    "my-event, MyEvent",
                    "snake_case, SnakeCase",
                    "AlreadyPascalCase, AlreadyPascalCase",
                    "123_test, 123Test"
            })
            void constructorUsesNormalizedName(String rawName, String expectedNormalizedName) {
                EventManager.CustomEvent event = new EventManager.CustomEvent(rawName, List.of(), List.of());
                assertThat(event.name()).isEqualTo(expectedNormalizedName);
            }

            @ParameterizedTest
            @ValueSource(strings = {"", " ", "\t", "\n"})
            @DisplayName("constructor rejects blank names with 'cannot be blank' message")
            void rejectsBlankNames(String invalidName) {
                IllegalArgumentException ex = assertThrows(
                        IllegalArgumentException.class,
                        () -> new EventManager.CustomEvent(invalidName, List.of(), List.of())
                );
                assertThat(ex.getMessage()).isEqualTo("name cannot be blank");
            }

            @ParameterizedTest
            @ValueSource(strings = {"___", "!@#", "___!!!___", "123", "___123___"})
            @DisplayName("constructor rejects names without Latin letters after normalization")
            void rejectsNoLatinLettersAfterNormalization(String invalidName) {
                IllegalArgumentException ex = assertThrows(
                        IllegalArgumentException.class,
                        () -> new EventManager.CustomEvent(invalidName, List.of(), List.of())
                );
                assertThat(ex.getMessage())
                        .contains("must contain at least one Latin letter after normalization");
            }

            @Test
            @DisplayName("normalized name is stored immutably in record")
            void normalizedNameIsImmutable() {
                EventManager.CustomEvent event = new EventManager.CustomEvent("test_event", List.of(), List.of());
                assertThat(event.name()).isEqualTo("TestEvent")
                        .isNotEqualTo("test_event")
                        .isNotEqualTo("TEST_EVENT");
            }
        }

        @Nested
        @DisplayName("Idempotency and consistency")
        class Idempotency {

            @Test
            @DisplayName("normalization is idempotent for already-normalized names")
            void idempotentForPascalCase() {
                String input = "MyEventName123";
                String first = invokeNormalize(input);
                String second = invokeNormalize(first);
                assertThat(second).isEqualTo(first).isEqualTo(input);
            }

            @Test
            @DisplayName("repeated calls yield consistent result")
            void consistentAcrossMultipleCalls() {
                String input = "test_event_name";
                String expected = "TestEventName";
                for (int i = 0; i < 50; i++) {
                    assertThat(invokeNormalize(input)).isEqualTo(expected);
                }
            }

            @Test
            @DisplayName("proper PascalCase input is preserved unchanged")
            void preservesProperPascalCase() {
                assertThat(invokeNormalize("AlreadyPascalCase")).isEqualTo("AlreadyPascalCase");
                assertThat(invokeNormalize("Simple")).isEqualTo("Simple");
                assertThat(invokeNormalize("A")).isEqualTo("A");
            }
        }

    }
}