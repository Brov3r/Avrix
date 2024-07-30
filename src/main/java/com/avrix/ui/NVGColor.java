package com.avrix.ui;

import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

/**
 * {@link NVGColor} is a class representing a color with RGBA components. It provides methods to create and manipulate colors,
 * as well as to convert them to {@link org.lwjgl.nanovg.NVGColor} objects for use with NanoVG.
 */
public class NVGColor {
    /**
     * Predefined color constant for white (#FFFFFF).
     */
    public static final NVGColor WHITE = new NVGColor("#FFFFFF").immutable();

    /**
     * Predefined color constant for transparency.
     */
    public static final NVGColor TRANSPARENT = new NVGColor("#FFFFFF").alpha(0).immutable();

    /**
     * Predefined color constant for light gray (#D3D3D3).
     */
    public static final NVGColor LIGHT_GRAY = new NVGColor("#D3D3D3").immutable();

    /**
     * Predefined color constant for silver (#C0C0C0).
     */
    public static final NVGColor SILVER = new NVGColor("#C0C0C0").immutable();

    /**
     * Predefined color constant for gray (#808080).
     */
    public static final NVGColor GRAY = new NVGColor("#808080").immutable();

    /**
     * Predefined color constant for dark gray (#A9A9A9).
     */
    public static final NVGColor DARK_GRAY = new NVGColor("#A9A9A9").immutable();

    /**
     * Predefined color constant for light black (#0A0A0A).
     */
    public static final NVGColor LIGHT_BLACK = new NVGColor("#0A0A0A").immutable();

    /**
     * Predefined color constant for white smoke (#F5F5F5).
     */
    public static final NVGColor WHITE_SMOKE = new NVGColor("#F5F5F5").immutable();

    /**
     * Predefined color constant for black (#000000).
     */
    public static final NVGColor BLACK = new NVGColor("#000000").immutable();

    /**
     * Predefined color constant for red (#FF0000).
     */
    public static final NVGColor RED = new NVGColor("#FF0000").immutable();

    /**
     * Predefined color constant for pink (#FFC0CB).
     */
    public static final NVGColor PINK = new NVGColor("#FFC0CB").immutable();

    /**
     * Predefined color constant for orange (#E59400).
     */
    public static final NVGColor ORANGE = new NVGColor("#E59400").immutable();

    /**
     * Predefined color constant for yellow (#FFFF00).
     */
    public static final NVGColor YELLOW = new NVGColor("#FFFF00").immutable();

    /**
     * Predefined color constant for light yellow (#FFFFE0).
     */
    public static final NVGColor LIGHT_YELLOW = new NVGColor("#FFFFE0").immutable();

    /**
     * Predefined color constant for light blue (#ADD8E6).
     */
    public static final NVGColor LIGHT_BLUE = new NVGColor("#ADD8E6").immutable();

    /**
     * Predefined color constant for green (#00FF00).
     */
    public static final NVGColor GREEN = new NVGColor("#00FF00").immutable();

    /**
     * Predefined color constant for magenta (#FF00FF).
     */
    public static final NVGColor MAGENTA = new NVGColor("#FF00FF").immutable();

    /**
     * Predefined color constant for violet (#EE82EE).
     */
    public static final NVGColor VIOLET = new NVGColor("#EE82EE").immutable();

    /**
     * Predefined color constant for dark violet (#8A2BE2).
     */
    public static final NVGColor DARK_VIOLET = new NVGColor("#8A2BE2").immutable();

    /**
     * Predefined color constant for cyan (#00FFFF).
     */
    public static final NVGColor CYAN = new NVGColor("#00FFFF").immutable();

    /**
     * Predefined color constant for blue (#0000FF).
     */
    public static final NVGColor BLUE = new NVGColor("#0000FF").immutable();

    /**
     * Predefined color constant for baby blue (#0078D7).
     */
    public static final NVGColor BABY_BLUE = new NVGColor("#0078D7").immutable();

    /**
     * Predefined color constant for aqua (#00FFFF).
     */
    public static final NVGColor AQUA = new NVGColor("#00FFFF").immutable();

    /**
     * Predefined color constant for coral (#FF7F50).
     */
    public static final NVGColor CORAL = new NVGColor("#FF7F50").immutable();

    private Vector4f color;
    private boolean immutable = false;

