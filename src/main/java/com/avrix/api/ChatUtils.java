package com.avrix.api;

import zombie.core.Color;
import zombie.core.raknet.UdpConnection;
import zombie.network.chat.ChatServer;

/**
 * A set of tools for chat management
 */
public class ChatUtils {
    /**
     * White space symbol, used to separate words after applying color, etc. to it.
     */
    public static final String SPACE_SYMBOL = "\u200B";

    /**
     * Getting the color code for coloring a chat message
     *
     * @param r Red color component in the range 0.0 to 1.0.
     * @param g The green color component in the range 0.0 to 1.0.
     * @param b The blue component in the range 0.0 to 1.0.
     * @return Color code in RGB format.
     */
    public static String getColorCode(float r, float g, float b) {
        return getColorCode(new Color(r, g, b, 1f));
    }

    /**
     * Getting the color code for coloring a chat message
     *
     * @param r Red color component in the range 0 to 255.
     * @param g The green color component in the range 0 to 255.
     * @param b The blue component in the range 0 to 255.
     * @return Color code in RGB format.
     */
    public static String getColorCode(int r, int g, int b) {
        return getColorCode(new Color(r, g, b, 255));
    }

    /**
     * Getting the color code for coloring a chat message
     *
     * @param color Color object.
     * @return Color code in RGB format.
     */
    public static String getColorCode(Color color) {
        return String.format("<RGB:%.3f,%.3f,%.3f>",
                color.getR(),
                color.getG(),
                color.getB());
    }

    /**
     * Sending a message to a general chat for all users
     *
     * @param text Message text
     */
    public static void sendMessageToAll(String text) {
        ChatServer.getInstance().sendMessageToServerChat(text);
    }

    /**
     * Sending a chat message to a specific user
     *
     * @param playerConnection player connection
     * @param text             Message text
     */
    public static void sendMessageToPlayer(UdpConnection playerConnection, String text) {
        ChatServer.getInstance().sendMessageToServerChat(playerConnection, text);
    }
}