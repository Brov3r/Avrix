package com.avrix.patches;

import com.avrix.agent.ClassTransformer;

/**
 * Test
 */
public class PatchTest1 extends ClassTransformer {
    /**
     * Constructor for creating a {@link ClassTransformer} object.
     */
    public PatchTest1() {
        super("zombie.network.GameServer");
    }

    /**
     * Test
     */
    @Override
    public void modifyClass() {
        getModifierBuilder().modifyMethod("main", (ctClass, ctMethod) -> {
            try {
                ctMethod.insertBefore("{System.out.println(\"Inject 1\");}");
            } catch (Exception e) {

            }
        });
    }
}