    /**
     * Creates a {@link NVGColor} with the specified RGBA values.
     *
     * @param r the red component (0-255)
     * @param g the green component (0-255)
     * @param b the blue component (0-255)
     * @param a the alpha component (0-255)
     */
    public NVGColor(int r, int g, int b, int a) {
        this((float) r / 255f, (float) g / 255f, (float) b / 255f, (float) a / 255f);
    }

    /**
     * Creates a {@link NVGColor} from a hexadecimal string.
     *
     * @param hex the hexadecimal color string (e.g., "#FFFFFF")
     */
    public NVGColor(String hex) {
        this(
                Integer.valueOf(hex.substring(1, 3), 16),
                Integer.valueOf(hex.substring(3, 5), 16),
                Integer.valueOf(hex.substring(5, 7), 16),
                255
        );
    }

    /**
     * Creates a {@link NVGColor} by copying another NVGColor.
     *
     * @param color the NVGColor to copy
     */
    public NVGColor(NVGColor color) {
        this(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    /**
     * Creates a {@link NVGColor} with the specified RGBA values.
     *
     * @param r the red component (0.0-1.0)
     * @param g the green component (0.0-1.0)
     * @param b the blue component (0.0-1.0)
     * @param a the alpha component (0.0-1.0)
     */
    public NVGColor(float r, float g, float b, float a) {
        set(r, g, b, a);
    }

    /**
     * Makes this {@link NVGColor} immutable. Any further modifications will create a new {@link NVGColor} instance.
     *
     * @return this {@link NVGColor} instance
     */
    public NVGColor immutable() {
        this.immutable = true;
        return this;
    }

    /**
     * Creates a copy of this {@link NVGColor}.
     *
     * @return a new {@link NVGColor} instance with the same color values
     */
    public NVGColor copy() {
        return new NVGColor(this);
    }

    /**
     * Sets the red component of this {@link NVGColor}.
     *
     * @param r the red component (0.0-1.0)
     * @return this {@link NVGColor} instance if mutable, otherwise a new {@link NVGColor} instance
     */
    public NVGColor red(float r) {
        return set(r, getGreen(), getBlue(), getAlpha());
    }

    /**
     * Sets the green component of this {@link NVGColor}.
     *
     * @param g the green component (0.0-1.0)
     * @return this {@link NVGColor} instance if mutable, otherwise a new {@link NVGColor} instance
     */
    public NVGColor green(float g) {
        return set(getRed(), g, getBlue(), getAlpha());
    }

    /**
     * Sets the blue component of this {@link NVGColor}.
     *
     * @param b the blue component (0.0-1.0)
     * @return this {@link NVGColor} instance if mutable, otherwise a new {@link NVGColor} instance
     */
    public NVGColor blue(float b) {
        return set(getRed(), getGreen(), b, getAlpha());
    }

    /**
     * Sets the alpha component of this {@link NVGColor}.
     *
     * @param a the alpha component (0.0-1.0)
     * @return this {@link NVGColor} instance if mutable, otherwise a new {@link NVGColor} instance
     */
    public NVGColor alpha(float a) {
        return set(getRed(), getGreen(), getBlue(), a);
    }

    /**
     * Sets the RGBA values of this {@link NVGColor} to match another {@link NVGColor}.
     *
     * @param color the {@link NVGColor} to copy values from
     * @return this {@link NVGColor} instance if mutable, otherwise a new {@link NVGColor} instance
     */
    public NVGColor set(NVGColor color) {
        return set(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    /**
     * Sets the RGBA values of this {@link NVGColor}.
     *
     * @param r the red component (0.0-1.0)
     * @param g the green component (0.0-1.0)
     * @param b the blue component (0.0-1.0)
     * @param a the alpha component (0.0-1.0)
     * @return this {@link NVGColor} instance
     */
    public NVGColor set(float r, float g, float b, float a) {
        if (immutable) {
            return new NVGColor(r, g, b, a);
        } else if (color == null) {
            color = new Vector4f(r, g, b, a);
        } else {
            color.set(r, g, b, a);
        }

        return this;
    }

    /**
     * Allocates and returns a new {@link org.lwjgl.nanovg.NVGColor} synchronized with this NVGColor.
     *
     * @return the allocated {@link org.lwjgl.nanovg.NVGColor} instance
     */
    public org.lwjgl.nanovg.NVGColor callocNVG() {
        org.lwjgl.nanovg.NVGColor color = org.lwjgl.nanovg.NVGColor.calloc();
        return sync(color);
    }

    /**
     * Allocates and returns a new {@link org.lwjgl.nanovg.NVGColor} synchronized with this {@link NVGColor} using the specified MemoryStack.
     *
     * @param stack the MemoryStack to use for allocation
     * @return the allocated {@link org.lwjgl.nanovg.NVGColor} instance
     */
    public org.lwjgl.nanovg.NVGColor mallocNVG(MemoryStack stack) {
        org.lwjgl.nanovg.NVGColor color = org.lwjgl.nanovg.NVGColor.malloc(stack);
        return sync(color);
    }

    private org.lwjgl.nanovg.NVGColor sync(org.lwjgl.nanovg.NVGColor color) {
        return color.r(getRed()).g(getGreen()).b(getBlue()).a(getAlpha());
    }

    /**
     * Temporarily allocates an {@link org.lwjgl.nanovg.NVGColor} and executes the given action with it. The {@link org.lwjgl.nanovg.NVGColor} will be automatically cleaned up after the action is executed.
     * <br><br>
     * Usage example:
     * <br>
     * <code>uiColor.tallocNVG(nvgColor -> {<br>
     * nvgFillColor(contextID, nvgColor);<br>
     * });</code>
     *
     * @param action the action to execute with the NVGColor
     */
    public void tallocNVG(ColorAction action) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            org.lwjgl.nanovg.NVGColor color = org.lwjgl.nanovg.NVGColor.malloc(stack);
            action.execute(sync(color));
        }
    }

    /**
     * Converts this {@link NVGColor} to a hexadecimal color string.
     *
     * @return the hexadecimal color string
     */
    public String toHEX() {
        return String.format("#%02X%02X%02X", (int) (255 * getRed()), (int) (255 * getGreen()), (int) (255 * getBlue()));
    }

    /**
     * Returns the red component of this {@link NVGColor}.
     *
     * @return the red component (0.0-1.0)
     */
    public float getRed() {
        return clamp(color.x(), 0f, 1f);
    }

    /**
     * Returns the green component of this {@link NVGColor}.
     *
     * @return the green component (0.0-1.0)
     */
    public float getGreen() {
        return clamp(color.y(), 0f, 1f);
    }

    /**
     * Returns the blue component of this {@link NVGColor}.
     *
     * @return the blue component (0.0-1.0)
     */
    public float getBlue() {
        return clamp(color.z(), 0f, 1f);
    }

    /**
     * Returns the alpha component of this {@link NVGColor}.
     *
     * @return the alpha component (0.0-1.0)
     */
    public float getAlpha() {
        return clamp(color.w(), 0f, 1f);
    }

    /**
     * Multiplies the RGB components of this {@link NVGColor} by the specified factor.
     *
     * @param factor the factor to multiply by
     * @return a new {@link NVGColor} instance with the modified color values
     */
    public NVGColor multiply(float factor) {
        return new NVGColor(getRed() * factor, getGreen() * factor, getBlue() * factor, getAlpha());
    }

    /**
     * Divides the RGB components of this {@link NVGColor} by the specified quotient.
     *
     * @param quotient the quotient to divide by
     * @return a new {@link NVGColor} instance with the modified color values
     */
    public NVGColor divide(float quotient) {
        return new NVGColor(getRed() / quotient, getGreen() / quotient, getBlue() / quotient, getAlpha());
    }

    /**
     * Checks if the RGB components of this NVGColor match those of another {@link NVGColor}.
     *
     * @param color the {@link NVGColor} to compare with
     * @return true if the RGB components match, false otherwise
     */
    public boolean rgbMatches(NVGColor color) {
        return (getRed() == color.getRed() && getGreen() == color.getGreen() && getBlue() == color.getBlue());
    }

    /**
     * Functional interface for performing actions with an {@link org.lwjgl.nanovg.NVGColor} instance.
     */
    public interface ColorAction {
        /**
         * Executes an action with the given {@link org.lwjgl.nanovg.NVGColor} instance.
         *
         * @param color the {@link org.lwjgl.nanovg.NVGColor} instance to use in the action
         */
        void execute(org.lwjgl.nanovg.NVGColor color);
    }

    /**
     * Clamps a value between a minimum and maximum.
     *
     * @param value the value to clamp
     * @param min   the minimum value
     * @param max   the maximum value
     * @return the clamped value
     */
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }
}