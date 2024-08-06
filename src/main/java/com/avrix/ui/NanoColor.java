package com.avrix.ui;

import org.joml.Vector4f;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.system.MemoryStack;

/**
 * {@link NanoColor} is a class representing a color with RGBA components. It provides methods to create and manipulate colors,
 * as well as to convert them to {@link NVGColor} objects for use with NanoVG.
 */
public class NanoColor {
    /**
     * Predefined color constant for white (#FFFFFF).
     */
    public static final NanoColor WHITE = new NanoColor("#FFFFFF").immutable();

    /**
     * Predefined color constant for transparency.
     */
    public static final NanoColor TRANSPARENT = new NanoColor("#FFFFFF").alpha(0).immutable();

    /**
     * Predefined color constant for light gray (#D3D3D3).
     */
    public static final NanoColor LIGHT_GRAY = new NanoColor("#D3D3D3").immutable();

    /**
     * Predefined color constant for silver (#C0C0C0).
     */
    public static final NanoColor SILVER = new NanoColor("#C0C0C0").immutable();

    /**
     * Predefined color constant for gray (#808080).
     */
    public static final NanoColor GRAY = new NanoColor("#808080").immutable();

    /**
     * Predefined color constant for dark gray (#A9A9A9).
     */
    public static final NanoColor DARK_GRAY = new NanoColor("#A9A9A9").immutable();

    /**
     * Predefined color constant for light black (#0A0A0A).
     */
    public static final NanoColor LIGHT_BLACK = new NanoColor("#0A0A0A").immutable();

    /**
     * Predefined color constant for white smoke (#F5F5F5).
     */
    public static final NanoColor WHITE_SMOKE = new NanoColor("#F5F5F5").immutable();

    /**
     * Predefined color constant for black (#000000).
     */
    public static final NanoColor BLACK = new NanoColor("#000000").immutable();

    /**
     * Predefined color constant for red (#FF0000).
     */
    public static final NanoColor RED = new NanoColor("#FF0000").immutable();

    /**
     * Predefined color constant for pink (#FFC0CB).
     */
    public static final NanoColor PINK = new NanoColor("#FFC0CB").immutable();

    /**
     * Predefined color constant for orange (#E59400).
     */
    public static final NanoColor ORANGE = new NanoColor("#E59400").immutable();

    /**
     * Predefined color constant for yellow (#FFFF00).
     */
    public static final NanoColor YELLOW = new NanoColor("#FFFF00").immutable();

    /**
     * Predefined color constant for light yellow (#FFFFE0).
     */
    public static final NanoColor LIGHT_YELLOW = new NanoColor("#FFFFE0").immutable();

    /**
     * Predefined color constant for light blue (#ADD8E6).
     */
    public static final NanoColor LIGHT_BLUE = new NanoColor("#ADD8E6").immutable();

    /**
     * Predefined color constant for green (#00FF00).
     */
    public static final NanoColor GREEN = new NanoColor("#00FF00").immutable();

    /**
     * Predefined color constant for magenta (#FF00FF).
     */
    public static final NanoColor MAGENTA = new NanoColor("#FF00FF").immutable();

    /**
     * Predefined color constant for violet (#EE82EE).
     */
    public static final NanoColor VIOLET = new NanoColor("#EE82EE").immutable();

    /**
     * Predefined color constant for dark violet (#8A2BE2).
     */
    public static final NanoColor DARK_VIOLET = new NanoColor("#8A2BE2").immutable();

    /**
     * Predefined color constant for cyan (#00FFFF).
     */
    public static final NanoColor CYAN = new NanoColor("#00FFFF").immutable();

    /**
     * Predefined color constant for blue (#0000FF).
     */
    public static final NanoColor BLUE = new NanoColor("#0000FF").immutable();

    /**
     * Predefined color constant for baby blue (#0078D7).
     */
    public static final NanoColor BABY_BLUE = new NanoColor("#0078D7").immutable();

    /**
     * Predefined color constant for aqua (#00FFFF).
     */
    public static final NanoColor AQUA = new NanoColor("#00FFFF").immutable();

