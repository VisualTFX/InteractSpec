package com.visualtfx.interactspec;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class InteractSpec extends JavaPlugin implements Listener {

    // A set to keep track of players who are in spec mode
    private final HashSet<UUID> spectatingPlayers = new HashSet<>();
    // A map to store the original gamemode of players
    private final HashMap<UUID, GameMode> originalGameModes = new HashMap<>();
    // ProtocolManager instance for sending packets
    private ProtocolManager protocolManager;

    @Override
    public void onEnable() {
        // This runs when the plugin is enabled
        protocolManager = ProtocolLibrary.getProtocolManager();
        this.getCommand("interactspec").setExecutor(new InteractSpecCommand(this));
        this.getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("InteractSpec has been enabled!");
    }

    @Override
    public void onDisable() {
        // This runs when the plugin is disabled
        // Restore all spectating players to their original state to prevent issues
        for (UUID playerId : new HashSet<>(spectatingPlayers)) {
            Player player = getServer().getPlayer(playerId);
            if (player != null) {
                disableSpecMode(player);
            }
        }
        getLogger().info("InteractSpec has been disabled!");
    }

    // Method to check if a player is in spec mode
    public boolean isSpectating(Player player) {
        return spectatingPlayers.contains(player.getUniqueId());
    }

    // Method to enable spec mode for a player
    public void enableSpecMode(Player player) {
        spectatingPlayers.add(player.getUniqueId());
        originalGameModes.put(player.getUniqueId(), player.getGameMode());

        // Set server-side gamemode to Creative
        player.setGameMode(GameMode.CREATIVE);
        // Make player invisible with no particles
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false));
        // Disable collision with other entities
        player.setCollidable(false);

        // Send a packet to the client to put them in spectator mode (for noclip)
        sendGameStatePacket(player, 3, 3); // 3 = change gamemode, 3 = spectator

        player.sendMessage("§aInteractSpec mode enabled.");
    }

    // Method to disable spec mode for a player
    public void disableSpecMode(Player player) {
        // Send a packet to change client back to creative mode before server-side change
        sendGameStatePacket(player, 3, 1); // 3 = change gamemode, 1 = creative

        // Restore original gamemode
        GameMode originalMode = originalGameModes.getOrDefault(player.getUniqueId(), GameMode.SURVIVAL);
        player.setGameMode(originalMode);

        // Clear effects
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.setCollidable(true);

        // Remove from tracking lists
        spectatingPlayers.remove(player.getUniqueId());
        originalGameModes.remove(player.getUniqueId());

        player.sendMessage("§cInteractSpec mode disabled.");
    }

    // Utility method to send the gamemode change packet
    private void sendGameStatePacket(Player player, int reason, float value) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.GAME_STATE_CHANGE);
        packet.getIntegers().write(0, reason);
        packet.getFloat().write(0, value);
        try {
            protocolManager.sendServerPacket(player, packet);
        } catch (InvocationTargetException e) {
            getLogger().severe("Failed to send gamestate packet to " + player.getName());
            e.printStackTrace();
        }
    }

    // Event handler to clean up when a player quits
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (isSpectating(event.getPlayer())) {
            disableSpecMode(event.getPlayer());
        }
    }
}
