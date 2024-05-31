package com.avrix.patches;

import com.avrix.agent.ClassTransformer;

/**
 * Test
 */
public class PatchTest2 extends ClassTransformer {
    /**
     * Constructor for creating a {@link ClassTransformer} object.
     */
    public PatchTest2() {
        super("zombie.network.GameServer");
    }

    /**
     * Test
     */
    @Override
    public void modifyClass() {
        getModifierBuilder().modifyMethod("main", (ctClass, ctMethod) -> {
            try {
                ctMethod.insertBefore("{System.out.println(\"Inject 2\");}");
            } catch (Exception e) {

            }
        });
    }
}
