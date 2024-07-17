package com.avrix.patches;

import com.avrix.agent.ClassTransformer;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

/**
 * ZLogger patcher
 */
public class PatchZLogger extends ClassTransformer {
    /**
     * Constructor for creating a {@link ClassTransformer} object.
     */
    public PatchZLogger() {
        super("zombie.core.logger.ZLogger");
    }

    /**
     * Method for performing class modification.
     * The implementing method must contain the logic for modifying the target class.
     */
    @Override
    public void modifyClass() {
        getModifierBuilder().modifyMethod("writeUnsafe", (ctClass, ctMethod) -> {
            try {
                ctMethod.instrument(new ExprEditor() {
                    public void edit(MethodCall m) throws CannotCompileException {
                        if (m.getClassName().equalsIgnoreCase("zombie.core.logger.ZLogger$OutputStreams") && m.getMethodName().equalsIgnoreCase("println")) {
                            m.replace("{}");
                        }
                    }
                });
                ctMethod.insertBefore("{" +
                        "if (this.name.equalsIgnoreCase(\"DebugLog-server\") || this.name.equalsIgnoreCase(\"DebugLog\")) return;" +
                        "this.outputStreams.console = java.lang.System.out;" +
                        "this.outputStreams.println($1);" +
                        "}");
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        });
    }
}