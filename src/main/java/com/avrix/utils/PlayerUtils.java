package com.avrix.utils;

import com.avrix.enums.AccessLevel;
import com.avrix.events.EventManager;
import zombie.characters.IsoPlayer;
import zombie.commands.PlayerType;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.SteamUtils;
import zombie.inventory.InventoryItem;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerWorldDatabase;
import zombie.network.Userlog;
import zombie.network.chat.ChatServer;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;

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
     * @return {@link IsoPlayer} instance or null if player not found
     */
    public static IsoPlayer getPlayerByUdpConnection(UdpConnection udpConnection) {
        return getPlayerByUsername(udpConnection.username);
    }

    /**
     * Getting a player's connection based on his character
     *
     * @param player player instance
     * @return the player's {@link UdpConnection}, or null if there is none
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
     * @return {@link IsoPlayer}  instance, or null if not found
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
     * @return The first {@link IsoPlayer} object that matches the given username, or null if no match is found.
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
     * Adds a specified amount of an item to a player's inventory by its {@link InventoryItem}.
     *
     * @param player the {@link IsoPlayer} to add the item to
     * @param item   the {@link InventoryItem} to add
     * @param amount the amount of the item to add
     */
    public static void addItem(IsoPlayer player, InventoryItem item, int amount) {
        if (player == null) return;

        addItem(getUdpConnectionByPlayer(player), item, amount);
    }

    /**
     * Adds a specified amount of an item to a player's inventory by its type.
     *
     * @param player   the {@link IsoPlayer} to add the item to
     * @param itemType the type of the item to add
     * @param amount   the amount of the item to add
     */
    public static void addItem(IsoPlayer player, String itemType, int amount) {
        if (player == null) return;

        addItem(getUdpConnectionByPlayer(player), itemType, amount);
    }

    /**
     * Adds a specified amount of an item to a player's inventory by its {@link InventoryItem}.
     *
     * @param connection the player's connection
     * @param item       the {@link InventoryItem} to add
     * @param amount     the amount of the item to add
     */
    public static void addItem(UdpConnection connection, InventoryItem item, int amount) {
        addItem(connection, item.getFullType(), amount);
    }

    /**
     * Adds a specified amount of an item to a player's inventory by its type.
     *
     * @param connection the player's {@link UdpConnection}
     * @param itemType   the type of the item to add
     * @param amount     the amount of the item to add
     */
    public static void addItem(UdpConnection connection, String itemType, int amount) {
        if (connection == null) return;

        IsoPlayer player = getPlayerByUdpConnection(connection);

        if (player == null) return;

        Item item = ScriptManager.instance.FindItem(itemType);

        if (item == null) {
            System.out.printf("[!] Cannot add item for player '%s' because ID '%s' does not exist!%n", connection.username, itemType);
            return;
        }

        ByteBufferWriter byteBufferWriter = connection.startPacket();
        PacketTypes.PacketType.AddItemInInventory.doPacket(byteBufferWriter);
        byteBufferWriter.putShort(player.OnlineID);
        byteBufferWriter.putUTF(itemType);
        byteBufferWriter.putInt(amount);
        PacketTypes.PacketType.AddItemInInventory.send(connection);

        System.out.printf("[#] The item '%s' has been added to '%s' inventory in the amount of '%s'%n", player.getUsername(), itemType, amount);
        LoggerManager.getLogger("admin").write("Console added item " + itemType + " in " + player.getUsername() + "'s inventory in the amount of " + amount);
    }

    /**
     * Removes an item from a player's inventory by its type.
     *
     * @param player   the {@link IsoPlayer} instance
     * @param itemType the type of item to remove
     */
    public static void removeItem(IsoPlayer player, String itemType) {
        if (player == null) return;

        removeItem(getUdpConnectionByPlayer(player), itemType);
    }

    /**
     * Removes an item from a player's inventory by its ID.
     *
     * @param player the {@link IsoPlayer} instance
     * @param itemId the ID of the item to remove
     */
    public static void removeItem(IsoPlayer player, int itemId) {
        if (player == null) return;

        removeItem(getUdpConnectionByPlayer(player), itemId);
    }

    /**
     * Removes the specified item from a player's inventory.
     *
     * @param player the {@link IsoPlayer} from whom the item is to be removed
     * @param item   the {@link InventoryItem} to be removed
     */
    public static void removeItem(IsoPlayer player, InventoryItem item) {
        if (player == null) return;
        removeItem(player, item.getFullType());
    }

    /**
     * Removes the specified item from a player's inventory.
     *
     * @param connection the {@link UdpConnection} of the player from whom the item is to be removed
     * @param item       the {@link InventoryItem} to be removed
     */
    public static void removeItem(UdpConnection connection, InventoryItem item) {
        if (connection == null) return;
        removeItem(connection, item.getFullType());
    }

    /**
     * Removes an item from a player's inventory by its type.
     *
     * @param connection the player's {@link UdpConnection}
     * @param itemType   the type of item to remove
     */
    public static void removeItem(UdpConnection connection, String itemType) {
        if (connection == null) return;

        ByteBufferWriter byteBufferWriter = connection.startPacket();
        PacketTypes.PacketType.InvMngReqItem.doPacket(byteBufferWriter);
        byteBufferWriter.putByte((byte) 1);
        byteBufferWriter.putUTF(itemType);
        byteBufferWriter.putShort((byte) -1);
        PacketTypes.PacketType.InvMngReqItem.send(connection);
    }

    /**
     * Removes an item from a player's inventory by its ID.
     *
     * @param connection the player's {@link UdpConnection}
     * @param itemId     the ID of the item to remove
     */
    public static void removeItem(UdpConnection connection, int itemId) {
        if (connection == null) return;

        ByteBufferWriter byteBufferWriter = connection.startPacket();
        PacketTypes.PacketType.InvMngReqItem.doPacket(byteBufferWriter);
        byteBufferWriter.putByte((byte) 0);
        byteBufferWriter.putInt(itemId);
        byteBufferWriter.putShort((byte) -1);
        PacketTypes.PacketType.InvMngReqItem.send(connection);
    }

    /**
     * Kicks a player from the server.
     *
     * @param player the {@link IsoPlayer} instance
     * @param reason the reason for kicking the player
     */
    public static void kickPlayer(IsoPlayer player, String reason) {
        if (player == null) return;

        kickPlayer(getUdpConnectionByPlayer(player), reason);
    }

    /**
     * Kicks a player from the server.
     *
     * @param connection the player's {@link UdpConnection}
     * @param reason     the reason for kicking the player
     */
    public static void kickPlayer(UdpConnection connection, String reason) {
        if (connection == null) return;

        EventManager.invokeEvent("onPlayerKick", connection, "Console", reason);

        String kickMessage = String.format("[!] You have been kicked from this server by `%s`", reason);
        GameServer.kick(connection, kickMessage, null);
        connection.forceDisconnect("command-kick");

        System.out.printf("[!] Player `%s` (IP: %s, SteamID: %s) was kicked from this server for the following reason: `%s`%n",
                connection.steamID, connection.ip, connection.steamID, reason);
    }

    /**
     * Bans a player from the server.
     *
     * @param player     the {@link IsoPlayer} instance
     * @param reason     the reason for banning the player
     * @param banIP      whether to ban the player's IP address
     * @param banSteamID whether to ban the player's SteamID
     */
    public static void banPlayer(IsoPlayer player, String reason, boolean banIP, boolean banSteamID) {
        if (player == null) return;

        banPlayer(getUdpConnectionByPlayer(player), reason, banIP, banSteamID);
    }

    /**
     * Bans a player from the server.
     *
     * @param connection the player's {@link UdpConnection}
     * @param reason     the reason for banning the player
     * @param banIP      whether to ban the player's IP address
     * @param banSteamID whether to ban the player's SteamID
     */
    public static void banPlayer(UdpConnection connection, String reason, boolean banIP, boolean banSteamID) {
        if (connection == null) return;

        EventManager.invokeEvent("onPlayerBan", connection, "Console", reason);

        ServerWorldDatabase.instance.addUserlog(connection.username, Userlog.UserlogType.Banned, reason, "Server", 1);

        banByName(connection);

        if (SteamUtils.isSteamModeEnabled() && banSteamID) banBySteamID(connection, reason);

        if (banIP) banByIP(connection, reason);

        String kickMessage = String.format("[!] You have been banned from this server for the following reason: `%s`", reason);
        GameServer.kick(connection, kickMessage, null);
        connection.forceDisconnect("command-ban-ip");

        System.out.printf("[!] Player `%s` (IP: %s, SteamID: %s) was banned from this server for the following reason: `%s`%n",
                connection.username, connection.ip, connection.steamID, reason);
    }

    /**
     * Bans a player by their SteamID.
     *
     * @param connection the player's {@link UdpConnection}
     * @param reason     the reason for banning the player
     */
    private static void banBySteamID(UdpConnection connection, String reason) {
        String steamID = SteamUtils.convertSteamIDToString(connection.steamID);

        try {
            ServerWorldDatabase.instance.banSteamID(steamID, reason, true);
        } catch (SQLException e) {
            System.out.printf("[!] Error while ban SteamID: '%s', error: %s%n", steamID, e);
        }
    }

    /**
     * Blocks a player by IP address.
     *
     * @param playerConnection the player's {@link UdpConnection} to the server
     * @param reason           the reason for blocking the player
     */
    private static void banByIP(UdpConnection playerConnection, String reason) {
        try {
            ServerWorldDatabase.instance.banIp(playerConnection.ip, playerConnection.username, reason, true);
        } catch (SQLException e) {
            System.out.printf("[!] Error while ban IP: '%s', error: %s%n", playerConnection.ip, e);
        }
    }

    /**
     * Bans a player by their username.
     *
     * @param connection the player's {@link UdpConnection}
     */
    private static void banByName(UdpConnection connection) {
        try {
            ServerWorldDatabase.instance.banUser(connection.username, true);
        } catch (SQLException e) {
            System.out.printf("[!] Error while ban user: '%s', error: %s%n", connection.username, e);
        }
    }
}