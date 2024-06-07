package com.avrix.patches;

import com.avrix.agent.ClassTransformer;
import com.avrix.plugin.EventManager;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * ChatServer patcher
 */
public class PatchChatServer extends ClassTransformer {
    /**
     * Constructor for creating a {@link ClassTransformer} object.
     */
    public PatchChatServer() {
        super("zombie.network.chat.ChatServer");
    }

    /**
     * Method for performing class modification.
     * The implementing method must contain the logic for modifying the target class.
     */
    @Override
    public void modifyClass() {
        getModifierBuilder().modifyMethod("processMessageFromPlayerPacket", (ctClass, ctMethod) -> {
            try {
                ctMethod.instrument(new ExprEditor() {
                    public void edit(MethodCall m) throws CannotCompileException {
                        if (m.getMethodName().equals("unpackMessage")) {
                            m.replace("{ $_ = $proceed($$); " +
                                    "zombie.chat.ChatBase base = (zombie.chat.ChatBase)this.chats.get(new Integer($1.rewind().getInt()));"
                                    + EventManager.class.getName() + ".invokeEvent(\"onChatMessageProcessed\", new Object[]{base, $_}); }");
                        }
                    }
                });
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        });
    }
}