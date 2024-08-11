package com.avrix.ui.notify;

import com.avrix.ui.NanoColor;
import com.avrix.ui.NanoDrawer;
import com.avrix.utils.WindowUtils;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * The Notification class represents a notification that is displayed on the screen and has a limited lifetime.
 * It contains text, an icon, and various visual properties.
 */
public class Notification {
    /**
     * The font name used to display the text.
     */
    protected String fontName = "Montserrat-Regular";

    /**
     * The font name used to display the icon.
     */
    protected String iconFontName = "FontAwesome";

    /**
     * The notification text.
     */
    protected String text;

    /**
     * The notification icon.
     */
    protected String icon;

    /**
     * The font size for the text.
     */
    protected int fontSize = 16;

    /**
     * Line spacing
     */
    protected int lineSpacing = 2;

    /**
     * Maximum number of lines
     */
    protected int maxLines = 4;

    /**
     * The font size for the icon.
     */
    protected int iconFontSize = 24;

    /**
     * The margin inside the notification.
     */
    protected int margin = 10;

    /**
     * The width of the notification.
     */
    protected int width;

    /**
     * The height of the notification.
     */
    protected int height;

    /**
     * The lifetime of the notification in seconds.
     */
    protected int lifeTime;

    /**
     * The creation time of the notification in milliseconds since the program started.
     */
    protected long creationTime = 0L;

    /**
     * The expiration time of the notification in milliseconds since the program started.
     */
    protected long expirationTime;

    /**
     * The visibility of the notification. If true, the notification is displayed on the screen.
     */
    protected boolean visible = false;

    /**
     * The background color of the notification.
     */
    protected NanoColor backgroundColor = NanoColor.LIGHT_BLACK;

    /**
     * The color of the notification text.
     */
    protected NanoColor textColor = NanoColor.WHITE;

    /**
     * The color of the notification icon.
     */
    protected NanoColor iconColor;

    /**
     * The color of the time bar that shows the remaining lifetime of the notification.
     */
    protected NanoColor timeColor;

    /**
     * Creates a new notification with the specified text, icon, lifetime, icon color, and time bar color.
     *
     * @param text         The notification text.
     * @param icon         The notification icon.
     * @param lifeTime     The lifetime of the notification in seconds.
     * @param iconColor    The color of the icon.
     * @param timeBarColor The color of the time bar indicating remaining time.
     */
    Notification(String text, String icon, int lifeTime, NanoColor iconColor, NanoColor timeBarColor) {
        this.text = text;
        this.icon = icon;
        this.lifeTime = lifeTime;
        this.expirationTime = lifeTime * 1000L;
        this.iconColor = iconColor;
        this.timeColor = timeBarColor;
    }

    /**
     * Creates a new notification with the specified text, icon, lifetime, and icon color.
     * The time bar color will be the same as the icon color.
     *
     * @param text        The notification text.
     * @param icon        The notification icon.
     * @param lifeTime    The lifetime of the notification in seconds.
     * @param accentColor The color of the icon and the time bar.
     */
    Notification(String text, String icon, int lifeTime, NanoColor accentColor) {
        this(text, icon, lifeTime, accentColor, accentColor);
    }

    /**
     * Creates a new notification with the specified text, icon, and lifetime.
     * The icon color is set to white, and the time bar color is set to green.
     *
     * @param text     The notification text.
     * @param icon     The notification icon.
     * @param lifeTime The lifetime of the notification in seconds.
     */
    Notification(String text, String icon, int lifeTime) {
        this(text, icon, lifeTime, NanoColor.WHITE, NanoColor.GREEN);
    }

    /**
     * Sets the font name for the text.
     *
     * @param fontName The name of the font.
     */
    public void setFontName(String fontName) {
        this.fontName = fontName;
    }

    /**
     * Sets the font name for the icon.
     *
     * @param iconFontName The name of the icon font.
     */
    public void setIconFontName(String iconFontName) {
        this.iconFontName = iconFontName;
    }

    /**
     * Sets the font size for the text.
     *
     * @param fontSize The size of the font.
     */
    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    /**
     * Sets the font size for the icon.
     *
     * @param iconFontSize The size of the icon font.
     */
    public void setIconFontSize(int iconFontSize) {
        this.iconFontSize = iconFontSize;
    }

    /**
     * Sets the margin inside the notification.
     *
     * @param margin The margin in pixels.
     */
    public void setMargin(int margin) {
        this.margin = margin;
    }

    /**
     * Sets the width of the notification.
     *
     * @param width The width in pixels.
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Sets the height of the notification.
     *
     * @param height The height in pixels.
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Sets the lifetime of the notification.
     *
     * @param lifeTime The lifetime in seconds.
     */
    public void setLifeTime(int lifeTime) {
        this.lifeTime = lifeTime;
    }

