package com.avrix.patches;

import com.avrix.agent.ClassTransformer;
import com.avrix.commands.CommandsManager;
import com.avrix.events.EventManager;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * Game Server patcher
 */
public class PatchGameServer extends ClassTransformer {
    /**
     * Constructor for creating a {@link ClassTransformer} object.
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
                ctMethod.instrument(new ExprEditor() {
                    public void edit(MethodCall m) throws CannotCompileException {
                        if (m.getClassName().contains("GlobalObject") && m.getMethodName().equals("refreshAnimSets")) {
                            m.replace("{ $proceed($$);" +
                                    EventManager.class.getName() + ".invokeEvent(\"onServerInitialize\", new Object[0]); }");
                        }
                    }
                });
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        }).modifyMethod("addIncoming", (ctClass, ctMethod) -> {
            try {
                ctMethod.insertBefore("{ " +
                        "java.nio.ByteBuffer bb = $2.duplicate();" +
                        EventManager.class.getName() + ".invokeEvent(\"onAddIncoming\", new Object[]{new java.lang.Short($1), bb, $3}); " +
                        "}");
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        }).modifyMethod("handleServerCommand", (ctClass, ctMethod) -> {
            try {
                ctMethod.insertBefore("{ " +
                        "if($1 != null) {" +
                        EventManager.class.getName() + ".invokeEvent(\"onSendConsoleCommand\", new Object[]{$1}); " +
                        "java.lang.String customResult = " + CommandsManager.class.getName() + ".handleCustomCommand($2, $1);" +
                        "if (customResult != null) return customResult;" +
                        "}" +
                        "}");
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        }).modifyMethod("receivePlayerConnect", "java.nio.ByteBuffer, zombie.core.raknet.UdpConnection, java.lang.String", (ctClass, ctMethod) -> {
            try {
                ctMethod.insertBefore("{ " +
                        "java.nio.ByteBuffer bb = $1.duplicate();" +
                        EventManager.class.getName() + ".invokeEvent(\"onPlayerConnect\", new Object[]{bb, $2, $3}); " +
                        "}");
                ctMethod.insertAfter("{ " +
                        "java.nio.ByteBuffer bb = $1.rewind().duplicate();" +
                        EventManager.class.getName() + ".invokeEvent(\"onPlayerFullyConnected\", new Object[]{bb, $2, $3}); " +
                        "}");
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        }).modifyMethod("disconnectPlayer", "zombie.characters.IsoPlayer, zombie.core.raknet.UdpConnection", (ctClass, ctMethod) -> {
            try {
                ctMethod.insertBefore("{ " +
                        EventManager.class.getName() + ".invokeEvent(\"onPlayerDisconnect\", $args); " +
                        "}");
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        }).modifyMethod("receiveReceiveCommand", (ctClass, ctMethod) -> {
            try {
                String code = "{ java.lang.String readString = zombie.GameWindow.ReadString($1);" +
                        EventManager.class.getName() + ".invokeEvent(\"onSendChatCommand\", new Object[]{$2, readString});" +
                        "java.lang.String handleCommand = " + CommandsManager.class.getName() + ".handleCustomCommand($2, readString);" +
                        "if (handleCommand == null) {" +
                        "    handleCommand = handleClientCommand(readString.substring(1), $2);" +
                        "}" +
                        "if (handleCommand == null) {" +
                        "    handleCommand = handleServerCommand(readString.substring(1), $2);" +
                        "}" +
                        "if (handleCommand == null) {" +
                        "    handleCommand = \"Unknown command \" + readString;" +
                        "}" +
                        "if (handleCommand.isEmpty()) return;" +
                        "zombie.network.chat.ChatServer.getInstance().sendMessageToServerChat($2, handleCommand);" +
                        "}";
                ctMethod.setBody(code);
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        });
    }
}