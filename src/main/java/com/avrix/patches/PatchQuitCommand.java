package com.avrix.patches;

import com.avrix.agent.ClassTransformer;
import com.avrix.events.EventManager;
import javassist.CannotCompileException;

/**
 * QuitCommand patcher
 */
public class PatchQuitCommand extends ClassTransformer {
    /**
     * Constructor for creating a {@link ClassTransformer} object.
     */
    public PatchQuitCommand() {
        super("zombie.commands.serverCommands.QuitCommand");
    }

    /**
     * Method for performing class modification.
     * The implementing method must contain the logic for modifying the target class.
     */
    @Override
    public void modifyClass() {
        getModifierBuilder().modifyMethod("Command", (ctClass, ctMethod) -> {
            try {
                ctMethod.insertBefore(EventManager.class.getName() + ".invokeEvent(\"onServerShutdown\", new Object[0]);");
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        });
    }
}