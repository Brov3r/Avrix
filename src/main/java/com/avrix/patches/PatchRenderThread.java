package com.avrix.patches;

import com.avrix.agent.ClassTransformer;
import com.avrix.ui.NVGContext;
import com.avrix.ui.WidgetManager;
import javassist.CannotCompileException;

/**
 * RenderThread patcher
 */
public class PatchRenderThread extends ClassTransformer {
    /**
     * Constructor for creating a {@link ClassTransformer} object.
     */
    public PatchRenderThread() {
        super("zombie.core.opengl.RenderThread");
    }

    /**
     * Method for performing class modification.
     * The implementing method must contain the logic for modifying the target class.
     */
    @Override
    public void modifyClass() {
        getModifierBuilder().modifyMethod("shutdown", (ctClass, ctMethod) -> {
            try {
                ctMethod.insertBefore("{" +
                        NVGContext.class.getName() + " context = " + WidgetManager.class.getName() + ".getContext();" +
                        "if (context != null) {" +
                        "System.out.println(\"[#] Closing the NVG context in the render thread...\");" +
                        "context.dispose();" +
                        "}" +
                        "}");
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        });
    }
}