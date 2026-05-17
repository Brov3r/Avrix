package com.avrix.api.mixins;

import com.avrix.api.events.EventManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;
import zombie.Lua.LuaEventManager;

/**
 * Mixin for {@link LuaEventManager} that bridges Lua-triggered events
 * to the native {@link EventManager} dispatch system.
 *
 * <p>Injects a call to {@code EventManager.invoke(eventName, args...)} at the
 * beginning ({@code HEAD}) of each {@code triggerEvent} overload (0–8 parameters),
 * enabling seamless integration between Lua scripts and Java event listeners.
 */
@CTransformer(value = LuaEventManager.class)
public class LuaEventManagerMixin {

    /**
     * Injects {@code EventManager.invoke(event)} into {@code triggerEvent(String)}.
     * Descriptor: (Ljava/lang/String;)V
     */
    @CInject(
            method = "triggerEvent(Ljava/lang/String;)V",
            target = @CTarget("HEAD")
    )
    private static void inject0(String event) {
        EventManager.invoke(event);
    }

    /**
     * Injects {@code EventManager.invoke(event, p1)} into {@code triggerEvent(String, Object)}.
     * Descriptor: (Ljava/lang/String;Ljava/lang/Object;)V
     */
    @CInject(
            method = "triggerEvent(Ljava/lang/String;Ljava/lang/Object;)V",
            target = @CTarget("HEAD")
    )
    private static void inject1(String event, Object p1) {
        EventManager.invoke(event, p1);
    }

    /**
     * Injects {@code EventManager.invoke(event, p1, p2)} into {@code triggerEvent(String, Object, Object)}.
     * Descriptor: (Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V
     */
    @CInject(
            method = "triggerEvent(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V",
            target = @CTarget("HEAD")
    )
    private static void inject2(String event, Object p1, Object p2) {
        EventManager.invoke(event, p1, p2);
    }

    /**
     * Injects {@code EventManager.invoke(event, p1..p3)}.
     * Descriptor: (Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
     */
    @CInject(
            method = "triggerEvent(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V",
            target = @CTarget("HEAD")
    )
    private static void inject3(String event, Object p1, Object p2, Object p3) {
        EventManager.invoke(event, p1, p2, p3);
    }

    /**
     * Injects {@code EventManager.invoke(event, p1..p4)}.
     * Descriptor: (Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
     */
    @CInject(
            method = "triggerEvent(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V",
            target = @CTarget("HEAD")
    )
    private static void inject4(String event, Object p1, Object p2, Object p3, Object p4) {
        EventManager.invoke(event, p1, p2, p3, p4);
    }

    /**
     * Injects {@code EventManager.invoke(event, p1..p5)}.
     * Descriptor: (Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
     */
    @CInject(
            method = "triggerEvent(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V",
            target = @CTarget("HEAD")
    )
    private static void inject5(String event, Object p1, Object p2, Object p3, Object p4, Object p5) {
        EventManager.invoke(event, p1, p2, p3, p4, p5);
    }

    /**
     * Injects {@code EventManager.invoke(event, p1..p6)}.
     * Descriptor: (Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
     */
    @CInject(
            method = "triggerEvent(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V",
            target = @CTarget("HEAD")
    )
    private static void inject6(String event, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
        EventManager.invoke(event, p1, p2, p3, p4, p5, p6);
    }

    /**
     * Injects {@code EventManager.invoke(event, p1..p7)}.
     * Descriptor: (Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
     */
    @CInject(
            method = "triggerEvent(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V",
            target = @CTarget("HEAD")
    )
    private static void inject7(String event, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
        EventManager.invoke(event, p1, p2, p3, p4, p5, p6, p7);
    }

    /**
     * Injects {@code EventManager.invoke(event, p1..p8)}.
     * Descriptor: (Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V
     */
    @CInject(
            method = "triggerEvent(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V",
            target = @CTarget("HEAD")
    )
    private static void inject8(String event, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {
        EventManager.invoke(event, p1, p2, p3, p4, p5, p6, p7, p8);
    }
}