package com.avrix.patches;

import com.avrix.agent.ClassTransformer;
import com.avrix.ui.InputWidgetHandler;
import com.avrix.ui.WidgetManager;
import javassist.CannotCompileException;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.core.Core;
import zombie.ui.UIManager;

/**
 * GameKeyboard patcher
 */
public class PatchGameKeyboard extends ClassTransformer {
    /**
     * Constructor for creating a {@link ClassTransformer} object.
     */
    public PatchGameKeyboard() {
        super("zombie.input.GameKeyboard");
    }

    /**
     * Method for performing class modification.
     * The implementing method must contain the logic for modifying the target class.
     */
    @Override
    public void modifyClass() {
        getModifierBuilder().modifyMethod("update", (ctClass, ctMethod) -> {
            try {
                ctMethod.setBody("{"
                        + "if (!s_keyboardStateCache.getState().isCreated()) {"
                        + "s_keyboardStateCache.swap();"
                        + "return;"
                        + "}"
                        + "int keyCount = s_keyboardStateCache.getState().getKeyCount();"
                        + "if (bDown == null) {"
                        + "bDown = new boolean[keyCount];"
                        + "bLastDown = new boolean[keyCount];"
                        + "bEatKey = new boolean[keyCount];"
                        + "}"
                        + "boolean isBlockInput = " + WidgetManager.class.getName() + ".isBlockInputKeyboard() || " + Core.class.getName() + ".CurrentTextEntryBox != null && " + Core.class.getName() + ".CurrentTextEntryBox.DoingTextEntry;"
                        + "for (int i = 1; i < keyCount; i++) {"
                        + "bLastDown[i] = bDown[i];"
                        + "bDown[i] = s_keyboardStateCache.getState().isKeyDown(i);"
                        + "if (!bDown[i] && bLastDown[i]) {"
                        + "if (bEatKey[i]) {"
                        + "bEatKey[i] = false;"
                        + "} else if (" + InputWidgetHandler.class.getName() + ".onKeyRelease(i) && !bNoEventsWhileLoading && !isBlockInput && (" + LuaManager.class.getName() + ".thread != " + UIManager.class.getName() + ".defaultthread || !" + UIManager.class.getName() + ".onKeyRelease(i))) {"
                        + "if (" + LuaManager.class.getName() + ".thread == " + UIManager.class.getName() + ".defaultthread && doLuaKeyPressed) {"
                        + LuaEventManager.class.getName() + ".triggerEvent(\"OnKeyPressed\", Integer.valueOf(i));"
                        + "}"
                        + "if (" + LuaManager.class.getName() + ".thread == " + UIManager.class.getName() + ".defaultthread) {"
                        + LuaEventManager.class.getName() + ".triggerEvent(\"OnCustomUIKey\", Integer.valueOf(i));"
                        + LuaEventManager.class.getName() + ".triggerEvent(\"OnCustomUIKeyReleased\", Integer.valueOf(i));"
                        + "}"
                        + "}"
                        + "}"
                        + "if (bDown[i] && bLastDown[i]) {"
                        + "if (" + InputWidgetHandler.class.getName() + ".onKeyRepeat(i) && !bNoEventsWhileLoading && !isBlockInput && (" + LuaManager.class.getName() + ".thread != " + UIManager.class.getName() + ".defaultthread || !" + UIManager.class.getName() + ".onKeyRepeat(i))) {"
                        + "if (" + LuaManager.class.getName() + ".thread == " + UIManager.class.getName() + ".defaultthread && doLuaKeyPressed) {"
                        + LuaEventManager.class.getName() + ".triggerEvent(\"OnKeyKeepPressed\", Integer.valueOf(i));"
                        + "}"
                        + "}"
                        + "}"
                        + "if (bDown[i] && !bLastDown[i] && " + InputWidgetHandler.class.getName() + ".onKeyPress(i) && !bNoEventsWhileLoading && !isBlockInput && !bEatKey[i] && ((" + LuaManager.class.getName() + ".thread != " + UIManager.class.getName() + ".defaultthread || !" + UIManager.class.getName() + ".onKeyPress(i)) && !bEatKey[i])) {"
                        + "if (" + LuaManager.class.getName() + ".thread == " + UIManager.class.getName() + ".defaultthread && doLuaKeyPressed) {"
                        + LuaEventManager.class.getName() + ".triggerEvent(\"OnKeyStartPressed\", Integer.valueOf(i));"
                        + "}"
                        + "if (" + LuaManager.class.getName() + ".thread == " + UIManager.class.getName() + ".defaultthread) {"
                        + LuaEventManager.class.getName() + ".triggerEvent(\"OnCustomUIKeyPressed\", Integer.valueOf(i));"
                        + "}"
                        + "}"
                        + "}"
                        + "s_keyboardStateCache.swap();"
                        + "}");
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        });
    }
}