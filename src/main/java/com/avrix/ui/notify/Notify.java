package com.avrix.ui.notify;

import com.avrix.enums.NotificationType;
import com.avrix.utils.WindowUtils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * The {@code Notify} class is responsible for managing and rendering notifications.
 * It maintains a queue of notifications and provides methods to display various types of notifications.
 */
public class Notify {
    /**
     * A queue of {@link Notification} objects to be displayed.
     */
    private static final Queue<Notification> notifications = new LinkedList<>();

    /**
     * The maximum number of notifications that can be visible at once.
     */
    private static int maxVisibleNotification = 8;

    /**
     * The margin between notifications.
     */
    private static int notificationMargin = 5;

    /**
     * The offset from the bottom edge of the window where notifications are displayed.
     */
    private static int borderOffsetBottom = 50;

    /**
     * The offset from the right edge of the window where notifications are displayed.
     */
    private static int borderOffsetRight = 10;

    /**
     * Adds a {@link Notification} to the queue.
     *
     * @param notification The notification to be added.
     */
    public synchronized static void notify(Notification notification) {
        System.out.println("[#] Notification: " + notification.text);

        notifications.add(notification);
    }

    /**
     * Displays an informational notification with a specified lifetime.
     *
     * @param text     The notification text.
     * @param lifeTime The lifetime of the notification in seconds.
     */
    public synchronized static void info(String text, int lifeTime) {
        notify(new Notification(text, NotificationType.INFO.getIcon(), lifeTime, NotificationType.INFO.getColor()));
    }

    /**
     * Displays a success notification with a specified lifetime.
     *
     * @param text     The notification text.
     * @param lifeTime The lifetime of the notification in seconds.
     */
    public synchronized static void success(String text, int lifeTime) {
        notify(new Notification(text, NotificationType.SUCCESS.getIcon(), lifeTime, NotificationType.SUCCESS.getColor()));
    }

    /**
     * Displays a warning notification with a specified lifetime.
     *
     * @param text     The notification text.
     * @param lifeTime The lifetime of the notification in seconds.
     */
    public synchronized static void warn(String text, int lifeTime) {
        notify(new Notification(text, NotificationType.WARN.getIcon(), lifeTime, NotificationType.WARN.getColor()));
    }

    /**
     * Displays an error notification with a specified lifetime.
     *
     * @param text     The notification text.
     * @param lifeTime The lifetime of the notification in seconds.
     */
    public synchronized static void error(String text, int lifeTime) {
        notify(new Notification(text, NotificationType.ERROR.getIcon(), lifeTime, NotificationType.ERROR.getColor()));
    }

    /**
     * Displays a critical notification with a specified lifetime.
     *
     * @param text     The notification text.
     * @param lifeTime The lifetime of the notification in seconds.
     */
    public synchronized static void critical(String text, int lifeTime) {
        notify(new Notification(text, NotificationType.CRITICAL.getIcon(), lifeTime, NotificationType.CRITICAL.getColor()));
    }

    /**
     * Displays a hint notification with a specified lifetime.
     *
     * @param text     The notification text.
     * @param lifeTime The lifetime of the notification in seconds.
     */
    public synchronized static void hint(String text, int lifeTime) {
        notify(new Notification(text, NotificationType.HINT.getIcon(), lifeTime, NotificationType.HINT.getColor()));
    }

    /**
     * Sets the offset from the right edge of the window where notifications are displayed.
     *
     * @param borderOffsetRight The new offset from the right edge of the window.
     */
    public synchronized static void setBorderOffsetRight(int borderOffsetRight) {
        Notify.borderOffsetRight = borderOffsetRight;
    }

    /**
     * Sets the offset from the bottom edge of the window where notifications are displayed.
     *
     * @param borderOffsetBottom The new offset from the bottom edge of the window.
     */
    public synchronized static void setBorderOffsetBottom(int borderOffsetBottom) {
        Notify.borderOffsetBottom = borderOffsetBottom;
    }

    /**
     * Sets the maximum number of notifications that can be visible at once.
     *
     * @param maxVisibleNotification The new maximum number of visible notifications.
     */
    public synchronized static void setMaxVisibleNotification(int maxVisibleNotification) {
        Notify.maxVisibleNotification = maxVisibleNotification;
    }

    /**
     * Sets the margin between notifications.
     *
     * @param notificationMargin The new margin between notifications.
     */
    public synchronized static void setNotificationMargin(int notificationMargin) {
        Notify.notificationMargin = notificationMargin;
    }

    /**
     * Renders the notifications on the screen.
     * Only the maximum number of visible notifications are shown.
     * Notifications are rendered from the bottom of the screen upwards.
     */
    public static void render() {
        synchronized (notifications) {
            int posY = WindowUtils.getWindowHeight() - borderOffsetBottom;
            int displayedCount = 0;

            Iterator<Notification> iterator = notifications.iterator();

            while (iterator.hasNext() && displayedCount < maxVisibleNotification) {
                Notification notification = iterator.next();

                if (displayedCount == 0) posY += notificationMargin;

                if (notification.isExpired()) {
                    iterator.remove();
                } else {
                    notification.show();
                    posY -= notification.getHeight() + notificationMargin;
                    notification.render(WindowUtils.getWindowWidth() - notification.getWidth() - borderOffsetRight, posY);
                    displayedCount++;
                }
            }
        }
    }
}