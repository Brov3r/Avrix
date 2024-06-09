package com.avrix.patches;

import com.avrix.agent.ClassTransformer;
import com.avrix.api.PlayerUtils;
import com.avrix.events.EventManager;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * KickUserCommand patcher
 */
public class PatchKickUserCommand extends ClassTransformer {
    /**
     * Constructor for creating a {@link ClassTransformer} object.
     */
    public PatchKickUserCommand() {
        super("zombie.commands.serverCommands.KickUserCommand");
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
                        if (m.getClassName().equals("zombie.core.raknet.UdpConnection") && m.getMethodName().equals("forceDisconnect")) {
                            String code = "{ "
                                    + "zombie.characters.IsoPlayer player = " + PlayerUtils.class.getName() + ".getPlayerByUdpConnection($0);"
                                    + "java.lang.String adminName = this.getExecutorUsername().isEmpty() ? \"Console\" : this.getExecutorUsername();"
                                    + "if (player != null) {"
                                    + EventManager.class.getName() + ".invokeEvent(\"onPlayerKick\", new Object[]{player, adminName, this.reason});"
                                    + "}"
                                    + "$proceed($$);"
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