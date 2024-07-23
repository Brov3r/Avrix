package com.avrix.ui;

import org.joml.Vector4f;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.system.MemoryStack;

/**
 * {@link UIColor} is a class representing a color with RGBA components. It provides methods to create and manipulate colors,
 * as well as to convert them to {@link NVGColor} objects for use with NanoVG.
 */
public class UIColor {
    /**
     * Predefined color constant for white (#FFFFFF).
     */
    public static final UIColor WHITE = new UIColor("#FFFFFF").immutable();

    /**
     * Predefined color constant for transparency.
     */
    public static final UIColor TRANSPARENT = new UIColor("#FFFFFF").alpha(0).immutable();

    /**
     * Predefined color constant for light gray (#D3D3D3).
     */
    public static final UIColor LIGHT_GRAY = new UIColor("#D3D3D3").immutable();

    /**
     * Predefined color constant for silver (#C0C0C0).
     */
    public static final UIColor SILVER = new UIColor("#C0C0C0").immutable();

    /**
     * Predefined color constant for gray (#808080).
     */
    public static final UIColor GRAY = new UIColor("#808080").immutable();

    /**
     * Predefined color constant for dark gray (#A9A9A9).
     */
    public static final UIColor DARK_GRAY = new UIColor("#A9A9A9").immutable();

    /**
     * Predefined color constant for light black (#0A0A0A).
     */
    public static final UIColor LIGHT_BLACK = new UIColor("#0A0A0A").immutable();

    /**
     * Predefined color constant for white smoke (#F5F5F5).
     */
    public static final UIColor WHITE_SMOKE = new UIColor("#F5F5F5").immutable();

    /**
     * Predefined color constant for black (#000000).
     */
    public static final UIColor BLACK = new UIColor("#000000").immutable();

    /**
     * Predefined color constant for red (#FF0000).
     */
    public static final UIColor RED = new UIColor("#FF0000").immutable();

    /**
     * Predefined color constant for pink (#FFC0CB).
     */
    public static final UIColor PINK = new UIColor("#FFC0CB").immutable();

    /**
     * Predefined color constant for orange (#E59400).
     */
    public static final UIColor ORANGE = new UIColor("#E59400").immutable();

    /**
     * Predefined color constant for yellow (#FFFF00).
     */
    public static final UIColor YELLOW = new UIColor("#FFFF00").immutable();

    /**
     * Predefined color constant for light yellow (#FFFFE0).
     */
    public static final UIColor LIGHT_YELLOW = new UIColor("#FFFFE0").immutable();

    /**
     * Predefined color constant for light blue (#ADD8E6).
     */
    public static final UIColor LIGHT_BLUE = new UIColor("#ADD8E6").immutable();

    /**
     * Predefined color constant for green (#00FF00).
     */
    public static final UIColor GREEN = new UIColor("#00FF00").immutable();

    /**
     * Predefined color constant for magenta (#FF00FF).
     */
    public static final UIColor MAGENTA = new UIColor("#FF00FF").immutable();

    /**
     * Predefined color constant for violet (#EE82EE).
     */
    public static final UIColor VIOLET = new UIColor("#EE82EE").immutable();

    /**
     * Predefined color constant for dark violet (#8A2BE2).
     */
    public static final UIColor DARK_VIOLET = new UIColor("#8A2BE2").immutable();

    /**
     * Predefined color constant for cyan (#00FFFF).
     */
    public static final UIColor CYAN = new UIColor("#00FFFF").immutable();

    /**
     * Predefined color constant for blue (#0000FF).
     */
    public static final UIColor BLUE = new UIColor("#0000FF").immutable();

    /**
     * Predefined color constant for baby blue (#0078D7).
     */
    public static final UIColor BABY_BLUE = new UIColor("#0078D7").immutable();

    /**
     * Predefined color constant for aqua (#00FFFF).
     */
    public static final UIColor AQUA = new UIColor("#00FFFF").immutable();

