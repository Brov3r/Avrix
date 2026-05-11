package com.avrix.api.lua;

import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;
import zombie.Lua.LuaManager;

/**
 * ClassTransform mixin that extends the initialization logic of {@link LuaManager.Exposer}.
 */
@CTransformer(LuaManager.Exposer.class)
public final class LuaExposerMixin {
    /**
     * Injected at the HEAD of {@link LuaManager.Exposer#exposeAll()}.
     * <p>
     * This method is invoked automatically by the game's Lua initialization pipeline.
     * It ensures that all custom Java-Lua bindings registered in {@link LuaExtension}
     * are processed alongside the default game exposures.
     */
    @CInject(method = "exposeAll", target = @CTarget(value = "HEAD"))
    private void exposeAllMixin() {
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