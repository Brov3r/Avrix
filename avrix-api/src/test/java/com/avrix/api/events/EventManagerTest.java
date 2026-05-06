package com.avrix.api.events;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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
}