    /**
     * Predefined color constant for coral (#FF7F50).
     */
    public static final NanoColor CORAL = new NanoColor("#FF7F50").immutable();

    private Vector4f color;
    private boolean immutable = false;

    /**
     * Creates a {@link NanoColor} with the specified RGBA values.
     *
     * @param r the red component (0-255)
     * @param g the green component (0-255)
     * @param b the blue component (0-255)
     * @param a the alpha component (0-255)
     */
    public NanoColor(int r, int g, int b, int a) {
        this((float) r / 255f, (float) g / 255f, (float) b / 255f, (float) a / 255f);
    }

    /**
     * Creates a {@link NanoColor} from a hexadecimal string.
     *
     * @param hex the hexadecimal color string (e.g., "#FFFFFF" for RGB or "#FFFFFFFF" for RGBA)
     */
    public NanoColor(String hex) {
        if (hex.matches("^#[0-9A-Fa-f]{6}$")) {
            // Handle RGB color without alpha
            set(Integer.valueOf(hex.substring(1, 3), 16) / 255f,
                    Integer.valueOf(hex.substring(3, 5), 16) / 255f,
                    Integer.valueOf(hex.substring(5, 7), 16) / 255f,
                    1f); // Default alpha value (fully opaque)
        } else if (hex.matches("^#[0-9A-Fa-f]{8}$")) {
            // Handle RGBA color
            set(Integer.valueOf(hex.substring(1, 3), 16),
                    Integer.valueOf(hex.substring(3, 5), 16),
                    Integer.valueOf(hex.substring(5, 7), 16),
                    Integer.valueOf(hex.substring(7, 9), 16));
        } else {
            System.out.println("[!] Color in HEX format is not valid: " + hex);
            set(1f, 1f, 1f, 1f); // Default to white with full opacity
        }
    }