    /**
     * Predefined color constant for coral (#FF7F50).
     */
    public static final UIColor CORAL = new UIColor("#FF7F50").immutable();

    private Vector4f color;
    private boolean immutable = false;

    /**
     * Creates a {@link UIColor} with the specified RGBA values.
     *
     * @param r the red component (0-255)
     * @param g the green component (0-255)
     * @param b the blue component (0-255)
     * @param a the alpha component (0-255)
     */
    public UIColor(int r, int g, int b, int a) {
        this((float) r / 255f, (float) g / 255f, (float) b / 255f, (float) a / 255f);
    }

    /**
     * Creates a {@link UIColor} from a hexadecimal string.
     *
     * @param hex the hexadecimal color string (e.g., "#FFFFFF")
     */
    public UIColor(String hex) {
        this(
                Integer.valueOf(hex.substring(1, 3), 16),
                Integer.valueOf(hex.substring(3, 5), 16),
                Integer.valueOf(hex.substring(5, 7), 16),
                255
        );
    }

    /**
     * Creates a {@link UIColor} by copying another UIColor.
     *
     * @param color the UIColor to copy
     */
    public UIColor(UIColor color) {
        this(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    /**
     * Creates a {@link UIColor} with the specified RGBA values.
     *
     * @param r the red component (0.0-1.0)
     * @param g the green component (0.0-1.0)
     * @param b the blue component (0.0-1.0)
     * @param a the alpha component (0.0-1.0)
     */
    public UIColor(float r, float g, float b, float a) {
        set(r, g, b, a);
    }

    /**
     * Makes this {@link UIColor} immutable. Any further modifications will create a new {@link UIColor} instance.
     *
     * @return this {@link UIColor} instance
     */
    public UIColor immutable() {
        this.immutable = true;
        return this;
    }

    /**
     * Creates a copy of this {@link UIColor}.
     *
     * @return a new {@link UIColor} instance with the same color values
     */
    public UIColor copy() {
        return new UIColor(this);
    }

    /**
     * Sets the red component of this {@link UIColor}.
     *
     * @param r the red component (0.0-1.0)
     * @return this {@link UIColor} instance if mutable, otherwise a new {@link UIColor} instance
     */
    public UIColor red(float r) {
        return set(r, getGreen(), getBlue(), getAlpha());
    }

    /**
     * Sets the green component of this {@link UIColor}.
     *
     * @param g the green component (0.0-1.0)
     * @return this {@link UIColor} instance if mutable, otherwise a new {@link UIColor} instance
     */
    public UIColor green(float g) {
        return set(getRed(), g, getBlue(), getAlpha());
    }

    /**
     * Sets the blue component of this {@link UIColor}.
     *
     * @param b the blue component (0.0-1.0)
     * @return this {@link UIColor} instance if mutable, otherwise a new {@link UIColor} instance
     */
    public UIColor blue(float b) {
        return set(getRed(), getGreen(), b, getAlpha());
    }

    /**
     * Sets the alpha component of this {@link UIColor}.
     *
     * @param a the alpha component (0.0-1.0)
     * @return this {@link UIColor} instance if mutable, otherwise a new {@link UIColor} instance
     */
    public UIColor alpha(float a) {
        return set(getRed(), getGreen(), getBlue(), a);
    }

    /**
     * Sets the RGBA values of this {@link UIColor} to match another {@link UIColor}.
     *
     * @param color the {@link UIColor} to copy values from
     * @return this {@link UIColor} instance if mutable, otherwise a new {@link UIColor} instance
     */
    public UIColor set(UIColor color) {
        return set(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    /**
     * Sets the RGBA values of this {@link UIColor}.
     *
     * @param r the red component (0.0-1.0)
     * @param g the green component (0.0-1.0)
     * @param b the blue component (0.0-1.0)
     * @param a the alpha component (0.0-1.0)
     * @return this {@link UIColor} instance
     */
    public UIColor set(float r, float g, float b, float a) {
        if (immutable) {
            return new UIColor(r, g, b, a);
        } else if (color == null) {
            color = new Vector4f(r, g, b, a);
        } else {
            color.set(r, g, b, a);
        }

        return this;
    }

    /**
     * Allocates and returns a new {@link NVGColor} synchronized with this UIColor.
     *
     * @return the allocated {@link NVGColor} instance
     */
    public NVGColor callocNVG() {
        NVGColor color = NVGColor.calloc();
        return sync(color);
    }

    /**
     * Allocates and returns a new {@link NVGColor} synchronized with this {@link UIColor} using the specified MemoryStack.
     *
     * @param stack the MemoryStack to use for allocation
     * @return the allocated {@link NVGColor} instance
     */
    public NVGColor mallocNVG(MemoryStack stack) {
        NVGColor color = NVGColor.malloc(stack);
        return sync(color);
    }

    private NVGColor sync(NVGColor color) {
        return color.r(getRed()).g(getGreen()).b(getBlue()).a(getAlpha());
    }

    /**
     * Temporarily allocates an {@link NVGColor} and executes the given action with it. The {@link NVGColor} will be automatically cleaned up after the action is executed.
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
            NVGColor color = NVGColor.malloc(stack);
            action.execute(sync(color));
        }
    }

    /**
     * Converts this {@link UIColor} to a hexadecimal color string.
     *
     * @return the hexadecimal color string
     */
    public String toHEX() {
        return String.format("#%02X%02X%02X", (int) (255 * getRed()), (int) (255 * getGreen()), (int) (255 * getBlue()));
    }

    /**
     * Returns the red component of this {@link UIColor}.
     *
     * @return the red component (0.0-1.0)
     */
    public float getRed() {
        return clamp(color.x(), 0f, 1f);
    }

    /**
     * Returns the green component of this {@link UIColor}.
     *
     * @return the green component (0.0-1.0)
     */
    public float getGreen() {
        return clamp(color.y(), 0f, 1f);
    }

    /**
     * Returns the blue component of this {@link UIColor}.
     *
     * @return the blue component (0.0-1.0)
     */
    public float getBlue() {
        return clamp(color.z(), 0f, 1f);
    }

    /**
     * Returns the alpha component of this {@link UIColor}.
     *
     * @return the alpha component (0.0-1.0)
     */
    public float getAlpha() {
        return clamp(color.w(), 0f, 1f);
    }

    /**
     * Multiplies the RGB components of this {@link UIColor} by the specified factor.
     *
     * @param factor the factor to multiply by
     * @return a new {@link UIColor} instance with the modified color values
     */
    public UIColor multiply(float factor) {
        return new UIColor(getRed() * factor, getGreen() * factor, getBlue() * factor, getAlpha());
    }

    /**
     * Divides the RGB components of this {@link UIColor} by the specified quotient.
     *
     * @param quotient the quotient to divide by
     * @return a new {@link UIColor} instance with the modified color values
     */
    public UIColor divide(float quotient) {
        return new UIColor(getRed() / quotient, getGreen() / quotient, getBlue() / quotient, getAlpha());
    }

    /**
     * Checks if the RGB components of this UIColor match those of another {@link UIColor}.
     *
     * @param color the {@link UIColor} to compare with
     * @return true if the RGB components match, false otherwise
     */
    public boolean rgbMatches(UIColor color) {
        return (getRed() == color.getRed() && getGreen() == color.getGreen() && getBlue() == color.getBlue());
    }

    /**
     * Functional interface for performing actions with an {@link NVGColor} instance.
     */
    public interface ColorAction {
        /**
         * Executes an action with the given {@link NVGColor} instance.
         *
         * @param color the {@link NVGColor} instance to use in the action
         */
        void execute(NVGColor color);
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