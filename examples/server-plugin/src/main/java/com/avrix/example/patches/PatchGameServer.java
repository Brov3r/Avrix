package com.avrix.example.patches;

import com.avrix.agent.ClassTransformer;
import javassist.CannotCompileException;

/**
 * Game file patch class main entry point
 */
public class PatchGameServer extends ClassTransformer {
    /**
     * Constructor for creating a {@link ClassTransformer} object.
     * It is necessary to specify the constructor without arguments.
     */
    public PatchGameServer() {
        super("zombie.network.GameServer");
    }

    /**
     * Method for performing class modification.
     * The implementing method must contain the logic for modifying the target class.
     */
    @Override
    public void modifyClass() {
        getModifierBuilder().modifyMethod("main", (ctClass, ctMethod) -> {
            try {
                ctMethod.insertBefore("{System.out.println(\"Inject from example plugin!\");}");
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        });
    }
}