    /**
     * Creates a {@link NanoColor} by copying another NanoColor.
     *
     * @param color the NanoColor to copy
     */
    public NanoColor(NanoColor color) {
        this(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    /**
     * Creates a {@link NanoColor} with the specified RGBA values.
     *
     * @param r the red component (0.0-1.0)
     * @param g the green component (0.0-1.0)
     * @param b the blue component (0.0-1.0)
     * @param a the alpha component (0.0-1.0)
     */
    public NanoColor(float r, float g, float b, float a) {
        set(r, g, b, a);
    }

    /**
     * Makes this {@link NanoColor} immutable. Any further modifications will create a new {@link NanoColor} instance.
     *
     * @return this {@link NanoColor} instance
     */
    public NanoColor immutable() {
        this.immutable = true;
        return this;
    }

    /**
     * Creates a copy of this {@link NanoColor}.
     *
     * @return a new {@link NanoColor} instance with the same color values
     */
    public NanoColor copy() {
        return new NanoColor(this);
    }

    /**
     * Sets the red component of this {@link NanoColor}.
     *
     * @param r the red component (0.0-1.0)
     * @return this {@link NanoColor} instance if mutable, otherwise a new {@link NanoColor} instance
     */
    public NanoColor red(float r) {
        return set(r, getGreen(), getBlue(), getAlpha());
    }

    /**
     * Sets the green component of this {@link NanoColor}.
     *
     * @param g the green component (0.0-1.0)
     * @return this {@link NanoColor} instance if mutable, otherwise a new {@link NanoColor} instance
     */
    public NanoColor green(float g) {
        return set(getRed(), g, getBlue(), getAlpha());
    }

    /**
     * Sets the blue component of this {@link NanoColor}.
     *
     * @param b the blue component (0.0-1.0)
     * @return this {@link NanoColor} instance if mutable, otherwise a new {@link NanoColor} instance
     */
    public NanoColor blue(float b) {
        return set(getRed(), getGreen(), b, getAlpha());
    }

    /**
     * Sets the alpha component of this {@link NanoColor}.
     *
     * @param a the alpha component (0.0-1.0)
     * @return this {@link NanoColor} instance if mutable, otherwise a new {@link NanoColor} instance
     */
    public NanoColor alpha(float a) {
        return set(getRed(), getGreen(), getBlue(), a);
    }

    /**
     * Sets the RGBA values of this {@link NanoColor} to match another {@link NanoColor}.
     *
     * @param color the {@link NanoColor} to copy values from
     * @return this {@link NanoColor} instance if mutable, otherwise a new {@link NanoColor} instance
     */
    public NanoColor set(NanoColor color) {
        return set(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    /**
     * Sets the RGBA values of this {@link NanoColor}.
     *
     * @param r the red component (0.0-1.0)
     * @param g the green component (0.0-1.0)
     * @param b the blue component (0.0-1.0)
     * @param a the alpha component (0.0-1.0)
     * @return this {@link NanoColor} instance
     */
    public NanoColor set(float r, float g, float b, float a) {
        if (immutable) {
            return new NanoColor(r, g, b, a);
        } else if (color == null) {
            color = new Vector4f(r, g, b, a);
        } else {
            color.set(r, g, b, a);
        }

        return this;
    }

    /**
     * Allocates and returns a new {@link NVGColor} synchronized with this NanoColor.
     *
     * @return the allocated {@link NVGColor} instance
     */
    public NVGColor callocNVG() {
        NVGColor color = NVGColor.calloc();
        return sync(color);
    }

    /**
     * Allocates and returns a new {@link NVGColor} synchronized with this {@link NanoColor} using the specified MemoryStack.
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
     * @param action the action to execute with the NanoColor
     */
    public void tallocNVG(ColorAction action) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            NVGColor color = NVGColor.malloc(stack);
            action.execute(sync(color));
        }
    }

    /**
     * Converts the color to its HEX representation including alpha.
     * The HEX format is #RRGGBBAA.
     *
     * @return the HEX representation of the color with alpha
     */
    public String toHEX() {
        return String.format("#%02X%02X%02X%02X",
                (int) (255 * getRed()),   // Red component
                (int) (255 * getGreen()), // Green component
                (int) (255 * getBlue()),  // Blue component
                (int) (255 * getAlpha())); // Alpha component
    }

    /**
     * Returns the red component of this {@link NanoColor}.
     *
     * @return the red component (0.0-1.0)
     */
    public float getRed() {
        return clamp(color.x(), 0f, 1f);
    }

    /**
     * Returns the green component of this {@link NanoColor}.
     *
     * @return the green component (0.0-1.0)
     */
    public float getGreen() {
        return clamp(color.y(), 0f, 1f);
    }

    /**
     * Returns the blue component of this {@link NanoColor}.
     *
     * @return the blue component (0.0-1.0)
     */
    public float getBlue() {
        return clamp(color.z(), 0f, 1f);
    }

    /**
     * Returns the alpha component of this {@link NanoColor}.
     *
     * @return the alpha component (0.0-1.0)
     */
    public float getAlpha() {
        return clamp(color.w(), 0f, 1f);
    }

    /**
     * Multiplies the RGB components of this {@link NanoColor} by the specified factor.
     *
     * @param factor the factor to multiply by
     * @return a new {@link NanoColor} instance with the modified color values
     */
    public NanoColor multiply(float factor) {
        return new NanoColor(getRed() * factor, getGreen() * factor, getBlue() * factor, getAlpha());
    }

    /**
     * Divides the RGB components of this {@link NanoColor} by the specified quotient.
     *
     * @param quotient the quotient to divide by
     * @return a new {@link NanoColor} instance with the modified color values
     */
    public NanoColor divide(float quotient) {
        return new NanoColor(getRed() / quotient, getGreen() / quotient, getBlue() / quotient, getAlpha());
    }

    /**
     * Checks if the RGB components of this NanoColor match those of another {@link NanoColor}.
     *
     * @param color the {@link NanoColor} to compare with
     * @return true if the RGB components match, false otherwise
     */
    public boolean rgbMatches(NanoColor color) {
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