package com.avrix.api;

import com.avrix.plugin.EventManager;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.SteamUtils;
import zombie.network.GameServer;
import zombie.network.ServerWorldDatabase;
import zombie.network.Userlog;

import java.sql.SQLException;

/**
 * A set of tools for player management, monitoring and analysis
 */
public class PlayerUtils {
    /**
     * Getting a player's instance on his connection
     *
     * @param udpConnection player connection
     * @return player instance or null if player not found
     */
    public static IsoPlayer getPlayerByUdpConnection(UdpConnection udpConnection) {
        return getPlayerByUsername(udpConnection.username);
    }

    /**
     * Getting a player's connection based on his character
     *
     * @param player player instance
     * @return the player's connection, or null if there is none
     */
    public static UdpConnection getUdpConnectionByPlayer(IsoPlayer player) {
        return GameServer.getConnectionFromPlayer(player);
    }

    /**
     * Returns the player's IP address.
     *
     * @param player The player instance for which you want to obtain the IP address.
     * @return The player's IP address or null if the address is not found.
     */
    public static String getPlayerIP(IsoPlayer player) {
        UdpConnection playerConnection = getUdpConnectionByPlayer(player);
        return playerConnection != null ? playerConnection.ip : null;
    }

    /**
     * Returns the player's SteamID.
     *
     * @param player The player instance for which you want to obtain a SteamID.
     * @return Player's SteamID or null if Steam mode is disabled or SteamID not found.
     */
    public static String getPlayerSteamID(IsoPlayer player) {
        if (SteamUtils.isSteamModeEnabled()) {
            return String.valueOf(player.getSteamID());
        }
        return null;
    }

    /**
     * Getting a player instance by nickname
     *
     * @param username player nickname
     * @return IsoPlayer instance, or null if not found
     */
    public static IsoPlayer getPlayerByUsername(String username) {
        for (int connectionIndex = 0; connectionIndex < GameServer.udpEngine.connections.size(); ++connectionIndex) {
            UdpConnection connection = GameServer.udpEngine.connections.get(connectionIndex);

            for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
                IsoPlayer player = connection.players[playerIndex];
                if (player != null && (player.getDisplayName().equals(username) || player.getUsername().equals(username))) {
                    return player;
                }
            }
        }
        return null;
    }

    /**
     * Searches for a player by a full or partial username.
     *
     * @param userName The full or partial name of the player to search for.
     * @return The first IsoPlayer object that matches the given username, or null if no match is found.
     */
    public static IsoPlayer getPlayerByPartialUsername(String userName) {
        for (int connectionIndex = 0; connectionIndex < GameServer.udpEngine.connections.size(); ++connectionIndex) {
            UdpConnection connection = GameServer.udpEngine.connections.get(connectionIndex);

            for (int playerIndex = 0; playerIndex < 4; ++playerIndex) {
                IsoPlayer player = connection.players[playerIndex];
                if (player != null) {
                    String displayNameLower = player.getDisplayName().toLowerCase();
                    String userNameLower = userName.toLowerCase();
                    if (displayNameLower.equals(userNameLower) || displayNameLower.startsWith(userNameLower)) {
                        return player;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Kick a player from the server
     *
     * @param player Player instance
     * @param reason kick reason
     */
    public static void kickPlayer(IsoPlayer player, String reason) {
        if (player == null) return;

        EventManager.invokeEvent("onPlayerKick", player, "Console", reason);

        UdpConnection playerConnection = getUdpConnectionByPlayer(player);

        if (playerConnection == null) return;

        String kickMessage = String.format("You have been kicked from this server by `%s`", reason);
        GameServer.kick(playerConnection, kickMessage, null);
        playerConnection.forceDisconnect("command-kick");

        String logMessage = String.format("[!] Player `%s` (IP: %s | SteamID: %s) was kicked from this server for the following reason: `%s`",
                player.getDisplayName(), playerConnection.ip, player.getSteamID(), reason);
        System.out.println(logMessage);
    }

    /**
     * Blocking a user by nickname, IP and/or SteamID
     *
     * @param player     Player instance
     * @param reason     Reason for blocking
     * @param banIP      flag whether to block by IP
     * @param banSteamID flag whether to block by SteamID
     */
    public static void banPlayer(IsoPlayer player, String reason, boolean banIP, boolean banSteamID) {
        if (player == null) return;

        EventManager.invokeEvent("onPlayerBan", player, "Console", reason);

        UdpConnection playerConnection = getUdpConnectionByPlayer(player);

        if (playerConnection == null) return;

        ServerWorldDatabase.instance.addUserlog(player.getUsername(), Userlog.UserlogType.Banned, reason, "Server", 1);

        banByName(player);

        if (SteamUtils.isSteamModeEnabled() && banSteamID) banBySteamID(player, reason);

        if (banIP) banByIP(playerConnection, player, reason);

        String kickMessage = String.format("You have been banned from this server for the following reason: `%s`", reason);
        GameServer.kick(playerConnection, kickMessage, null);
        playerConnection.forceDisconnect("command-ban-ip");

        String logMessage = String.format("[!] Player `%s` (IP: %s | SteamID: %s) was banned from this server for the following reason: `%s`",
                player.getDisplayName(), playerConnection.ip, player.getSteamID(), reason);
        System.out.println(logMessage);
    }

    /**
     * Blocks a player by SteamID
     *
     * @param player Player to block
     * @param reason Reason for blocking
     */
    private static void banBySteamID(IsoPlayer player, String reason) {
        String steamID = SteamUtils.convertSteamIDToString(player.getSteamID());
        try {
            ServerWorldDatabase.instance.banSteamID(steamID, reason, true);
        } catch (SQLException e) {
            String errorMessage = String.format("[!] Error while ban SteamID: '%s', error: %s", steamID, e);
            System.out.println(errorMessage);
        }
    }

    /**
     * Blocks a player by IP address.
     *
     * @param playerConnection Connecting the player to the server.
     * @param player           The player to block.
     * @param reason           Reason for blocking.
     */
    private static void banByIP(UdpConnection playerConnection, IsoPlayer player, String reason) {
        try {
            ServerWorldDatabase.instance.banIp(playerConnection.ip, player.getUsername(), reason, true);
        } catch (SQLException e) {
            String errorMessage = String.format("[!] Error while ban IP: '%s', error: %s", playerConnection.ip, e);
            System.out.println(errorMessage);
        }
    }

    /**
     * Blocks a player by username.
     *
     * @param player The player to block.
     */
    private static void banByName(IsoPlayer player) {
        try {
            ServerWorldDatabase.instance.banUser(player.getUsername(), true);
        } catch (SQLException e) {
            String errorMessage = String.format("[!] Error while ban user: '%s', error: %s", player.getUsername(), e);
            System.out.println(errorMessage);
        }
    }
}