    /**
     * Sets the background color of the notification.
     *
     * @param backgroundColor The background color.
     */
    public void setBackgroundColor(NanoColor backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    /**
     * Sets the color of the time bar.
     *
     * @param timeColor The color of the time bar.
     */
    public void setTimeColor(NanoColor timeColor) {
        this.timeColor = timeColor;
    }

    /**
     * Sets the color of the text.
     *
     * @param textColor The color of the text.
     */
    public void setTextColor(NanoColor textColor) {
        this.textColor = textColor;
    }

    /**
     * Sets the color of the icon.
     *
     * @param iconColor The color of the icon.
     */
    public void setIconColor(NanoColor iconColor) {
        this.iconColor = iconColor;
    }

    /**
     * Returns the height of the notification.
     *
     * @return The height in pixels.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the width of the notification.
     *
     * @return The width in pixels.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Checks if the notification has expired.
     *
     * @return true if the notification is expired, false otherwise.
     */
    public boolean isExpired() {
        return visible && (System.currentTimeMillis() > expirationTime);
    }

    /**
     * Wraps the given text to fit within the specified width.
     *
     * @param text  The text to wrap.
     * @param width The maximum width for the text.
     * @return An array of strings, where each string is a line of wrapped text.
     */
    protected List<String> wrapText(String text, int width) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        List<String> lines = new ArrayList<>();

        if (words.length == 1 && NanoDrawer.getTextSize(words[0], fontName, fontSize).x > width) {
            lines.add(NanoDrawer.truncateText(words[0], fontName, fontSize, width));
            return lines;
        }

        for (String word : words) {
            // Check if adding the new word exceeds the width
            String testLine = line + (line.isEmpty() ? "" : " ") + word;
            Vector2f size = NanoDrawer.getTextSize(testLine, fontName, fontSize);

            if (lines.size() >= maxLines) {
                String lastLine = NanoDrawer.truncateText(lines.get(maxLines - 1) + "...", fontName, fontSize, width);
                lines.remove(maxLines - 1);
                lines.add(lastLine);
                return lines;
            }
            if (size.x > width) {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                if (!line.isEmpty()) {
                    line.append(" ");
                }
                line.append(word);
            }
        }

        // Add the last line if it's not empty
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }

        return lines;
    }

    /**
     * Displays the notification.
     * Initializes the creation and expiration times, as well as calculates the size of the notification.
     */
    public void show() {
        if (visible) return;

        visible = true;
        creationTime = System.currentTimeMillis();
        expirationTime += System.currentTimeMillis();

        // Calculate the size of the icon
        Vector2f iconSize = NanoDrawer.getTextSize(icon, iconFontName, iconFontSize);

        // Wrap the text if necessary and calculate the size
        Vector2f textSize = NanoDrawer.getTextSize(text, fontName, fontSize);

        // Update width and height based on wrapped text
        width = (int) Math.min(textSize.x + iconSize.x + margin * 3, (float) WindowUtils.getWindowWidth() / 3);
        List<String> wrappedText = wrapText(text, width);
        height = (int) (margin * 2 + (Math.max(textSize.y, iconSize.y)) * wrappedText.size());
    }

    /**
     * Renders the notification at the specified position on the screen.
     *
     * @param x The x-coordinate of the top-left corner of the notification.
     * @param y The y-coordinate of the top-left corner of the notification.
     */
    public void render(int x, int y) {
        NanoDrawer.saveRenderState();
        NanoDrawer.intersectScissor(x, y, width, height);

        Vector2f iconSize = NanoDrawer.getTextSize(icon, iconFontName, iconFontSize);

        // Draw the background and icon
        NanoDrawer.drawRect(x, y, width, height, backgroundColor);
        NanoDrawer.drawText(icon, iconFontName, x + margin, (int) (y + (height - iconSize.y) / 2 - iconFontSize / 4), iconFontSize, iconColor);

        // Wrap the text if necessary and calculate the size
        List<String> wrappedText = wrapText(text, (int) (width - iconSize.x - margin * 3));
        int totalTextHeight = wrappedText.size() * (fontSize + lineSpacing) - lineSpacing; // Total height of all lines

        // Calculate the starting Y position to vertically center the text
        int textY = y + (height - totalTextHeight) / 2 - fontSize / 4;

        // Draw each line of the wrapped text
        for (String line : wrappedText) {
            NanoDrawer.drawText(line, fontName, (int) (x + margin * 2 + iconSize.x), textY, fontSize, textColor);
            textY += fontSize + lineSpacing; // Move to the next line
        }

        // Draw the remaining time bar
        long currentTime = System.currentTimeMillis();
        long totalLifetime = expirationTime - creationTime;
        long remainingTime = expirationTime - currentTime;
        double remainingTimeRatio = Math.max(0, Math.min(1, (double) remainingTime / totalLifetime));
        int barWidth = (int) (width * remainingTimeRatio);
        NanoDrawer.drawRect(x, y + height - 4, width, 4, timeColor.alpha(0.1f));
        NanoDrawer.drawRect(x, y + height - 4, barWidth, 4, timeColor);

        NanoDrawer.restoreRenderState();
    }
}