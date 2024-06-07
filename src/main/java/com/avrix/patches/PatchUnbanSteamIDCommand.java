package com.avrix.patches;

import com.avrix.agent.ClassTransformer;
import com.avrix.plugin.EventManager;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * UnbanSteamIDCommand patcher
 */
public class PatchUnbanSteamIDCommand extends ClassTransformer {
    /**
     * Constructor for creating a {@link ClassTransformer} object.
     */
    public PatchUnbanSteamIDCommand() {
        super("zombie.commands.serverCommands.UnbanSteamIDCommand");
    }

    /**
     * Method for performing class modification.
     * The implementing method must contain the logic for modifying the target class.
     */
    @Override
    public void modifyClass() {
        getModifierBuilder().modifyMethod("Command", (ctClass, ctMethod) -> {
            try {
                ctMethod.instrument(new ExprEditor() {
                    public void edit(MethodCall m) throws CannotCompileException {
                        if (m.getClassName().contains("ServerWorldDatabase") && m.getMethodName().contains("banSteamID")) {
                            String code = "{ "
                                    + "$_ = $proceed($$);"
                                    + "java.lang.String adminName = this.getExecutorUsername().isEmpty() ? \"Console\" : this.getExecutorUsername();"
                                    + EventManager.class.getName() + ".invokeEvent(\"onPlayerUnban\", new Object[]{$1, adminName});"
                                    + "}";
                            m.replace(code);
                        }
                    }
                });
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        });
    }
}