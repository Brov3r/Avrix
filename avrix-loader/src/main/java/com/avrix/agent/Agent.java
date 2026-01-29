package com.avrix.agent;

import java.lang.instrument.Instrumentation;

public final class Agent {
    private static volatile Instrumentation instrumentation;

    public static void agentmain(String args, Instrumentation inst) {
        instrumentation = inst;
    }
}
