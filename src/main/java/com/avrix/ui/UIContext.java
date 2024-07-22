package com.avrix.ui;

import org.lwjgl.nanovg.NanoVGGL2;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;

import static org.lwjgl.nanovg.NanoVG.nvgBeginFrame;
import static org.lwjgl.nanovg.NanoVG.nvgEndFrame;

/**
 * The UIContext class encapsulates a NanoVG context and provides utility methods for managing
 * and rendering with the context in an OpenGL environment.
 */
public class UIContext {
    private final boolean modernOpenGL;
    private final long context;

    /**
     * Initializes a NanoVG context and returns this object.
     */
    public UIContext() {
        this.modernOpenGL = (GL11.glGetInteger(GL30.GL_MAJOR_VERSION) > 3) || (GL11.glGetInteger(GL30.GL_MAJOR_VERSION) == 3 && GL11.glGetInteger(GL30.GL_MINOR_VERSION) >= 2);

        if (this.modernOpenGL) {
            this.context = NanoVGGL3.nvgCreate(NanoVGGL3.NVG_STENCIL_STROKES | NanoVGGL3.NVG_ANTIALIAS);
        } else {
            this.context = NanoVGGL2.nvgCreate(NanoVGGL2.NVG_STENCIL_STROKES | NanoVGGL2.NVG_ANTIALIAS);
        }
    }

    /**
     * Begins a new frame for rendering using NanoVG. This method calculates the pixel ratio based on the given window dimensions.
     *
     * @param windowWidth  the width of the window in pixels
     * @param windowHeight the height of the window in pixels
     */
    public void beginFrame(int windowWidth, int windowHeight) {
        beginFrame(windowWidth, windowHeight, (float) windowWidth / (float) windowHeight);
    }

    /**
     * Begins a new frame for rendering using NanoVG with a specified pixel ratio.
     * This method should be called at the start of each frame before any drawing operations.
     *
     * @param windowWidth  the width of the window in pixels
     * @param windowHeight the height of the window in pixels
     * @param pxRatio      the pixel ratio, which is typically the ratio of the framebuffer size to the window size
     */
    public void beginFrame(int windowWidth, int windowHeight, float pxRatio) {
        nvgBeginFrame(context, windowWidth, windowHeight, pxRatio);
    }

    /**
     * Ends the current frame of rendering (<code>nvgEndFrame(context)</code>)
     */
    public void endFrame() {
        nvgEndFrame(context);
        GL40.glBlendFunc(GL40.GL_SRC_ALPHA, GL40.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * @return true if the machine this program is running on supports modern OpenGL (Version 3 and above)
     */
    public boolean isModernOpenGL() {
        return modernOpenGL;
    }

    /**
     * @return the NanoVG context long ID.
     */
    public long get() {
        return context;
    }

    /**
     * Disposes this NanoVG context.
     */
    public void dispose() {
        if (modernOpenGL) {
            NanoVGGL3.nvgDelete(context);
        } else {
            NanoVGGL2.nvgDelete(context);
        }
    }
}