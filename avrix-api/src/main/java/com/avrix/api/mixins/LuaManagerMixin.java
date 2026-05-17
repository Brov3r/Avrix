package com.avrix.api.mixins;

import com.avrix.api.events.EventManager;
import com.avrix.api.lua.LuaExtension;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;
import zombie.Lua.LuaManager;

@CTransformer(LuaManager.class)
public class LuaManagerMixin {
    @CInject(
            method = "init",
            target = @CTarget(
                    value = "INVOKE",
                    target = "zombie/Lua/LuaManager$Exposer.exposeAll()V",
                    shift = CTarget.Shift.BEFORE
            )
    )
    public static void initBeforeExposerInject() {
        // Register custom events
        EventManager.registerCustomEvents();

        // Register custom Java classes for Lua exposure
        for (Class<?> clazz : LuaExtension.getExposedClasses()) {
            LuaManager.exposer.setExposed(clazz);
        }

        // Register custom global objects for Lua function exposure
        for (Object object : LuaExtension.getExposedGlobalObjects()) {
            LuaManager.exposer.exposeGlobalFunctions(object);
        }
    }
}
