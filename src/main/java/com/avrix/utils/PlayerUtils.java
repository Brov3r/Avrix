package com.avrix.utils;

import com.avrix.enums.AccessLevel;
import com.avrix.events.EventManager;
import zombie.characters.IsoPlayer;
import zombie.commands.PlayerType;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.SteamUtils;
import zombie.network.GameServer;
import zombie.network.ServerWorldDatabase;
import zombie.network.Userlog;
import zombie.network.chat.ChatServer;

import java.sql.SQLException;

/**
 * A set of tools for player management, monitoring and analysis
 */
public class PlayerUtils {
    /**
     * Sets the {@link AccessLevel} of a player by their username.
     *
     * @param playerName  the username of the player
     * @param accessLevel the new {@link AccessLevel} to set
     */
    public static void setAccessLevel(String playerName, AccessLevel accessLevel) {
        setAccessLevel(getPlayerByPartialUsername(playerName), accessLevel);
    }

    /**
     * Sets the {@link AccessLevel} of a player.
     *
     * @param player      the {@link IsoPlayer} object representing the player
     * @param accessLevel the new {@link AccessLevel} to set
     */
    public static void setAccessLevel(IsoPlayer player, AccessLevel accessLevel) {
        setAccessLevel(getUdpConnectionByPlayer(player), accessLevel);
    }

    /**
     * Sets the {@link AccessLevel} of a player.
     *
     * @param connection  the {@link UdpConnection} object representing the player's connection
     * @param accessLevel the new {@link AccessLevel} to set
     */
    public static void setAccessLevel(UdpConnection connection, AccessLevel accessLevel) {
        if (connection == null) {
            System.out.println("[!] UdpConnection is null. Unable to set access level.");
            return;
        }

        IsoPlayer player = getPlayerByUdpConnection(connection);
        if (player == null) {
            System.out.printf("[!] IsoPlayer not found for UdpConnection: %s%n", connection.username);
            return;
        }

        if (!ServerWorldDatabase.instance.containsUser(connection.username)) {
            System.out.printf("[!] Player '%s' is not in the whitelist!%n", connection.username);
            return;
        }

        if (player.networkAI != null) {
            player.networkAI.setCheckAccessLevelDelay(5000L);
        }

        boolean isAdmin = accessLevel.equals(AccessLevel.ADMIN);

        if (isAdmin) {
            ChatServer.getInstance().joinAdminChat(player.OnlineID);
        } else {
            ChatServer.getInstance().leaveAdminChat(player.OnlineID);
        }

        player.setGodMod(isAdmin);
        player.setGhostMode(isAdmin);
        player.setInvisible(isAdmin);
        player.setNoClip(isAdmin);
        player.setShowAdminTag(isAdmin);

        player.accessLevel = accessLevel.getRoleName();
        connection.accessLevel = PlayerType.fromString(accessLevel.getRoleName());

        GameServer.sendPlayerExtraInfo(player, null);

        try {
            ServerWorldDatabase.instance.setAccessLevel(connection.username, accessLevel.getRoleName());
        } catch (Exception e) {
            System.out.printf("[!] Failed to update access level for player '%s' to '%s' in database: %s%n", connection.username, accessLevel.getRoleName(), e.getMessage());
            LoggerManager.getLogger("admin").write(String.format("[!] Failed to update access level for player '%s' to '%s' in database: %s", connection.username, accessLevel.getRoleName(), e.getMessage()));
            return;
        }

        System.out.printf("[#] Console granted '%s' access level on '%s'%n", accessLevel.getRoleName(), connection.username);
        LoggerManager.getLogger("admin").write(String.format("Console granted '%s' access level on '%s'", accessLevel.getRoleName(), connection.username));
        ChatUtils.sendMessageToPlayer(connection, String.format("[#] Your access level is set to '%s'", accessLevel.getRoleName()));
    }

    /**
     * Retrieves the access level of a player based on their {@link UdpConnection}.
     *
     * @param connection the {@link UdpConnection} of the player
     * @return the access level of the player as an {@link AccessLevel} enumeration
     */
    public static AccessLevel getAccessLevel(UdpConnection connection) {
        return AccessLevel.fromString(getPlayerByUdpConnection(connection).accessLevel);
    }

    /**
     * Retrieves the access level of a player.
     *
     * @param player the {@link IsoPlayer} whose access level is to be retrieved
     * @return the access level of the player as an {@link AccessLevel} enumeration
     */
    public static AccessLevel getAccessLevel(IsoPlayer player) {
        return AccessLevel.fromString(player.accessLevel);
    }

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
        if (!SteamUtils.isSteamModeEnabled()) return null;

        return String.valueOf(player.getSteamID());
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

                if (player == null) continue;

                if (player.getDisplayName().equals(username) || player.getUsername().equals(username)) {
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

                if (player == null) continue;

                String displayNameLower = player.getDisplayName().toLowerCase();
                String userNameLower = userName.toLowerCase();
                if (displayNameLower.equals(userNameLower) || displayNameLower.startsWith(userNameLower)) {
                    return player;
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

        UdpConnection playerConnection = getUdpConnectionByPlayer(player);

        if (playerConnection == null) return;

        EventManager.invokeEvent("onPlayerKick", player, "Console", reason);

        String kickMessage = String.format("[!] You have been kicked from this server by `%s`", reason);
        GameServer.kick(playerConnection, kickMessage, null);
        playerConnection.forceDisconnect("command-kick");

        System.out.printf("[!] Player `%s` (IP: %s, SteamID: %s) was kicked from this server for the following reason: `%s`%n",
                player.getDisplayName(), playerConnection.ip, player.getSteamID(), reason);
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

        UdpConnection playerConnection = getUdpConnectionByPlayer(player);

        if (playerConnection == null) return;

        EventManager.invokeEvent("onPlayerBan", player, "Console", reason);

        ServerWorldDatabase.instance.addUserlog(player.getUsername(), Userlog.UserlogType.Banned, reason, "Server", 1);

        banByName(player);

        if (SteamUtils.isSteamModeEnabled() && banSteamID) banBySteamID(player, reason);

        if (banIP) banByIP(playerConnection, player, reason);

        String kickMessage = String.format("[!] You have been banned from this server for the following reason: `%s`", reason);
        GameServer.kick(playerConnection, kickMessage, null);
        playerConnection.forceDisconnect("command-ban-ip");

        System.out.printf("[!] Player `%s` (IP: %s, SteamID: %s) was banned from this server for the following reason: `%s`%n",
                player.getDisplayName(), playerConnection.ip, player.getSteamID(), reason);
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
            System.out.printf("[!] Error while ban SteamID: '%s', error: %s%n", steamID, e);
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
            System.out.printf("[!] Error while ban IP: '%s', error: %s%n", playerConnection.ip, e);
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
            System.out.printf("[!] Error while ban user: '%s', error: %s%n", player.getUsername(), e);
        }
    }
}