package com.avrix.enums;

import com.avrix.ui.NanoColor;

/**
 * Enum representing different types of notifications.
 * Each type has an associated icon and color.
 */
public enum NotificationType {
    /**
     * Informational notification.
     */
    INFO("\uf129", NanoColor.BABY_BLUE),

    /**
     * Success notification.
     */
    SUCCESS("\uf00c", NanoColor.GREEN),

    /**
     * Warning notification.
     */
    WARN("\uf071", NanoColor.ORANGE),

    /**
     * Error notification.
     */
    ERROR("\uf00d", NanoColor.RED),

    /**
     * Critical notification.
     */
    CRITICAL("\uf12a", NanoColor.RED),

    /**
     * Hint notification.
     */
    HINT("\uf0eb", NanoColor.LIGHT_BLUE);

    /**
     * The icon associated with the notification type.
     * This field contains the Unicode character for the icon that represents the notification type.
     */
    private final String icon;

    /**
     * The color associated with the notification type.
     */
    private final NanoColor color;

    /**
     * Constructs a NotificationType with the specified icon and color.
     *
     * @param icon  The icon associated with the notification type.
     * @param color The color associated with the notification type.
     */
    NotificationType(String icon, NanoColor color) {
        this.icon = icon;
        this.color = color;
    }

    /**
     * Returns the icon for this notification type.
     *
     * @return The icon.
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Returns the color for this notification type.
     *
     * @return Notification color
     */
    public NanoColor getColor() {
        return color;
    }
}